/*
 * Copyright 2014-2016 ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.pie;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.RemoteException;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;
import com.android.systemui.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Pie menu
 * Handles creating, drawing, animations and touch eventing for pie.
 */
public class PieMenu extends RelativeLayout {
    private static final String TAG = "PIEMENU";

    private static final String FONT_FAMILY_MEDIUM = "sans-serif-medium";
    private static final String ROBOTO_REGULAR = "roboto:regular";

    private final Point mCenter = new Point(0, 0);

    // paints
    private final Paint mToggleBackground;
    private final Paint mToggleExtraBackground;
    private final Paint mClockPaint;
    private final Paint mStatusPaint;
    private final Paint mLinePaint;
    private final Paint mCirclePaint;
    private final Paint mBackgroundPaint;
    private final Context mContext;
    private final Resources mResources;
    private final PieHelper mPieHelper;
    private final Vibrator mVibrator;
    private final List<PieItem> mItems;
    private final PieControlPanel mPanel;
    private final ImageView mNOTLogo;
    private final TogglePoint[] mTogglePoint = new TogglePoint[4];

    private int mForegroundColor;
    private int mBackgroundColor;
    private int mIconColor;
    private int mLineColor;
    private int mSnapAnimColor;

    private int mPanelOrientation;
    private int mPieGap;
    private int mOuterCircleRadius;
    private int mOuterCircleThickness;
    private int mInnerCircleRadius;
    private float mCenterDistance = 0;

    private String mClockText;
    private String mBatteryText;
    private String mDateText;

    // Animators
    private ValueAnimator mBackgroundAnimator;
    private ValueAnimator mToggleOutsideAnimator;
    private ValueAnimator mToggleGrowAnimator;
    private ValueAnimator mPieMoveAnimator;
    private ValueAnimator mPieGrowAnimator;

    private int mOverallSpeed;

    private float mClockOffsetX;
    private float mClockOffsetY;
    private float mDateOffsetX;
    private float mDateOffsetY;
    private float mBatteryOffsetX;
    private float mBatteryOffsetY;
    private float mBatteryOffsetYSide;

    private int mLineLength;
    private int mLineOffset;
    private int mLineOffsetSide;
    private int mWidth;
    private int mHeight;

    private PieItem mCurrentItem;
    private List<ImageView> mIconViews;
    private boolean mHasShown;

    private int mNumberOfTogglePoints;
    private int mNumberOfSnapPoints;

    private int mSnapRadius;
    private int mSnapOffset;

    private boolean mOpen;
    private boolean mHapticFeedback;

    /**
     * Creates a new pie outline view
     *
     * @param context the current context
     * @param panel the current PieControlPanel
     */
    public PieMenu(Context context, PieControlPanel panel) {
        super(context);

        mContext = context;
        mResources = mContext.getResources();
        mPanel = panel;

        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        setElevation(mResources.getDimensionPixelSize(R.dimen.pie_elevation));

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mPieHelper = PieHelper.getInstance();

        // initialize classes
        mItems = new ArrayList<>();

        mNOTLogo = new ImageView(mContext);
        mNOTLogo.setImageResource(R.drawable.ic_google_logo);
        addView(mNOTLogo);

        // initialize main paints
        mToggleBackground = new Paint();
        mToggleBackground.setAntiAlias(true);

        // snap point animation paint
        mToggleExtraBackground = new Paint();
        mToggleExtraBackground.setAntiAlias(true);
        mToggleExtraBackground.setAlpha(20);
        mToggleExtraBackground.setStrokeWidth(
                mResources.getDimensionPixelSize(R.dimen.pie_snap_anim_stroke_width));
        mToggleExtraBackground.setStyle(Paint.Style.STROKE);

        // clock
        mClockPaint = new Paint();
        mClockPaint.setAntiAlias(true);
        mClockPaint.setTypeface(Typeface.create(ROBOTO_REGULAR, Typeface.NORMAL));

        // status (date and battery)
        mStatusPaint = new Paint();
        mStatusPaint.setAntiAlias(true);
        mStatusPaint.setTypeface(Typeface.create(FONT_FAMILY_MEDIUM, Typeface.NORMAL));

        // line
        mLinePaint = new Paint();
        mLinePaint.setAntiAlias(true);

        // pie circles
        mCirclePaint = new Paint();
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.FILL_AND_STROKE);
        // drop shadow doesn't support hardware acceleration so we can't use it

        // background circle
        mBackgroundPaint = new Paint();

        // Clock observer
        mPieHelper.setOnClockChangedListener(new PieHelper.OnClockChangedListener() {
            public void onChange(String s) {
                measureClock(s);
            }
        });

        // Get all dimensions
        getDimensions();
    }

    private void getDimensions() {
        // fetch colors
        mForegroundColor = mResources.getColor(R.color.pie_foreground);
        mBackgroundColor = mResources.getColor(R.color.pie_background);
        mIconColor = mResources.getColor(R.color.pie_icon);
        mLineColor = mResources.getColor(R.color.pie_line);
        mSnapAnimColor = mResources.getColor(R.color.pie_snap_anim_color);

        // fetch orientation
        mPanelOrientation = mPanel.getOrientation();

        // fetch modes
        mHapticFeedback = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;

        // Gap size
        mPieGap = mResources.getDimensionPixelSize(R.dimen.pie_gap);

        // snap
        mSnapRadius = mResources.getDimensionPixelSize(R.dimen.pie_snap_radius);
        mSnapOffset = mResources.getDimensionPixelSize(R.dimen.pie_snap_offset);

        // Pie animation speed
        mOverallSpeed = mResources.getInteger(R.integer.pie_animation_speed);

        Point outSize = new Point(0, 0);
        WindowManager windowManager = (WindowManager)
                mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(outSize);
        mWidth = outSize.x;
        mHeight = outSize.y;

        int snapIndex = 0;
        if (mPanelOrientation != Gravity.LEFT && mPanel.isGravityPossible(Gravity.LEFT)) {
            mTogglePoint[snapIndex++] = new SnapPoint(
                    0 - mSnapOffset, mHeight / 2, mSnapRadius, Gravity.LEFT);
        }

        if (mPanelOrientation != Gravity.RIGHT && mPanel.isGravityPossible(Gravity.RIGHT)) {
            mTogglePoint[snapIndex++] = new SnapPoint(
                    mWidth + mSnapOffset, mHeight / 2, mSnapRadius, Gravity.RIGHT);
        }

        if ((!mPanel.isLandScape() || isTablet()) && mPanelOrientation != Gravity.BOTTOM &&
                mPanel.isGravityPossible(Gravity.BOTTOM)) {
            mTogglePoint[snapIndex++] = new SnapPoint(
                    mWidth / 2, mHeight + mSnapOffset, mSnapRadius, Gravity.BOTTOM);
        }
        mNumberOfSnapPoints = snapIndex;

        // now on tap
        int mNOTSize = (mResources.getDimensionPixelSize(R.dimen.pie_not_size));
        int mNOTRadius = (mResources.getDimensionPixelSize(R.dimen.pie_not_radius));
        int mNOTOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_not_offset);
        int mNOTOffsetYside = mResources.getDimensionPixelSize(R.dimen.pie_not_offsetSide);
        int mNOTOffsetX = mResources.getDimensionPixelSize(R.dimen.pie_not_offsetx);
        int mNOTOffsetXright = mResources.getDimensionPixelSize(R.dimen.pie_not_offsetxright);
        setColor(mNOTLogo, mBackgroundColor);
        final boolean mPieRight = mPanelOrientation == Gravity.RIGHT;
        final boolean mPieBottom = mPanelOrientation == Gravity.BOTTOM;
        mTogglePoint[mNumberOfSnapPoints] = new NowOnTapPoint(mWidth / 2 +
                (mPieBottom ? 0 :
                (mPieRight ? mNOTOffsetXright : mNOTOffsetX)),
                mHeight / 2 + (mPieBottom ? mNOTOffsetY : mNOTOffsetYside),
                mNOTRadius, mNOTLogo, mNOTSize);
        mNumberOfTogglePoints = mNumberOfSnapPoints + 1;

        // create pie
        mOuterCircleRadius = mResources
                .getDimensionPixelSize(R.dimen.pie_outer_circle_radius);
        mOuterCircleThickness = mResources
                .getDimensionPixelSize(R.dimen.pie_outer_circle_thickness);
        mInnerCircleRadius = mResources
                .getDimensionPixelSize(R.dimen.pie_inner_circle_radius);

        // clock
        mClockPaint.setTextSize(mResources
                .getDimensionPixelSize(R.dimen.pie_clock_size));
        measureClock(mPieHelper.getSimpleTime());
        mClockOffsetY = mResources
                .getDimensionPixelSize(R.dimen.pie_clock_offset);

        // status (date and battery)
        mStatusPaint.setTextSize(mResources
                .getDimensionPixelSize(R.dimen.pie_status_size));

        // date
        mDateText = mPieHelper.getSimpleDate();
        mDateOffsetX = mStatusPaint.measureText(mDateText) / 2;
        mDateOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_date_offset);

        // battery
        mBatteryText = mResources.getString(R.string.pie_battery_level)
                + mPieHelper.getBatteryLevel() + "%";
        mBatteryOffsetX = mStatusPaint.measureText(mBatteryText) / 2;
        mBatteryOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_battery_offset);
        mBatteryOffsetYSide = mResources.getDimensionPixelSize(R.dimen.pie_battery_offset_side);

        // line
        mLinePaint.setStrokeWidth(mResources.getDimensionPixelSize(R.dimen.pie_line_width));
        mLineLength = mResources.getDimensionPixelSize(R.dimen.pie_line_length);
        mLineOffset = mResources.getDimensionPixelSize(R.dimen.pie_line_offset);
        mLineOffsetSide = mResources.getDimensionPixelSize(R.dimen.pie_line_offset_side);

        // notifications
        int iconSize = mResources.getDimensionPixelSize(R.dimen.pie_icon_size);
        int mIconPadding = mResources.getDimensionPixelSize(R.dimen.pie_icon_padding);
        List<Pair<String, Icon>> mIcons = mPieHelper.getNotificationIcons();
        float iconOffsetX = ((mIcons.size() * iconSize) + ((mIcons.size() - 1) * mIconPadding)) / 2;
        float iconOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_icon_offset);
        float iconOffsetYside = mResources.getDimensionPixelSize(R.dimen.pie_icon_offset_side);

        if (mIconViews == null) {
            mIconViews = new ArrayList<>();
        }

        if (isAllowedToDraw()) {
            if (mIconViews != null) {
                for (View view : mIconViews) {
                    removeView(view);
                }
                mIconViews.clear();
            }
            int iconPos = (int) iconOffsetX;
            for (Pair<String, Icon> icon : mIcons) {
                ImageView view = new ImageView(mContext);
                try {
                    view.setImageDrawable(icon.second.loadDrawable(
                            mContext.createPackageContext(icon.first,
                                    Context.CONTEXT_IGNORE_SECURITY)));
                } catch (PackageManager.NameNotFoundException e) {
                    Log.d(TAG, "Could not load notification drawable", e);
                }
                setColor(view, mIconColor);
                view.setMinimumWidth(iconSize);
                view.setMinimumHeight(iconSize);
                view.setScaleType(ScaleType.FIT_XY);
                RelativeLayout.LayoutParams lp = new
                        RelativeLayout.LayoutParams(iconSize, iconSize);
                boolean pieBottom = mPanelOrientation == Gravity.BOTTOM;
                if (pieBottom) lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                lp.topMargin = mHeight / (pieBottom ? 2 : 4) +
                        (int) (pieBottom ? iconOffsetY : iconOffsetYside);
                lp.leftMargin = mWidth / 2 - iconPos;
                view.setLayoutParams(lp);
                addView(view);
                mIconViews.add(view);
                iconPos -= iconSize + mIconPadding;
            }
        }

        mToggleBackground.setColor(mForegroundColor);
        mToggleExtraBackground.setColor(mSnapAnimColor);
        mStatusPaint.setColor(mForegroundColor);
        mClockPaint.setColor(mForegroundColor);
        mLinePaint.setColor(mLineColor);
        mCirclePaint.setColor(mForegroundColor);
        mBackgroundPaint.setColor(mBackgroundColor);

        // linear animators
        mBackgroundAnimator = ValueAnimator.ofInt(0, 1);
        mBackgroundAnimator.setDuration((int) (mOverallSpeed * 1.5));
        mBackgroundAnimator.setInterpolator(new DecelerateInterpolator());

        mToggleOutsideAnimator = ValueAnimator.ofInt(0, 1);
        mToggleOutsideAnimator.setDuration((int) (mOverallSpeed - 50));
        mToggleOutsideAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        mToggleOutsideAnimator.setRepeatCount(1);
        mToggleOutsideAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mToggleOutsideAnimator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) {
                switchSnapPoints();
            }
        });

        mToggleGrowAnimator = ValueAnimator.ofInt(0, 1);
        mToggleGrowAnimator.setDuration((int) (mOverallSpeed + 85));
        mToggleGrowAnimator.setInterpolator(new DecelerateInterpolator());
        mToggleGrowAnimator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationCancel(Animator animation) {
                mToggleOutsideAnimator.cancel();
            }
            @Override public void onAnimationRepeat(Animator animation) {}
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) {
                mToggleOutsideAnimator.start();
            }
        });

        // move circles in
        mPieMoveAnimator = ValueAnimator.ofInt(0, 1);
        mPieMoveAnimator.setDuration((int) (mOverallSpeed));
        mPieMoveAnimator.setInterpolator(new DecelerateInterpolator());

        // grow outer circle
        mPieGrowAnimator = ValueAnimator.ofInt(0, 1);
        mPieGrowAnimator.setDuration((int) (mOverallSpeed * 1.5));
        mPieGrowAnimator.setInterpolator(new DecelerateInterpolator());
    }

    private void switchSnapPoints() {
        // Once we are here, fraction is 1 so no need to check it
        for (int i = 0; i < mNumberOfTogglePoints; i++) {
            TogglePoint toggle = mTogglePoint[i];
            if (toggle != null && toggle.active && toggle.isCurrentlyPossible(true)) {
                if (mHapticFeedback) mVibrator.vibrate(2);
                deselect();
                animateOut();
                if (toggle instanceof NowOnTapPoint) {
                    startAssist();
                } else if (toggle instanceof SnapPoint) {
                    mPanel.reorient(((SnapPoint)toggle).gravity);
                }
            }
        }
    }

    /**
     * Checks wether assist activity is available
     */
    private void startAssist() {
        final boolean mHasAssistant = mPieHelper.isAssistantAvailable();
        if (mHasAssistant) {
            mPieHelper.startAssistActivity();
        }
    }

    private void setColor(ImageView view, int color) {
        Drawable drawable = view.getDrawable();
        drawable.setColorFilter(color, Mode.SRC_ATOP);
        view.setImageDrawable(drawable);
    }

    private void measureClock(String text) {
        mClockText = text;
        mClockOffsetX = mClockPaint.measureText(mClockText) / 2;
    }

    /**
     * Checks whether the current configuration is specified as for a tablet.
     */
    private boolean isTablet() {
        return mPanel.isTablet();
    }

    public void addItem(PieItem item) {
        mItems.add(item);
    }

    public void show(boolean show) {
        mOpen = show;
        if (mOpen) {
            getDimensions();

            // de-select all items
            mCurrentItem = null;
            for (PieItem item : mItems) {
                item.setSelected(false);
            }

            // calculate pie
            layoutPie();
        }
        invalidate();
    }

    public void setCenter(int x, int y) {
        mCenter.y = y;
        mCenter.x = x;
    }

    private boolean canItemDisplay(PieItem item) {
        return !(item.getName().equals(PieController.MENU_BUTTON) && !mPanel.currentAppUsesMenu());
    }

    private void layoutPie() {
        float emptyAngle = 0;
        int inner = mOuterCircleRadius - mOuterCircleThickness;
        int outer = mOuterCircleRadius + mOuterCircleThickness;

        int itemCount = mItems.size();
        if (!mPanel.currentAppUsesMenu()) itemCount--;

        int lesserSweepCount = 0;
        for (PieItem item : mItems) {
            boolean canDisplay = canItemDisplay(item);
            if (canDisplay) {
                if (item.isLesser()) {
                    lesserSweepCount += 1;
                }
            }
        }

        float adjustedSweep = lesserSweepCount > 0 ?
                (((1 - 0.65f) * lesserSweepCount) / (itemCount - lesserSweepCount)) : 0;
        float sweep;
        float angle;
        float total = 0;

        int count = 0;
        for (PieItem item : mItems) {
            if (!canItemDisplay(item)) continue;

            sweep = ((float) (Math.PI - 2 * emptyAngle) /
                    itemCount) * (item.isLesser() ? 0.65f : 1 + adjustedSweep);
            angle = (emptyAngle + sweep / 2 - (float) Math.PI / 2);
            item.setPath(
                    makeSlice(getDegrees(0) - mPieGap, getDegrees(sweep) + mPieGap,
                            outer, inner, mCenter, (mPieGap > 0 ? mPieGap + 0.4f : 0), count != 0));
            View view = item.getView();

            if (view != null) {
                view.measure(view.getLayoutParams().width, view.getLayoutParams().height);
                int w = view.getMeasuredWidth();
                int h = view.getMeasuredHeight();
                int r = mOuterCircleRadius;
                int x = (int) (r * Math.sin(total + angle));
                int y = (int) (r * Math.cos(total + angle));

                switch (mPanelOrientation) {
                    case Gravity.LEFT:
                        y = mCenter.y - (int) (r * Math.sin(total + angle)) - h / 2;
                        x = (int) (r * Math.cos(total + angle)) - w / 2;
                        break;
                    case Gravity.RIGHT:
                        y = mCenter.y - (int) (Math.PI / 2 - r * Math.sin(total + angle)) - h / 2;
                        x = mCenter.x - (int) (r * Math.cos(total + angle)) - w / 2;
                        break;
                    case Gravity.BOTTOM:
                        y = mCenter.y - y - h / 2;
                        x = mCenter.x - x - w / 2;
                        break;
                }
                view.layout(x, y, x + w, y + h);
            }
            float itemStart = total + angle - sweep / 2;
            item.setGeometry(itemStart, sweep, inner, outer);
            total += sweep;
            count++;
        }
    }

    // param angle from 0 - pi to android degrees (clockwise starting at 3)
    private float getDegrees(double angle) {
        return (float) (270 - 180 * angle / Math.PI);
    }

    private void cancelAnimation() {
        mBackgroundAnimator.cancel();
        mToggleOutsideAnimator.cancel();
        mToggleGrowAnimator.cancel();
        mPieMoveAnimator.cancel();
        mPieGrowAnimator.cancel();
        invalidate();
    }

    private void animateInStartup() {
        // cancel & start startup animations
        cancelAnimation();
        mPieMoveAnimator.start();
        mPieGrowAnimator.start();
    }

    private void animateInRest() {
        // start missing animations
        mHasShown = true;
        mBackgroundAnimator.start();
    }

    private void animateOut() {
        mHasShown = false;
        mPanel.show(false);
        cancelAnimation();
    }

    @Override
    protected void onDraw(final Canvas canvas) {
        final boolean pieBottom = mPanelOrientation == Gravity.BOTTOM;
        if (mOpen) {
            int state;

            // draw background
            final float backgroundFraction = mBackgroundAnimator.getAnimatedFraction();
            canvas.drawARGB((int) (backgroundFraction * 0xcc), 0, 0, 0);

            if (isAllowedToDraw()) {
                // draw clock, date, battery level and line
                mClockPaint.setAlpha((int) (backgroundFraction * 0xff));
                mStatusPaint.setAlpha((int) (backgroundFraction * 0xff));
                mLinePaint.setAlpha((int) (backgroundFraction * (mLineColor >> 24)));
                canvas.drawText(mClockText,
                        mWidth / 2 - mClockOffsetX,
                        mHeight / (pieBottom ? 2 : 4) - mClockOffsetY,
                        mClockPaint);
                canvas.drawText(mPieHelper.getSimpleDate(),
                        mWidth / 2 - mDateOffsetX,
                        mHeight / (pieBottom ? 2 : 4) - mDateOffsetY,
                        mStatusPaint);
                canvas.drawText(mBatteryText,
                        mWidth / 2 - mBatteryOffsetX,
                        mHeight / (pieBottom ? 2 : 4) -
                                (pieBottom ? mBatteryOffsetY : mBatteryOffsetYSide),
                        mStatusPaint);
                canvas.drawLine(mWidth / 2 - mLineLength / 2,
                        (pieBottom ? mHeight / 2 - mLineOffset :
                        mHeight / 4 + mLineOffsetSide),
                        mWidth / 2 + mLineLength / 2,
                        (pieBottom ? mHeight / 2 - mLineOffset :
                        mHeight / 4 + mLineOffsetSide), mLinePaint);

                // draw notification icons
                for (final ImageView view : mIconViews) {
                    view.setAlpha((int) (backgroundFraction * (mIconColor >> 24)));
                    setColor(view, mIconColor);
                }
            }

            // draw snap points and now on tap
            final int threshold = mOuterCircleRadius + mOuterCircleThickness;
            for (int i = 0; i < mNumberOfTogglePoints; i++) {
                final TogglePoint toggle = mTogglePoint[i];
                if (!toggle.isCurrentlyPossible(mCenterDistance > threshold)) continue;
                float fraction = 1f + (toggle.active ?
                        mToggleGrowAnimator.getAnimatedFraction() * 0.5f : 0f);
                toggle.draw(canvas, mToggleBackground, fraction, backgroundFraction);
                if (mToggleOutsideAnimator.isRunning()) {
                    final float snapDistance = (float) Math.sqrt(
                            Math.pow(toggle.x, 2) +
                            Math.pow(toggle.y, 2));
                    final float snapTouch = snapDistance < mSnapRadius *
                            7 ? 200 - (snapDistance * (200 - 40)
                            / (mSnapRadius * 7)) : 40;
                    final float toggleOutSideFraction =
                            mToggleOutsideAnimator.getAnimatedFraction();
                    mToggleExtraBackground.setAlpha((int) snapTouch);
                    fraction = 1f + (toggle.active ? toggleOutSideFraction * 0.8f : 0);
                    toggle.draw(canvas, mToggleExtraBackground, fraction, toggleOutSideFraction);
                }
            }

            final float pieMoveFraction = mPieMoveAnimator.getAnimatedFraction();
            final float pieGrowFraction = mPieGrowAnimator.getAnimatedFraction();
            final float circleCenterY = mCenter.y + mInnerCircleRadius -
                    (pieMoveFraction * mInnerCircleRadius);
            final float circleThickness = pieGrowFraction * mOuterCircleThickness;
            final float circleRadius = pieMoveFraction * mOuterCircleRadius;

            // draw background circle
            state = canvas.save();
            canvas.drawCircle(mCenter.x, circleCenterY, circleRadius, mBackgroundPaint);
            canvas.restoreToCount(state);

            // draw outer circle
            state = canvas.save();
            final Path outerCirclePath = new Path();
            outerCirclePath.addCircle(mCenter.x, circleCenterY,
                    circleRadius + circleThickness, Direction.CW);
            outerCirclePath.close();
            outerCirclePath.addCircle(mCenter.x, circleCenterY,
                    circleRadius - circleThickness, Direction.CW);
            outerCirclePath.close();
            outerCirclePath.setFillType(Path.FillType.EVEN_ODD);
            canvas.drawPath(outerCirclePath, mCirclePaint);
            canvas.restoreToCount(state);

            // draw inner circle
            state = canvas.save();
            canvas.drawCircle(mCenter.x, circleCenterY, mInnerCircleRadius, mCirclePaint);
            canvas.restoreToCount(state);

            // draw base menu
            for (final PieItem item : mItems) {
                if (!canItemDisplay(item)) continue;
                drawItem(canvas, item, pieMoveFraction);
            }

            invalidateOutline();
        }
    }

    private void drawItem(Canvas canvas, PieItem item, float fraction) {
        if (item.getView() != null) {
            item.setColor(mBackgroundColor);
            final int itemOffset = item.getSize() / 2;
            final Point start = new Point(mCenter.x - itemOffset, mCenter.y + itemOffset);
            int state = canvas.save();
            ImageView view = (ImageView) item.getView();
            canvas.translate(start.x + (fraction * (view.getX() - start.x)),
                    start.y + (fraction * (view.getY() - start.y)));
            view.draw(canvas);
            canvas.restoreToCount(state);
        }
    }

    private Path makeSlice(float start, float end, int outer,
                           int inner, Point center, float narrow, boolean bothEnds) {
        RectF bb = new RectF(center.x - outer, center.y - outer,
                center.x + outer, center.y + outer);
        RectF bbi = new RectF(center.x - inner, center.y - inner,
                center.x + inner, center.y + inner);
        Path path = new Path();
        path.arcTo(bb, start, end - start, true);
        path.arcTo(bbi, end + narrow, start - end - (bothEnds ? narrow : narrow * 2));
        path.close();
        return path;
    }

    private boolean isAllowedToDraw() {
        KeyguardManager mKeyguardManger = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        final int immersiveModeAllowsPie = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.SYSTEM_DESIGN_FLAGS, 0);
        final boolean isKeyguardSecure = !mKeyguardManger.isKeyguardLocked()
                || !mKeyguardManger.isKeyguardSecure();
        return isKeyguardSecure && immersiveModeAllowsPie !=
                View.SYSTEM_DESIGN_FLAG_IMMERSIVE_NAV;
    }

    // touch handling for pie
    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        if (evt.getPointerCount() > 1) return true;
        float mX = evt.getRawX();
        float mY = evt.getRawY();
        float distanceX = mCenter.x - mX;
        float distanceY = mCenter.y - mY;
        mCenterDistance = (float) Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
        float shadeThreshold = mOuterCircleRadius + mOuterCircleThickness;

        int action = evt.getActionMasked();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                // open panel
                animateInStartup();
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < mNumberOfTogglePoints; i++) {
                    TogglePoint toggle = mTogglePoint[i];
                    if (!toggle.isCurrentlyPossible(true)) continue;

                    float toggleDistanceX = toggle.x - mX;
                    float toggleDistanceY = toggle.y - mY;
                    float toggleDistance = (float)
                            Math.sqrt(Math.pow(toggleDistanceX, 2) + Math.pow(toggleDistanceY, 2));

                    if (toggleDistance < toggle.radius && isAllowedToDraw()) {
                        if (!toggle.active) {
                            mToggleGrowAnimator.cancel();
                            mToggleGrowAnimator.start();
                            if (mHapticFeedback) mVibrator.vibrate(2);
                        }
                        toggle.active = true;
                    } else {
                        if (toggle.active) {
                            mToggleGrowAnimator.cancel();
                        }
                        toggle.active = false;
                    }
                }

                // trigger the shades
                if (mCenterDistance > shadeThreshold && isAllowedToDraw()) {
                    if (!mHasShown) animateInRest();
                    deselect();
                }

                // take back shade trigger if user decides to abandon his gesture
                if (mCenterDistance < shadeThreshold) {
                    // check for onEnter separately or'll face constant deselect
                    PieItem item = findItem(getPolar(mX, mY));
                    if (item != null) {
                        if (mCenterDistance < shadeThreshold &&
                                mCenterDistance > mInnerCircleRadius) {
                            onEnter(item);
                        } else {
                            deselect();
                        }
                    }
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (mOpen) {
                    PieItem item = mCurrentItem;

                    // check for click actions
                    if (item != null && item.getView() != null && mCenterDistance < shadeThreshold) {
                        if (mHapticFeedback) {
                            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        }
                        item.getView().performClick();
                        if (item.getName().equals(PieController.RECENT_BUTTON) ||
                                item.getName().equals(PieController.HOME_BUTTON)) {
                            try {
                                WindowManagerGlobal.getWindowManagerService().dismissKeyguard();
                            } catch (RemoteException ex) {
                                // system is dead
                            }
                        }
                    }
                }

                // say good bye
                deselect();
                animateOut();
                return true;
        }
        // always re-dispatch event
        return false;
    }

    private void onEnter(PieItem item) {
        if (mCurrentItem == item) return;

        // deselect
        if (mCurrentItem != null) {
            mCurrentItem.setSelected(false);
        }
        if (item != null) {
            // clear up stack
            item.setSelected(true);
            mCurrentItem = item;
        } else {
            mCurrentItem = null;
        }
    }

    private void deselect() {
        if (mCurrentItem != null) {
            mCurrentItem.setSelected(false);
        }
        mCurrentItem = null;
    }

    private float getPolar(float x, float y) {
        float deltaY = mCenter.y - y;
        float deltaX = mCenter.x - x;
        float adjustAngle = 0;
        switch (mPanelOrientation) {
            case Gravity.LEFT:
                adjustAngle = 90;
                break;
            case Gravity.RIGHT:
                adjustAngle = -90;
                break;
        }
        return (adjustAngle + (float) Math.atan2(deltaX,
                deltaY) * 180 / (float) Math.PI) * (float) Math.PI / 180;
    }

    private PieItem findItem(float polar) {
        if (mItems != null) {
            for (PieItem item : mItems) {
                if (!canItemDisplay(item)) continue;
                if (inside(polar, item)) {
                    return item;
                }
            }
        }
        return null;
    }

    private boolean inside(float polar, PieItem item) {
        return (item.getStartAngle() < polar) && (item.getStartAngle() + item.getSweep() > polar);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        mWidth = w;
        mHeight = h;
        setOutlineProvider(new CustomOutline());
    }

    private abstract class TogglePoint {
        public boolean active;
        public final int radius;
        public final int x;
        public final int y;

        TogglePoint(int toggleX, int toggleY, int toggleRadius) {
            x = toggleX;
            y = toggleY;
            radius = toggleRadius;
            active = false;
        }

        public void draw(Canvas canvas, Paint paint, float growFraction, float
                alphaFraction) {
            int growRadius = (int) (radius * growFraction);
            paint.setAlpha((int) (alphaFraction * 0xff));
            Path circle = new Path();
            circle.addCircle(x, y, growRadius, Path.Direction.CW);
            canvas.drawPath(circle, paint);
        }

        public abstract boolean isCurrentlyPossible(boolean trigger);
    }

    private class SnapPoint extends TogglePoint {
        public final int gravity;

        SnapPoint(int snapX, int snapY, int snapRadius, int snapGravity) {
            super(snapX, snapY, snapRadius);
            gravity = snapGravity;
        }

        /**
         * @return whether the gravity of this snap point is usable under the current conditions
         */
        public boolean isCurrentlyPossible(boolean trigger) {
            return (trigger && mPanel.isGravityPossible(gravity));
        }
    }

    private class NowOnTapPoint extends TogglePoint {
        private final ImageView mLogo;

        NowOnTapPoint(int notX, int notY, int notRadius, ImageView logo, int logoSize) {
            super(notX, notY, notRadius);

            logo.setMinimumWidth(logoSize);
            logo.setMinimumHeight(logoSize);
            logo.setScaleType(ScaleType.FIT_XY);
            RelativeLayout.LayoutParams lp = new
                    RelativeLayout.LayoutParams(logoSize, logoSize);
            if (mPanelOrientation == Gravity.BOTTOM)
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            lp.leftMargin = notX - logoSize / 2;
            lp.topMargin = notY - logoSize / 2;
            logo.setLayoutParams(lp);
            mLogo = logo;
        }

        @Override
        public void draw(Canvas canvas, Paint paint, float growFraction, float
                alphaFraction) {
            super.draw(canvas, paint, growFraction, alphaFraction);
            mLogo.setAlpha(alphaFraction);
        }

        public boolean isCurrentlyPossible(boolean trigger) {
            return true;
        }
    }

    private class CustomOutline extends ViewOutlineProvider {

        private final float mPadding;

        CustomOutline() {
            mPadding = mResources.getDimensionPixelSize(R.dimen.pie_elevation);
        }

        @Override
        public void getOutline(View view, Outline outline) {
            float circleCenterY = mCenter.y + mInnerCircleRadius -
                    (mPieMoveAnimator.getAnimatedFraction() * mInnerCircleRadius);
            float circleThickness = mPieGrowAnimator.getAnimatedFraction()
                    * mOuterCircleThickness;
            float circleRadius = mPieMoveAnimator.getAnimatedFraction()
                    * mOuterCircleRadius;
            int size = (int) (circleRadius + circleThickness + mPadding);
            final Path outerCirclePath = new Path();
            outerCirclePath.addCircle(0, 0,
                    circleRadius + circleThickness + mPadding, Direction.CW);
            outerCirclePath.close();
            outline.setConvexPath(outerCirclePath);
            outline.setOval(-size, -size, size, size);
            outline.offset(mCenter.x, mCenter.y);
        }
    }
}
