/*
 * Copyright (C) 2010, The Android Open Source Project
 * Copyright 2014 ParanoidAndroid Project
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
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Path;
import android.graphics.Path.Direction;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuff.Mode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Vibrator;
import android.provider.Settings;
import android.util.Log;
import android.util.Pair;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.ViewOutlineProvider;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
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
    private static final String FONT_FAMILY_MEDIUM = "sans-serif-medium";
    private static final String ROBOTO_REGULAR = "roboto:regular";

    private static int ANIMATOR_PIE_MOVE = 0;
    private static int ANIMATOR_PIE_GROW = 1;
    private static int ANIMATOR_BACKGROUND = 2;
    private static int ANIMATOR_TOGGLE_GROW = 3;

    private static final int OVERALL_SPEED = 300;
    private static final int GAP_BASE = 1;
    private static final int ANGLE_BASE = 12;
    private static final float SIZE_BASE = 1.0f;

    // colors
    private int mForegroundColor;
    private int mBackgroundColor;
    private int mIconColor;
    private int mLineColor;

    // structure
    private int mPanelDegree;
    private int mPanelOrientation;
    private int mPieAngle;
    private int mPieGap;
    private int mEmptyAngle = 0;
    private int mOuterCircleRadius;
    private int mOuterCircleThickness;
    private int mInnerCircleRadius;

    private float mPieSize;

    private Point mCenter = new Point(0, 0);
    private float mCenterDistance = 0;

    // paints
    private Paint mToggleBackground;
    private Paint mClockPaint;
    private Paint mStatusPaint;
    private Paint mLinePaint;
    private Paint mCirclePaint;
    private Paint mBackgroundPaint;

    private String mClockText;
    private String mBatteryText;
    private String mDateText;

    private float mClockOffsetX;
    private float mClockOffsetY;
    private float mDateOffsetX;
    private float mDateOffsetY;
    private float mBatteryOffsetX;
    private float mBatteryOffsetY;
    private float mBatteryOffsetYSide;
    private float mIconOffsetX;
    private float mIconOffsetY;
    private float mIconOffsetYside;
    private int mIconSize;
    private int mIconPadding;
    private int mNOTOffsetY;
    private int mNOTOffsetYside;
    private int mNOTOffsetX;
    private int mNOTOffsetXright;
    private int mLineLength;
    private int mLineOffset;
    private int mLineOffsetside;

    private int mWidth;
    private int mHeight;

    private Context mContext;
    private Resources mResources;
    private PieHelper mPieHelper;
    private Vibrator mVibrator;

    private PieItem mCurrentItem;
    private List<PieItem> mItems;
    private PieControlPanel mPanel;
    private List<Pair<String, Icon>> mIcons;
    private List<ImageView> mIconViews;
    private ImageView mNOTLogo;
    private KeyguardManager mKeyguardManger;

    private boolean mHasShown;
    private boolean mHasAssistant = false;

    private abstract class TogglePoint {
        public boolean active;
        public int radius;
        public int x;
        public int y;

        public TogglePoint(int toggleX, int toggleY, int toggleRadius) {
            x = toggleX;
            y = toggleY;
            radius = toggleRadius;
            active = false;
        }

        public void draw(Canvas canvas, Paint paint, float growFraction, float
                alphaFraction) {
            int growRadius = (int) (radius * growFraction);
            paint.setAlpha((int)(alphaFraction * 0xff));
            Path circle = new Path();
            circle.addCircle(x, y, growRadius, Path.Direction.CW);
            canvas.drawPath(circle, paint);
        }

        public abstract boolean isCurrentlyPossible(boolean trigger);
    }

    private class SnapPoint extends TogglePoint {
        public int gravity;

        public SnapPoint(int snapX, int snapY, int snapRadius, int snapGravity) {
            super(snapX, snapY, snapRadius);
            gravity = snapGravity;
        }

        /** @return whether the gravity of this snap point is usable under the current conditions */
        public boolean isCurrentlyPossible(boolean trigger) {
            return (trigger && mPanel.isGravityPossible(gravity));
        }
    }

    private class NowOnTapPoint extends TogglePoint {
        private ImageView mLogo;

        public NowOnTapPoint(int notX, int notY, int notRadius, ImageView logo, int logoSize) {
            super(notX, notY, notRadius);

            logo.setMinimumWidth(logoSize);
            logo.setMinimumHeight(logoSize);
            logo.setScaleType(ScaleType.FIT_XY);
            RelativeLayout.LayoutParams lp = new
                RelativeLayout.LayoutParams(logoSize, logoSize);
            switch (mPanelOrientation) {
                case Gravity.LEFT:
                case Gravity.RIGHT:
                    lp.leftMargin = notX - logoSize/2;
                    break;
                case Gravity.BOTTOM:
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    lp.leftMargin = notX - logoSize/2;
                    break;
            }
            lp.topMargin = notY - logoSize/2;
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

    private TogglePoint[] mTogglePoint = new TogglePoint[4];
    int mNumberOfTogglePoints;
    int mNumberOfSnapPoints;
    int mSnapRadius;
    int mSnapOffset;
    int mNOTRadius;
    int mNOTSize;

    private boolean mOpen;
    private boolean mHapticFeedback;

    private class CustomValueAnimator {

        public int index;
        public int duration;
        public boolean manual;
        public float fraction;
        public ValueAnimator animator;

        public CustomValueAnimator(int animateIndex) {
            index = animateIndex;
            manual = false;
            animator = ValueAnimator.ofInt(0, 1);
            animator.addUpdateListener(new CustomAnimatorUpdateListener(index));
            fraction = 0;
        }

        public void start() {
            if (!manual) {
                animator.setDuration(duration);
                animator.start();
            }
        }

        public void cancel() {
            animator.cancel();
            fraction = 0;
        }
    }

    private CustomValueAnimator[] mAnimators = new CustomValueAnimator[ANIMATOR_TOGGLE_GROW + 1];

    private float mX = 0;
    private float mY = 0;

    private void getDimensions() {
        // fetch colors
        mForegroundColor= mResources.getColor(R.color.pie_foreground);
        mBackgroundColor = mResources.getColor(R.color.pie_background);
        mIconColor = mResources.getColor(R.color.pie_icon);
        mLineColor = mResources.getColor(R.color.pie_line);

        // fetch orientation
        mPanelDegree = mPanel.getDegree();
        mPanelOrientation = mPanel.getOrientation();

        // fetch modes
        mHapticFeedback = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0;
        mHasAssistant = mPieHelper.isAssistantAvailable();

        // hardcode for now
        mPieAngle = ANGLE_BASE;
        mPieGap = GAP_BASE;
        mPieSize = SIZE_BASE;

        // snap
        mSnapRadius = mResources.getDimensionPixelSize(R.dimen.pie_snap_radius);
        mSnapOffset = mResources.getDimensionPixelSize(R.dimen.pie_snap_offset);

        Point outSize = new Point(0,0);
        WindowManager windowManager = (WindowManager)
                mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(outSize);
        mWidth = outSize.x;
        mHeight = outSize.y;

        int snapIndex = 0;
        if (mPanelOrientation != Gravity.LEFT && mPanel.isGravityPossible(Gravity.LEFT)) {
            mTogglePoint[snapIndex ++] = new SnapPoint(
                    0 - mSnapOffset, mHeight / 2, mSnapRadius, Gravity.LEFT);
        }

        if (mPanelOrientation != Gravity.RIGHT && mPanel.isGravityPossible(Gravity.RIGHT)) {
            mTogglePoint[snapIndex ++] = new SnapPoint(
                    mWidth + mSnapOffset, mHeight / 2, mSnapRadius, Gravity.RIGHT);
        }

        if ((!mPanel.isLandScape() || mPanel.isTablet()) && mPanelOrientation != Gravity.BOTTOM
                && mPanel.isGravityPossible(Gravity.BOTTOM)) {
            mTogglePoint[snapIndex ++] = new SnapPoint(
                    mWidth / 2, mHeight + mSnapOffset, mSnapRadius, Gravity.BOTTOM);
        }
        mNumberOfSnapPoints = snapIndex;

        // now on tap
        mNOTSize = (mResources.getDimensionPixelSize(R.dimen.pie_not_size));
        mNOTRadius = (mResources.getDimensionPixelSize(R.dimen.pie_not_radius));
        mNOTOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_not_offset);
        mNOTOffsetYside = mResources.getDimensionPixelSize(R.dimen.pie_not_offsetSide);
        mNOTOffsetX = mResources.getDimensionPixelSize(R.dimen.pie_not_offsetx);
        mNOTOffsetXright = mResources.getDimensionPixelSize(R.dimen.pie_not_offsetxright);
        setColor(mNOTLogo, mBackgroundColor);
        switch (mPanelOrientation) {
            case Gravity.LEFT:
                mTogglePoint[mNumberOfSnapPoints] = new NowOnTapPoint(mWidth/2 + mNOTOffsetX,
                        mHeight/2 + mNOTOffsetYside, mNOTRadius, mNOTLogo, mNOTSize);
                break;
            case Gravity.RIGHT:
                mTogglePoint[mNumberOfSnapPoints] = new NowOnTapPoint(mWidth/2 + mNOTOffsetXright,
                        mHeight/2 + mNOTOffsetYside, mNOTRadius, mNOTLogo, mNOTSize);
                break;
            case Gravity.BOTTOM:
                mTogglePoint[mNumberOfSnapPoints] = new NowOnTapPoint(mWidth/2,
                        mHeight/2 + mNOTOffsetY, mNOTRadius, mNOTLogo, mNOTSize);
                break;
        }
        mNumberOfTogglePoints = mNumberOfSnapPoints+1;

        // create pie
        mOuterCircleRadius = (int)mResources
            .getDimensionPixelSize(R.dimen.pie_outer_circle_radius);
        mOuterCircleThickness = (int)mResources
            .getDimensionPixelSize(R.dimen.pie_outer_circle_thickness);
        mInnerCircleRadius = (int)mResources
            .getDimensionPixelSize(R.dimen.pie_inner_circle_radius);

        // clock
        mClockPaint.setTextSize(mResources
                .getDimensionPixelSize(R.dimen.pie_clock_size));
        measureClock(mPieHelper.getSimpleTime());
        mClockOffsetY = mResources
                .getDimensionPixelSize(R.dimen.pie_clock_offset);

        // status (date and battery)
        mStatusPaint.setTextSize((int) (mResources
                .getDimensionPixelSize(R.dimen.pie_status_size)));

        // date
        mDateText = mPieHelper.getSimpleDate();
        mDateOffsetX = mStatusPaint.measureText(mDateText)/2;
        mDateOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_date_offset);

        // battery
        mBatteryText = "BATTERY LEVEL " + mPieHelper.getBatteryLevel() + "%";
        mBatteryOffsetX = mStatusPaint.measureText(mBatteryText)/2;
        mBatteryOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_battery_offset);
        mBatteryOffsetYSide = mResources.getDimensionPixelSize(R.dimen.pie_battery_offset_side);

        // line
        mLinePaint.setStrokeWidth(mResources.getDimensionPixelSize(R.dimen.pie_line_width));
        mLineLength = mResources.getDimensionPixelSize(R.dimen.pie_line_length);
        mLineOffset = mResources.getDimensionPixelSize(R.dimen.pie_line_offset);
        mLineOffsetside = mResources.getDimensionPixelSize(R.dimen.pie_line_offset_side);

        // notifications
        mIconSize = mResources.getDimensionPixelSize(R.dimen.pie_icon_size);
        mIconPadding = mResources.getDimensionPixelSize(R.dimen.pie_icon_padding);
        mIcons = mPieHelper.getNotificationIcons();
        mIconOffsetX = ((mIcons.size()*mIconSize)+((mIcons.size()-1)*mIconPadding))/2;
        mIconOffsetY = mResources.getDimensionPixelSize(R.dimen.pie_icon_offset);
        mIconOffsetYside = mResources.getDimensionPixelSize(R.dimen.pie_icon_offset_side);

        if (mIconViews == null) {
            mIconViews = new ArrayList<ImageView>();
        } else {
            for (View view : mIconViews) {
                removeView(view);
            }
            mIconViews.clear();
        }

        int iconPos = (int)mIconOffsetX;
        for (Pair<String, Icon> icon : mIcons) {
            ImageView view = new ImageView(mContext);
            try {
                view.setImageDrawable(icon.second.loadDrawable(
                            mContext.createPackageContext(icon.first,
                                Context.CONTEXT_IGNORE_SECURITY)));
            } catch (PackageManager.NameNotFoundException e) {
                Log.d("PIEMENU", "Could not load notification drawable", e);
            }
            setColor(view, mIconColor);
            view.setMinimumWidth(mIconSize);
            view.setMinimumHeight(mIconSize);
            view.setScaleType(ScaleType.FIT_XY);
            RelativeLayout.LayoutParams lp = new
                RelativeLayout.LayoutParams(mIconSize, mIconSize);
            switch (mPanelOrientation) {
                case Gravity.LEFT:
                case Gravity.RIGHT:
                    lp.topMargin = mHeight/4 + (int)mIconOffsetYside;
                    break;
                case Gravity.BOTTOM:
                    lp.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                    lp.topMargin = mHeight/2 + (int)mIconOffsetY;
                    break;
            }
            lp.leftMargin = mWidth/2 - (int)iconPos;
            view.setLayoutParams(lp);
            addView(view);
            mIconViews.add(view);
            iconPos -= mIconSize + mIconPadding;
        }

        mToggleBackground.setColor(mForegroundColor);
        mStatusPaint.setColor(mForegroundColor);
        mClockPaint.setColor(mForegroundColor);
        mLinePaint.setColor(mLineColor);
        mCirclePaint.setColor(mForegroundColor);
        mBackgroundPaint.setColor(mBackgroundColor);

        // create animators
        for (int i = 0; i < mAnimators.length; i++) {
            mAnimators[i] = new CustomValueAnimator(i);
        }

        // linear animators
        mAnimators[ANIMATOR_BACKGROUND].duration = (int)(OVERALL_SPEED * 1.5);
        mAnimators[ANIMATOR_BACKGROUND].animator.setInterpolator(new DecelerateInterpolator());

        mAnimators[ANIMATOR_TOGGLE_GROW].manual = true;
        mAnimators[ANIMATOR_TOGGLE_GROW].animator.setDuration(1000);
        mAnimators[ANIMATOR_TOGGLE_GROW].animator.setInterpolator(new AccelerateInterpolator());
        mAnimators[ANIMATOR_TOGGLE_GROW].animator.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) {
                if (mAnimators[ANIMATOR_TOGGLE_GROW].fraction == 1) {
                    for (int i = 0; i < mNumberOfTogglePoints; i++) {
                        TogglePoint toggle = mTogglePoint[i];
                        if (toggle != null && toggle.active && toggle.isCurrentlyPossible(true)) {
                            if(mHapticFeedback) mVibrator.vibrate(2);
                            deselect();
                            animateOut();
                            if (toggle instanceof NowOnTapPoint) {
                                if (mHasAssistant) mPieHelper.startAssistActivity();
                            } else if (toggle instanceof SnapPoint) {
                                mPanel.reorient(((SnapPoint)toggle).gravity);
                            }
                        }
                    }
                }
            }});

        // move circles in
        mAnimators[ANIMATOR_PIE_MOVE].duration = (int)(OVERALL_SPEED);
        mAnimators[ANIMATOR_PIE_MOVE].animator.setInterpolator(new DecelerateInterpolator());

        // grow outer circle
        mAnimators[ANIMATOR_PIE_GROW].duration = (int)(OVERALL_SPEED * 1.5);
        mAnimators[ANIMATOR_PIE_GROW].animator.setInterpolator(new DecelerateInterpolator());
    }

    public void setColor(ImageView view, int color) {
        Drawable drawable = view.getDrawable();
        drawable.setColorFilter(color, Mode.SRC_ATOP);
        view.setImageDrawable(drawable);
    }

    private void measureClock(String text) {
        mClockText = text;
        mClockOffsetX = mClockPaint.measureText(mClockText)/2;
    }

    /**
     * Creates a new pie outline view
     * @Param context the current context
     * @Param panel the current PieControlPanel
     */
    public PieMenu(Context context, PieControlPanel panel) {
        super(context);

        mContext = context;
        mResources = mContext.getResources();
        mPanel = panel;

        setWillNotDraw(false);
        setDrawingCacheEnabled(false);
        setElevation(mResources.getDimensionPixelSize(R.dimen.pie_elevation));
        //setTranslationZ(mResources.getDimensionPixelSize(R.dimen.pie_elevation));

        mVibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
        mPieHelper = PieHelper.getInstance();

        // initialize classes
        mItems = new ArrayList<PieItem>();

        mNOTLogo = new ImageView(mContext);
        mNOTLogo.setImageResource(R.drawable.ic_google_logo);
        addView(mNOTLogo);

        // initialize main paints
        mToggleBackground = new Paint();
        mToggleBackground.setAntiAlias(true);

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
        // drop shadow (doesn't support hardware acceleration)
        //mCirclePaint.setShadowLayer(4.0f, 0.0f, 2.0f, Color.BLACK);
        //setLayerType(LAYER_TYPE_SOFTWARE, mCirclePaint);

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
        float emptyangle = mEmptyAngle * (float)Math.PI / 180;
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
                (((1 - 0.65f) * lesserSweepCount) / (itemCount-lesserSweepCount)) : 0;
        float sweep = 0;
        float angle = 0;
        float total = 0;

        int count = 0;
        for (PieItem item : mItems) {
            if (!canItemDisplay(item)) continue;

            sweep = ((float) (Math.PI - 2 * emptyangle) /
                    itemCount) * (item.isLesser() ? 0.65f : 1 + adjustedSweep);
            angle = (emptyangle + sweep / 2 - (float) Math.PI/2);
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

                switch(mPanelOrientation) {
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
            float itemstart = total + angle - sweep / 2;
            item.setGeometry(itemstart, sweep, inner, outer);
            total += sweep;
            count++;
        }
    }

    // param angle from 0 - pi to android degrees (clockwise starting at 3)
    private float getDegrees(double angle) {
        return (float) (270 - 180 * angle / Math.PI);
    }

    private class CustomAnimatorUpdateListener implements ValueAnimator.AnimatorUpdateListener {

        private int mIndex;
        CustomAnimatorUpdateListener(int index) {
            mIndex = index;
        }

        @Override
        public void onAnimationUpdate(ValueAnimator animation) {
            float fraction = animation.getAnimatedFraction();
            mAnimators[mIndex].fraction = fraction;

            invalidate();
        }
    }

    private void cancelAnimation() {
        for (int i = 0; i < mAnimators.length; i++) {
            mAnimators[i].cancel();
            invalidate();
        }
    }

    private void animateInStartup() {
        // cancel & start startup animations
        cancelAnimation();
        for (int i = 0; i < ANIMATOR_PIE_GROW+1; i++) {
            mAnimators[i].start();
        }
    }

    private void animateInRest() {
        // start missing animations
        mHasShown = true;
        for (int i = ANIMATOR_PIE_GROW+1; i < mAnimators.length; i++) {
            mAnimators[i].start();
        }
    }

    public void animateOut() {
        mHasShown = false;
        mPanel.show(false);
        cancelAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mOpen) {
            int state;

            // draw background
            canvas.drawARGB((int)(mAnimators[ANIMATOR_BACKGROUND].fraction * 0xcc), 0, 0, 0);

            // draw clock, date battery level and line
            mClockPaint.setAlpha((int)(mAnimators[ANIMATOR_BACKGROUND].fraction * 0xff));
            mStatusPaint.setAlpha((int)(mAnimators[ANIMATOR_BACKGROUND].fraction * 0xff));
            mLinePaint.setAlpha((int)(mAnimators[ANIMATOR_BACKGROUND].fraction * (mLineColor >> 24)));
            switch (mPanelOrientation) {
                case Gravity.LEFT:
                case Gravity.RIGHT:
                    canvas.drawText(mClockText, mWidth/2-mClockOffsetX, mHeight/4-mClockOffsetY, mClockPaint);
                    canvas.drawText(mPieHelper.getSimpleDate(), mWidth/2-mDateOffsetX, mHeight/4-mDateOffsetY ,mStatusPaint);
                    canvas.drawText(mBatteryText, mWidth/2-mBatteryOffsetX, mHeight/4+mBatteryOffsetYSide, mStatusPaint);
                    canvas.drawLine(mWidth/2- mLineLength/2, mHeight/4+mLineOffsetside,
                                    mWidth/2+ mLineLength/2, mHeight/4+mLineOffsetside, mLinePaint);
                    break;
                case Gravity.BOTTOM:
                    canvas.drawText(mClockText, mWidth / 2- mClockOffsetX, mHeight / 2 - mClockOffsetY, mClockPaint);
                    canvas.drawText(mPieHelper.getSimpleDate(), mWidth / 2 - mDateOffsetX, mHeight / 2 - mDateOffsetY, mStatusPaint);
                    canvas.drawText(mBatteryText, mWidth/2-mBatteryOffsetX, mHeight/2-mBatteryOffsetY, mStatusPaint);
                    canvas.drawLine(mWidth/2- mLineLength/2, mHeight/2- mLineOffset,
                                    mWidth/2+ mLineLength/2, mHeight/2- mLineOffset, mLinePaint);
                    break;
            }
            // draw notification icons
            for (ImageView view : mIconViews) {
                view.setAlpha((int)(mAnimators[ANIMATOR_BACKGROUND].fraction * (mIconColor >> 24)));
                setColor(view, mIconColor);
            }

            // snap points and now on tap
            final int threshold = mOuterCircleRadius + mOuterCircleThickness;
            for (int i = 0; i < mNumberOfTogglePoints; i++) {
                TogglePoint toggle = mTogglePoint[i];
                if (!toggle.isCurrentlyPossible(mCenterDistance > threshold)) continue;

                float fraction = 1f
                        + (toggle.active ? mAnimators[ANIMATOR_TOGGLE_GROW].fraction * 2f : 0f);
                toggle.draw(canvas, mToggleBackground, fraction, mAnimators[ANIMATOR_BACKGROUND].fraction);
            }

            float circleCenterY = mCenter.y + mInnerCircleRadius - (mAnimators[ANIMATOR_PIE_MOVE].fraction * mInnerCircleRadius);
            float circleThickness = mAnimators[ANIMATOR_PIE_GROW].fraction * mOuterCircleThickness;
            float circleRadius = mAnimators[ANIMATOR_PIE_MOVE].fraction * mOuterCircleRadius;

            // draw background circle
            state = canvas.save();
            canvas.drawCircle(mCenter.x, circleCenterY, circleRadius, mBackgroundPaint);
            canvas.restoreToCount(state);

            // draw outer circle
            state = canvas.save();
            final Path outerCirclePath = new Path();

            outerCirclePath.addCircle(mCenter.x, circleCenterY, circleRadius+circleThickness, Direction.CW);
            outerCirclePath.close();

            outerCirclePath.addCircle(mCenter.x, circleCenterY, circleRadius-circleThickness, Direction.CW);
            outerCirclePath.close();

            outerCirclePath.setFillType(Path.FillType.EVEN_ODD);
            canvas.drawPath(outerCirclePath, mCirclePaint);
            canvas.restoreToCount(state);

            // draw inner circle
            state = canvas.save();
            canvas.drawCircle(mCenter.x, circleCenterY, mInnerCircleRadius, mCirclePaint);
            canvas.restoreToCount(state);

            // draw base menu
            for (PieItem item : mItems) {
                if (!canItemDisplay(item)) continue;
                drawItem(canvas, item, mAnimators[ANIMATOR_PIE_MOVE].fraction);
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
            ImageView view = (ImageView)item.getView();
            canvas.translate(start.x + (fraction * (view.getX() - start.x)),
                             start.y + (fraction * (view.getY() - start.y)));
            view.draw(canvas);
            canvas.restoreToCount(state);
        }
    }

    private Path makeSlice(float start, float end, int outer, int inner, Point center) {
        return makeSlice(start, end, outer, inner, center, 0, true);
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

    private boolean isKeyguardSecure() {
        mKeyguardManger = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        return mKeyguardManger.isKeyguardLocked() && mKeyguardManger.isKeyguardSecure();
    }

    // touch handling for pie
    @Override
    public boolean onTouchEvent(MotionEvent evt) {
        if (evt.getPointerCount() > 1) return true;
        mX = evt.getRawX();
        mY = evt.getRawY();
        float distanceX = mCenter.x - mX;
        float distanceY = mCenter.y - mY;
        mCenterDistance = (float) Math.sqrt(Math.pow(distanceX, 2) + Math.pow(distanceY, 2));
        float shadeTreshold = mOuterCircleRadius + mOuterCircleThickness;

        int action = evt.getActionMasked();
        if (MotionEvent.ACTION_DOWN == action) {
            // open panel
            animateInStartup();
        } else if (MotionEvent.ACTION_MOVE == action) {
            for (int i = 0; i < mNumberOfTogglePoints; i++) {
                TogglePoint toggle = mTogglePoint[i];
                if (!toggle.isCurrentlyPossible(true)) continue;

                float toggleDistanceX = toggle.x - mX;
                float toggleDistanceY = toggle.y - mY;
                float toggleDistance = (float)
                        Math.sqrt(Math.pow(toggleDistanceX, 2) + Math.pow(toggleDistanceY, 2));

                if (toggleDistance < toggle.radius && !isKeyguardSecure()) {
                    if (!toggle.active) {
                        mAnimators[ANIMATOR_TOGGLE_GROW].cancel();
                        mAnimators[ANIMATOR_TOGGLE_GROW].animator.start();
                        if (mHapticFeedback) mVibrator.vibrate(2);
                    }
                    toggle.active = true;
                } else {
                    if (toggle.active) {
                        mAnimators[ANIMATOR_TOGGLE_GROW].cancel();
                    }
                    toggle.active = false;
                }
            }

            // trigger the shades
            if (mCenterDistance > shadeTreshold && !isKeyguardSecure()) {
                if (!mHasShown) animateInRest();
                deselect();
            }

            // take back shade trigger if user decides to abandon his gesture
            if (mCenterDistance < shadeTreshold) {
                // check for onEnter separately or'll face constant deselect
                PieItem item = findItem(getPolar(mX, mY));
                if (item != null) {
                    if (mCenterDistance < shadeTreshold &&
                            mCenterDistance > mInnerCircleRadius) {
                        onEnter(item);
                    } else {
                        deselect();
                    }
                }
            }
            invalidate();
        } else if (MotionEvent.ACTION_UP == action) {
            if (mOpen) {
                PieItem item = mCurrentItem;

                // check for click actions
                if (item != null && item.getView() != null && mCenterDistance < shadeTreshold) {
                    if (mHapticFeedback) {
                        performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
                    }
                    item.getView().performClick();
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
        float adjustAngle = 0;;
        switch(mPanelOrientation) {
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
        return (item.getStartAngle() < polar)
        && (item.getStartAngle() + item.getSweep() > polar);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mWidth = w;
        mHeight = h;
        setOutlineProvider(new CustomOutline());
    }

    private class CustomOutline extends ViewOutlineProvider {

        private float mPadding;

        public CustomOutline() {
            mPadding = mResources.getDimensionPixelSize(R.dimen.pie_elevation);
        }

        @Override
        public void getOutline(View view, Outline outline) {
            float circleCenterY = mCenter.y + mInnerCircleRadius - (mAnimators[ANIMATOR_PIE_MOVE].fraction * mInnerCircleRadius);
            float circleThickness = mAnimators[ANIMATOR_PIE_GROW].fraction * mOuterCircleThickness;
            float circleRadius = mAnimators[ANIMATOR_PIE_MOVE].fraction * mOuterCircleRadius;
            int size = (int)(circleRadius+circleThickness+mPadding);
            final Path outerCirclePath = new Path();
            outerCirclePath.addCircle(0, 0, circleRadius+circleThickness+mPadding, Direction.CW);
            outerCirclePath.close();
            outline.setConvexPath(outerCirclePath);
            outline.setOval(-size, -size, size, size);
            outline.offset(mCenter.x, mCenter.y);
        }
    }
}
