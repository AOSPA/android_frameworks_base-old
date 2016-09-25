/*
 * Copyright 2014-2017 ParanoidAndroid Project
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
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.*;
import android.graphics.Path.Direction;
import android.graphics.PorterDuff.Mode;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Bundle;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.util.Log;
import android.util.Pair;
import android.view.*;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.RelativeLayout;

import com.android.internal.app.AssistUtils;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Pie menu
 * Handles creating, drawing, animations and touch eventing for pie.
 */
public class PieMenu extends RelativeLayout {
    private static final String TAG = PieMenu.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String FONT_FAMILY_LIGHT = "sans-serif-light";
    private static final String FONT_FAMILY_MEDIUM = "sans-serif-medium";

    private final Point mCenter = new Point(0, 0);

    // paints
    private final Paint mToggleBackground;
    private final Paint mToggleOuterBackground;
    private final Paint mClockPaint;
    private final Paint mStatusPaint;
    private final Paint mLinePaint;
    private final Paint mCirclePaint;
    private final Paint mBackgroundPaint;

    private final Context mContext;
    private final Resources mResources;
    private final Vibrator mVibrator;
    private final List<PieItem> mItems;
    private final ImageView mNOTLogo;
    private final TogglePoint[] mTogglePoint = new TogglePoint[4];
    private final BaseStatusBar mBar;
    private final PieController mPanel;

    // Colors
    private int mForegroundColor;
    private int mBackgroundColor;
    private int mIconColor;
    private int mLineColor;

    // Dimensions
    private int mPanelOrientation;
    private int mOuterCircleRadius;
    private int mOuterCircleThickness;
    private int mInnerCircleRadius;
    private float mShadeThreshold;
    private float mCenterDistance = 0;

    // Info texts
    private String mClockText;
    private String mBatteryText;
    private String mDateText;

    // Animators
    private ValueAnimator mPieBackgroundAnimator;
    private ValueAnimator mPieFadeAnimator;
    private ValueAnimator mPieGrowAnimator;
    private ValueAnimator mPieMoveAnimator;
    private ValueAnimator mToggleGrowAnimator;
    private ValueAnimator mToggleOuterGrowAnimator;

    // Animator fractions
    private float mBackgroundFraction;

    private int mOverallSpeed;

    // Offsets
    private float mClockOffsetX;
    private float mClockOffsetY;
    private float mDateOffsetX;
    private float mDateOffsetY;
    private float mBatteryOffsetX;
    private float mBatteryOffsetY;
    private float mBatteryOffsetYSide;

    private float mSweep;

    private int mLineLength;
    private int mLineOffset;
    private int mLineOffsetSide;
    private int mWidth;
    private int mHeight;

    // Icons
    private List<ImageView> mIconViews;

    private boolean mHasShown;

    private int mNumberOfTogglePoints;
    private int mNumberOfSnapPoints;

    private int mSnapRadius;
    private int mSnapOffset;

    private int mBackgroundAlpha;

    private int mBatteryLevel;

    private boolean mOpen;
    private boolean mHapticFeedback;
    private boolean mPieBottom;
    private boolean mRegistered;

    /**
     * Creates a new pie outline view
     *
     * @param context the current context
     * @param panel instance of piecontroller
     * @param bar instance of basestatusbar
     */
    public PieMenu(Context context, PieController panel, BaseStatusBar bar) {
        super(context);

        mContext = context;
        mResources = mContext.getResources();
        mBar = bar;
        mPanel = panel;

        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        setElevation(mResources.getDimensionPixelSize(R.dimen.pie_elevation));

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);

        registerReceivers();

        // initialize the items list
        mItems = new ArrayList<>();

        // initialize main paints
        mToggleBackground = new Paint();
        mToggleBackground.setAntiAlias(true);

        // outer snap point animation paint
        mToggleOuterBackground = new Paint();
        mToggleOuterBackground.setAntiAlias(true);

        // clock
        mClockPaint = new Paint();
        mClockPaint.setAntiAlias(true);
        mClockPaint.setTypeface(Typeface.create(FONT_FAMILY_LIGHT, Typeface.NORMAL));

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

        // background circle
        mBackgroundPaint = new Paint();
        mBackgroundPaint.setAntiAlias(true);

        mNOTLogo = new ImageView(mContext);

        // Check if assist is available before creating imageview
        if (isAssistantAvailable()) {
            // Now On Tap icon
            mNOTLogo.setImageResource(R.drawable.ic_google_logo);
            addView(mNOTLogo);
        }

        // Get all dimensions
        getDimensions();
    }

    /**
     * Adds a new pie item to the item list
     */
    protected void addItem(PieItem item) {
        mItems.add(item);
    }

    /**
     * Initializes current dimensions
     */
    private void getDimensions() {
        // fetch colors
        mForegroundColor = mResources.getColor(R.color.pie_foreground);
        mBackgroundColor = mResources.getColor(R.color.pie_background);
        mIconColor = mResources.getColor(R.color.pie_icon);
        mLineColor = mResources.getColor(R.color.pie_line);

        // fetch orientation
        mPanelOrientation = mPanel.getOrientation();
        mPieBottom = mPanelOrientation == Gravity.BOTTOM || isLandScape();

        // fetch modes
        mHapticFeedback = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;

        // snap
        mSnapRadius = mResources.getDimensionPixelSize(R.dimen.pie_snap_radius);
        mSnapOffset = mResources.getDimensionPixelSize(R.dimen.pie_snap_offset);

        // Pie animation speed
        mOverallSpeed = mResources.getInteger(R.integer.pie_animation_speed);

        mBackgroundAlpha = mResources.getInteger(R.integer.pie_background_alpha);

        mBackgroundFraction = 0.0f;

        Point outSize = new Point(0, 0);
        WindowManager windowManager = (WindowManager)
                mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(outSize);
        mWidth = outSize.x;
        mHeight = outSize.y;

        // Create snap points
        int snapIndex = 0;
        if (mPanelOrientation != Gravity.LEFT && mPanel.isGravityPossible(Gravity.LEFT)
                && !isLandScape()) {
            mTogglePoint[snapIndex++] = new SnapPoint(
                    0 - mSnapOffset, mHeight / 2, mSnapRadius, Gravity.LEFT);
        }

        if (mPanelOrientation != Gravity.RIGHT && mPanel.isGravityPossible(Gravity.RIGHT)
                && !isLandScape()) {
            mTogglePoint[snapIndex++] = new SnapPoint(
                    mWidth + mSnapOffset, mHeight / 2, mSnapRadius, Gravity.RIGHT);
        }

        if ((!isLandScape() || isTablet()) && mPanelOrientation != Gravity.BOTTOM &&
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
        mNumberOfTogglePoints = mNumberOfSnapPoints;
        if (isAssistantAvailable()) {
            setColor(mNOTLogo, mBackgroundColor);
            final boolean mPieRight = mPanelOrientation == Gravity.RIGHT;
            mTogglePoint[mNumberOfSnapPoints] = new NowOnTapPoint(
                    mWidth / 2 + (mPieBottom ? 0 :
                    (mPieRight ? mNOTOffsetXright : mNOTOffsetX)),
                    mHeight / (isLandScape() ? 4 : 2) + (mPieBottom ? mNOTOffsetY : mNOTOffsetYside),
                    mNOTRadius, mNOTLogo, mNOTSize);
            mNumberOfTogglePoints = mNumberOfSnapPoints + 1;
        }

        // create pie
        mOuterCircleRadius = mResources
                .getDimensionPixelSize(R.dimen.pie_outer_circle_radius);
        mOuterCircleThickness = mResources
                .getDimensionPixelSize(R.dimen.pie_outer_circle_thickness);
        mInnerCircleRadius = mResources
                .getDimensionPixelSize(R.dimen.pie_inner_circle_radius);
        mShadeThreshold = mOuterCircleRadius + mOuterCircleThickness;

        // clock
        mClockPaint.setTextSize(mResources
                .getDimensionPixelSize(R.dimen.pie_clock_size));
        measureClock(getSimpleTime());
        mClockOffsetY = mResources
                .getDimensionPixelSize(R.dimen.pie_clock_offset);

        // status (date and battery)
        mStatusPaint.setTextSize(mResources
                .getDimensionPixelSize(R.dimen.pie_status_size));

        // date
        mDateText = getSimpleDate();
        mDateOffsetX = mStatusPaint.measureText(mDateText) / 2;
        mDateOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_date_offset);

        // battery
        mBatteryText = mResources.getString(R.string.pie_battery_level)
                + mBatteryLevel + "%";
        mBatteryOffsetX = mStatusPaint.measureText(mBatteryText) / 2;
        mBatteryOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_battery_offset);
        mBatteryOffsetYSide = mResources.getDimensionPixelSize(R.dimen.pie_battery_offset_side);

        // line
        mLinePaint.setStrokeWidth(mResources.getDimensionPixelSize(R.dimen.pie_line_width));
        mLineLength = mResources.getDimensionPixelSize(R.dimen.pie_line_length);
        mLineOffset = mResources.getDimensionPixelSize(R.dimen.pie_line_offset);
        mLineOffsetSide = mResources.getDimensionPixelSize(R.dimen.pie_line_offset_side);

        mSweep = 0;

        // notifications
        updateNotifications(false);

        // Set colors
        mToggleBackground.setColor(mForegroundColor);
        mToggleOuterBackground.setColor(mForegroundColor);
        mStatusPaint.setColor(mForegroundColor);
        mClockPaint.setColor(mForegroundColor);
        mLinePaint.setColor(mLineColor);
        mCirclePaint.setColor(mForegroundColor);
        mBackgroundPaint.setColor(mForegroundColor);

        // background animator
        mPieBackgroundAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mPieBackgroundAnimator.setDuration(500);
        mPieBackgroundAnimator.addUpdateListener(new AnimatorUpdateListener(mPieBackgroundAnimator));

        // snappoint grow animator
        mToggleGrowAnimator = ValueAnimator.ofInt(0, 1);
        mToggleGrowAnimator.setDuration((int) (mOverallSpeed + 185));
        mToggleGrowAnimator.setInterpolator(new DecelerateInterpolator());
        mToggleGrowAnimator.setRepeatCount(1);
        mToggleGrowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mToggleGrowAnimator.addUpdateListener(new AnimatorUpdateListener(mToggleGrowAnimator));

        // outer snappoint grow animator
        mToggleOuterGrowAnimator = ValueAnimator.ofInt(0, 1);
        mToggleOuterGrowAnimator.setDuration((int) (mOverallSpeed - 10));
        mToggleOuterGrowAnimator.setRepeatCount(1);
        mToggleOuterGrowAnimator.setRepeatMode(ValueAnimator.REVERSE);
        mToggleOuterGrowAnimator.addUpdateListener(new AnimatorUpdateListener(mToggleOuterGrowAnimator));
        mToggleOuterGrowAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationCancel(Animator animation) {
                // Remove all listeners to prevent onAnimationEnd from being called.
                // This is a limitation from the API, so this is all we can do.
                animation.removeAllListeners();
            }
            @Override
            public void onAnimationEnd(Animator animation) {
                switchSnapPoints();
                // Don't keep any listeners alive
                animation.removeAllListeners();
            }
        });

        // circle move animator
        mPieMoveAnimator = ValueAnimator.ofInt(0, 1);
        mPieMoveAnimator.setInterpolator(new DecelerateInterpolator());
        mPieMoveAnimator.addUpdateListener(new AnimatorUpdateListener(mPieMoveAnimator));

        // outer circle animator
        mPieGrowAnimator = ValueAnimator.ofInt(0, 1);
        mPieGrowAnimator.setDuration((int) (mOverallSpeed * 1.5));
        mPieGrowAnimator.setInterpolator(new DecelerateInterpolator());
        mPieGrowAnimator.addUpdateListener(new AnimatorUpdateListener(mPieGrowAnimator));

        // Buttons fade-in animator
        mPieFadeAnimator = ValueAnimator.ofFloat(0.0f, 1.0f);
        mPieFadeAnimator.setDuration(500);
        mPieFadeAnimator.addUpdateListener(new AnimatorUpdateListener(mPieFadeAnimator));
    }

    /**
     * Switches the snap points
     */
    private void switchSnapPoints() {
        for (int i = 0; i < mNumberOfTogglePoints; i++) {
            TogglePoint toggle = mTogglePoint[i];
            if (toggle != null && toggle.active && toggle.isCurrentlyPossible(true)) {
                if (mHapticFeedback) mVibrator.vibrate(2);
                animateOut(false);
                if (toggle instanceof NowOnTapPoint) {
                    startAssist();
                } else if (toggle instanceof SnapPoint) {
                    mPanel.reorient(((SnapPoint)toggle).gravity);
                }
            }
        }
    }

    /**
     * Register time and battery receivers
     */
    private void registerReceivers() {
        if (mRegistered) {
            if (DEBUG) {
                Log.d(TAG, "Cannot registering receivers, already registered.");
            }
            return;
        }

        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_TIME_TICK);
        filter.addAction(Intent.ACTION_TIME_CHANGED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        mContext.registerReceiver(mReceiver, filter);
        mRegistered = true;
    }

    /**
     * Unregister time and battery receivers
     */
    private void unregisterReceivers() {
        if (!mRegistered) {
            if (DEBUG) {
                Log.d(TAG, "Cannot unregistering receivers, they've never been registered.");
            }
            return;
        }
        mContext.unregisterReceiver(mReceiver);
        mRegistered = false;
    }

    /**
     * Starts assist activity
     */
    private void startAssist() {
        if (isAssistantAvailable()) {
            mBar.startAssist(new Bundle());
        }
    }

    /**
     * Measures clock text
     */
    private void measureClock(String text) {
        mClockText = text;
        mClockOffsetX = mClockPaint.measureText(mClockText) / 2;
    }

    /**
     * Checks whether the PIE is currently showing
     */
    protected boolean isShowing() {
        return mOpen;
    }

    /**
     * Checks whether the current configuration is specified as for a tablet.
     */
    private boolean isTablet() {
        return mPanel.isTablet();
    }

    /**
     * Checks whether the current rotation is landscape or not.
     */
    private boolean isLandScape() {
        return mPanel.isLandScape();
    }

    /**
     * create notification icons
     */
    protected void updateNotifications(boolean update) {
        int iconSize = mResources.getDimensionPixelSize(R.dimen.pie_icon_size);
        int mIconPadding = mResources.getDimensionPixelSize(R.dimen.pie_icon_padding);
        List<Pair<String, Icon>> mIcons = mBar.getNotificationIcons();
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
                if (mPieBottom) {
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                }
                lp.topMargin = mHeight / (mPieBottom ? 2 : 4) +
                        (int) (mPieBottom ? iconOffsetY : iconOffsetYside);
                lp.leftMargin = mWidth / (isLandScape() ? 3 : 2) - iconPos;
                view.setLayoutParams(lp);
                addView(view);
                mIconViews.add(view);
                iconPos -= iconSize + mIconPadding;
                if (update) {
                    view.invalidate();
                }
            }
        }
    }

    /**
     * Shows the PIE
     */
    protected void show(boolean show) {
        mOpen = show;

        setVisibility(show ? View.VISIBLE : View.GONE);

        switch (mPanel.getOrientation()) {
            case Gravity.LEFT:
                mPanel.setCenter(0, mHeight / 2);
                break;
            case Gravity.RIGHT:
                mPanel.setCenter(mWidth, mHeight / 2);
                break;
            default:
                mPanel.setCenter(mWidth / 2, mHeight);
                break;
        }

        if (mOpen) {
            registerReceivers();
            getDimensions();
            layoutPie();
        } else {
            unregisterReceivers();
        }

        invalidate();
    }

    /**
     * Centers the PIE
     */
    protected void setCenter(int x, int y) {
        mCenter.y = y;
        mCenter.x = x;
    }

    /**
     * Sets the color of the imageviews
     */
    private void setColor(ImageView view, int color) {
        Drawable drawable = view.getDrawable();
        drawable.setColorFilter(color, Mode.SRC_ATOP);
        view.setImageDrawable(drawable);
    }

    /**
     * Layout the pie
     */
    private void layoutPie() {
        int itemCount = mItems.size();

        float angle = 0;
        float total = 0;

        for (PieItem item : mItems) {
            mSweep = ((float) (Math.PI - 2 * 0f) /
                    itemCount) * (item.isLesser() ? 0.65f : 1);
            angle = (0f + mSweep / 2 - (float) Math.PI / 2);
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
            float itemStart = total + angle - mSweep / 2;
            item.setGeometry(itemStart);
            total += mSweep;
        }
    }

    /**
     * Cancels all animations
     */
    private void cancelAnimation() {
        mPieBackgroundAnimator.cancel();
        mPieFadeAnimator.cancel();
        mToggleGrowAnimator.cancel();
        mToggleOuterGrowAnimator.cancel();
        mPieGrowAnimator.cancel();
        invalidate();
    }

    /**
     * Start the first animations
     */
    private void animateInStartup() {
        // cancel & start startup animations
        cancelAnimation();
        mPieMoveAnimator.start();
        mPieGrowAnimator.start();
        mPieFadeAnimator.setStartDelay(50);
        mPieFadeAnimator.start();
    }

    /**
     * Starts the rest of the animations
     */
    private void animateInRest() {
        // start missing animations
        mHasShown = true;
        mPieBackgroundAnimator.setStartDelay(250);
        mPieBackgroundAnimator.start();
    }

    /**
     * Animates the PIE out of the view
     */
    private void animateOut(boolean animate) {
        deselect();

        if (animate) {
            // Hook the listener up onto the main pie grow animator
            // since  this one is always available
            mPieGrowAnimator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mHasShown = false;
                    show(false);
                    cancelAnimation();
                }
            });

            // Cancel pie toggle animators
            mToggleGrowAnimator.cancel();
            mToggleOuterGrowAnimator.cancel();

            // Remove start delay because we set it when animating in
            mPieFadeAnimator.setStartDelay(0);
            if (mHasShown && isAllowedToDraw()) {
                mPieBackgroundAnimator.setStartDelay(0);
                mPieBackgroundAnimator.reverse();
            }

            // Reverse the animators
            mPieMoveAnimator.reverse();
            mPieGrowAnimator.reverse();
            mPieFadeAnimator.reverse();
        } else {
            // We aren't allowed to animate :(
            mHasShown = false;
            show(false);
            cancelAnimation();
        }
    }

    /**
     * Draws the PIE items
     */
    private void drawItem(Canvas canvas, PieItem item, float fraction) {
        if (item.getView() == null) return;
        final ImageView view = (ImageView) item.getView();
        final int itemOffset = item.getSize() / 2;
        final Point start = new Point(mCenter.x - itemOffset, mCenter.y + itemOffset);
        final int state = canvas.save();
        final float x = start.x + (fraction * (view.getX() - start.x));
        final float y = start.y + (fraction * (view.getY() - start.y));
        setColor(view, view.isSelected() ? Color.BLACK : mBackgroundColor);
        canvas.translate(x, y);
        view.setImageAlpha((int) (mPieFadeAnimator.getAnimatedFraction() * 0xff));
        view.draw(canvas);
        canvas.restoreToCount(state);
        if (DEBUG) {
            Log.d(TAG, "Drawing item: " + view.getTag());
        }
    }

    /**
     * Checks whether the PIE detail is allowed to show
     */
    private boolean isAllowedToDraw() {
        final int immersiveModeAllowsPie = Settings.Secure.getInt(
                mContext.getContentResolver(),
                Settings.Secure.SYSTEM_DESIGN_FLAGS, 0);
        return !mPanel.isKeyguardLocked() && immersiveModeAllowsPie !=
                View.SYSTEM_DESIGN_FLAG_IMMERSIVE_NAV;
    }

    /**
     * Deselects current pie item
     */
    private void deselect() {
        for (PieItem item : mItems) {
            if (item == null) return;
            item.setSelected(false);
        }
    }

    /**
     * Calculates the polar
     */
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

    /**
     * Broadcast receiver for battery and time
     */
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (Intent.ACTION_TIME_TICK.equals(action)
                    || Intent.ACTION_TIME_CHANGED.equals(action)) {
                measureClock(getSimpleTime());
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                mBatteryLevel = intent.getIntExtra("level", 0);
            }

            invalidate();
        }
    };

    /**
     * Get the current date in a simple format
     */
    private String getSimpleDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                mContext.getString(R.string.pie_date_format), Locale.getDefault());
        String date = sdf.format(new Date());
        return date.toUpperCase();
    }

    /**
     * Get the current time in a simple format
     */
    private String getSimpleTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(
                mContext.getString(DateFormat.is24HourFormat(mContext)
                        ? R.string.pie_hour_format_24
                        : R.string.pie_hour_format_12), Locale.getDefault());
        String time = sdf.format(new Date());
        return time.toUpperCase();
    }

    /**
     * @return whether assistant is currently available
     */
    private boolean isAssistantAvailable() {
        final AssistUtils mAssistUtils = new AssistUtils(mContext);
        return mAssistUtils.getAssistComponentForUser(UserHandle.USER_CURRENT) != null
                && mBar.getAssistManager() != null;
    }

    /**
     * Draws the PIE
     */
    @Override
    protected void onDraw(final Canvas canvas) {
        if (!mOpen) return;
        int state;

        mBackgroundPaint.setAlpha(mBackgroundAlpha);

        if (isAllowedToDraw()) {
            // draw background
            canvas.drawARGB((int) (mBackgroundFraction * 0xcc), 0, 0, 0);

            // draw clock, date, battery level and line
            mClockPaint.setAlpha((int) (mBackgroundFraction * 0xff));
            mStatusPaint.setAlpha((int) (mBackgroundFraction * 0xff));
            mLinePaint.setAlpha((int) (mBackgroundFraction * 0xff));
            canvas.drawText(mClockText,
                    mWidth / (isLandScape() ? 3 : 2) - mClockOffsetX,
                    mHeight / (mPieBottom ? 2 : 4)
                    - mClockOffsetY, mClockPaint);
            canvas.drawText(getSimpleDate(),
                    mWidth / (isLandScape() ? 3 : 2) - mDateOffsetX,
                    mHeight / (mPieBottom ? 2 : 4)
                    - mDateOffsetY, mStatusPaint);
            canvas.drawText(mBatteryText,
                    mWidth / (isLandScape() ? 3 : 2) - mBatteryOffsetX,
                    mHeight / (mPieBottom ? 2 : 4) -
                    (mPieBottom ? mBatteryOffsetY : mBatteryOffsetYSide), mStatusPaint);
            // Hide line when there are no notifications
            if (mBar.getNotificationCount() > 0) {
                canvas.drawLine(mWidth / (isLandScape() ? 3 : 2) - mLineLength / 2, (mPieBottom
                        ? mHeight / 2 - mLineOffset : mHeight / 4 + mLineOffsetSide),
                        mWidth / (isLandScape() ? 3 : 2) + mLineLength / 2,
                        (mPieBottom ? mHeight / 2 - mLineOffset
                        : mHeight / 4 + mLineOffsetSide), mLinePaint);
            }

            // draw notification icons
            for (ImageView view : mIconViews) {
                view.setAlpha((int) (mBackgroundFraction * 0xff));
                setColor(view, mIconColor);
            }
        }
        // draw snap points and now on tap
        for (int i = 0; i < mNumberOfTogglePoints; i++) {
            TogglePoint toggle = mTogglePoint[i];
            if (!toggle.isCurrentlyPossible(mCenterDistance > mShadeThreshold)) continue;
            boolean isNotPoint = toggle instanceof NowOnTapPoint;
            float toggleOuterAnimatorFraction = mToggleOuterGrowAnimator.getAnimatedFraction();
            float fraction = 1f + (toggle.active ?
                    mToggleGrowAnimator.getAnimatedFraction() * (isNotPoint ? 0.5f : 0.7f) : 0f);
            float toggleOuterfraction = (toggle.active ? 1f + toggleOuterAnimatorFraction
                    * (isNotPoint ? 0.9f : 1f) : 0);
            toggle.draw(canvas, mToggleBackground, fraction, mBackgroundFraction);
            // Only draw when outside animator is running.
            if (mToggleOuterGrowAnimator.isStarted()) {
                toggle.draw(canvas, mToggleOuterBackground, toggleOuterfraction,
                        toggleOuterAnimatorFraction / 2);
            }
            if (DEBUG) {
                Log.d(TAG, "Drawing snappoint: " + i);
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
        outerCirclePath.setFillType(Path.FillType.EVEN_ODD);
        canvas.drawPath(outerCirclePath, mCirclePaint);
        canvas.restoreToCount(state);

        // draw inner circle
        state = canvas.save();
        canvas.drawCircle(mCenter.x, circleCenterY, mInnerCircleRadius, mCirclePaint);
        canvas.restoreToCount(state);

        // draw base menu
        for (PieItem item : mItems) {
            drawItem(canvas, item, mPieMoveAnimator.getAnimatedFraction());
        }

        invalidateOutline();
    }

    /**
     * Touch handling for pie
     */
    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        if (evt.getPointerCount() > 1) return true;
        final float mX = evt.getRawX();
        final float mY = evt.getRawY();
        final float distanceX = mCenter.x - mX;
        final float distanceY = mCenter.y - mY;
        final float polar = getPolar(mX, mY);
        PieItem mItem = null;
        for (PieItem item : mItems) {
            if ((item.getStartAngle() < polar) && (item.getStartAngle() + mSweep > polar)) {
                mItem = item;
            }
        }
        mCenterDistance = (float) Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));

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
                            mToggleOuterGrowAnimator.cancel();
                            mToggleGrowAnimator.start();
                            if (mHapticFeedback) mVibrator.vibrate(2);
                        }
                        toggle.active = true;
                    } else {
                        if (toggle.active) {
                            mToggleGrowAnimator.cancel();
                            mToggleOuterGrowAnimator.cancel();
                        }
                        toggle.active = false;
                    }
                }

                // trigger the shades
                if (mCenterDistance > mShadeThreshold && isAllowedToDraw()) {
                    if (!mHasShown) {
                        animateInRest();
                    }
                    deselect();
                }
                if (mCenterDistance < mShadeThreshold && mCenterDistance > mInnerCircleRadius) {
                    if (mItem != null) {
                        // Deselect all items before selecting
                        deselect();
                        mItem.setSelected(true);
                    }
                } else {
                    deselect();
                }
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                if (mOpen) {
                    // check for click actions
                    if (mItem != null && mItem.getView() != null
                            && mCenterDistance < mShadeThreshold
                            && mItem.getView().isSelected()) {
                        if (mHapticFeedback &&
                                !mItem.getName().equals(PieController.BACK_BUTTON)) {
                            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                        }
                        mItem.getView().performClick();
                        if (mItem.getName().equals(PieController.RECENT_BUTTON) ||
                                mItem.getName().equals(PieController.HOME_BUTTON)) {
                            try {
                                WindowManagerGlobal.getWindowManagerService()
                                        .dismissKeyguard();
                            } catch (RemoteException ex) {
                                // system is dead
                            }
                        }
                    }
                }

                // say good bye
                animateOut(mItem.getView().isSelected() || mPanelOrientation != Gravity.BOTTOM // TODO: remove limitation on side-pie
                        ? false : true);
                return true;
        }
        // always re-dispatch event
        return false;
    }

    @Override
    protected void onAttachedToWindow() {
        setVisibility(View.GONE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        mWidth = w;
        mHeight = h;
        setOutlineProvider(new CustomOutline());
    }

    private class AnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {
        private ValueAnimator animationIndex;

        AnimatorUpdateListener(ValueAnimator index) {
            animationIndex = index;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            if (animationIndex == mPieBackgroundAnimator) {
                mBackgroundFraction = animation.getAnimatedFraction();
            }

            if (animationIndex == mToggleGrowAnimator
                    && animation.getAnimatedFraction() >= 0.95
                    && !mToggleOuterGrowAnimator.isRunning()) {
                mToggleOuterGrowAnimator.start();
            }

            invalidate();
        }
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
            canvas.drawCircle(x, y, growRadius, paint);
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
        @Override
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
            if (mPieBottom) {
                lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
            }
            lp.leftMargin = notX - logoSize / 2;
            lp.topMargin = notY - logoSize / 2;
            logo.setLayoutParams(lp);
            mLogo = logo;
        }

        @Override
        public void draw(Canvas canvas, Paint paint, float growFraction, float
                alphaFraction) {
            super.draw(canvas, paint, growFraction, alphaFraction);
            // Don't set alpha when outside animator is running
            if (!mToggleOuterGrowAnimator.isRunning()) {
                mLogo.setAlpha(alphaFraction);
            }
        }

        /**
         * @return whether the assist manager is currently available
         */
        @Override
        public boolean isCurrentlyPossible(boolean trigger) {
            return isAssistantAvailable();
        }
    }

    private class CustomOutline extends ViewOutlineProvider {

        private final float mPadding;

        CustomOutline() {
            mPadding = mResources.getDimensionPixelSize(R.dimen.pie_elevation);
        }

        @Override
        public void getOutline(View view, Outline outline) {
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
            outline.setAlpha(0.6f);
            outline.offset(mCenter.x, mCenter.y);
        }
    }
}
