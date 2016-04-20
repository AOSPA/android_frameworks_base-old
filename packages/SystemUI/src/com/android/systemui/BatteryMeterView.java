/*
 * Copyright (C) 2013 The Android Open Source Project
 * Copyright (C) 2014 The CyanogenMod Project
 * Copyright (C) 2016 The ParanoidAndroid Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryMeterView extends View implements DemoMode,
        BatteryController.BatteryStateChangeCallback {
    public static final String TAG = BatteryMeterView.class.getSimpleName();
    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";

    private static final boolean SINGLE_DIGIT_PERCENT = false;
    private static final boolean SHOW_100_PERCENT = false;

    private static final int FULL = 96;

    private static final float BOLT_LEVEL_THRESHOLD = 0.3f;  // opaque bolt below this fraction

    private final int[] mColors;

    protected boolean mShowPercent = true;

    private float mButtonHeightFraction;
    private float mSubpixelSmoothingLeft;
    private float mSubpixelSmoothingRight;
    private int mIconTint = Color.WHITE;

    public static enum BatteryMeterMode {
        BATTERY_METER_GONE,
        BATTERY_METER_ICON_PORTRAIT,
        BATTERY_METER_CIRCLE,
        BATTERY_METER_TEXT
    }

    private int mHeight;
    private int mWidth;

    private String mWarningString;
    private final int mCriticalLevel;

    private final int mFrameColor;

    private boolean mAnimationsEnabled;

    private final Path mShapePath = new Path();
    private final Path mClipPath = new Path();
    private final Path mTextPath = new Path();

    private BatteryController mBatteryController;
    private boolean mPowerSaveEnabled;

    private int mDarkModeBackgroundColor;
    private int mDarkModeFillColor;

    private int mLightModeBackgroundColor;
    private int mLightModeFillColor;

    private final Handler mHandler;

    protected BatteryMeterMode mMeterMode = null;

    protected boolean mAttached;

    private boolean mDemoMode;
    protected BatteryTracker mDemoTracker = new BatteryTracker();
    protected BatteryTracker mTracker = new BatteryTracker();
    private BatteryMeterDrawable mBatteryMeterDrawable;
    private final Object mLock = new Object();

    public BatteryMeterView(Context context) {
        this(context, null, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BatteryMeterView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mHandler = new Handler();

        final Resources res = context.getResources();
        TypedArray atts = context.obtainStyledAttributes(attrs, R.styleable.BatteryMeterView,
                defStyle, 0);
        mFrameColor = atts.getColor(R.styleable.BatteryMeterView_frameColor,
                context.getColor(R.color.batterymeter_frame_color));
        TypedArray levels = res.obtainTypedArray(R.array.batterymeter_color_levels);
        TypedArray colors = res.obtainTypedArray(R.array.batterymeter_color_values);

        final int N = levels.length();
        mColors = new int[2*N];
        for (int i=0; i<N; i++) {
            mColors[2*i] = levels.getInt(i, 0);
            mColors[2*i+1] = colors.getColor(i, 0);
        }
        levels.recycle();
        colors.recycle();
        atts.recycle();
        mWarningString = context.getString(R.string.battery_meter_very_low_overlay_symbol);
        mCriticalLevel = mContext.getResources().getInteger(
                com.android.internal.R.integer.config_criticalBatteryWarningLevel);
        mButtonHeightFraction = context.getResources().getFraction(
                R.fraction.battery_button_height_fraction, 1, 1);
        mSubpixelSmoothingLeft = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_left, 1, 1);
        mSubpixelSmoothingRight = context.getResources().getFraction(
                R.fraction.battery_subpixel_smoothing_right, 1, 1);
        setAnimationsEnabled(true);
    }

    protected BatteryMeterDrawable createBatteryMeterDrawable(BatteryMeterMode mode) {
        Resources res = mContext.getResources();
        switch (mode) {
            case BATTERY_METER_CIRCLE:
                return new CircleBatteryMeterDrawable(res);
            case BATTERY_METER_TEXT:
            case BATTERY_METER_GONE:
                return null;
            default:
                return new NormalBatteryMeterDrawable(res);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (mMeterMode == BatteryMeterMode.BATTERY_METER_CIRCLE) {
            width = height;
            onSizeChanged(width, height, 0, 0);
        } else if (mMeterMode == BatteryMeterMode.BATTERY_METER_TEXT) {
            onSizeChanged(width, height, 0, 0); // Force a size changed event
        }

        setMeasuredDimension(width, height);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(ACTION_LEVEL_TEST);
        final Intent sticky = getContext().registerReceiver(mTracker, filter);
        if (sticky != null) {
            mTracker.onReceive(getContext(), sticky);
        }
        mBatteryController.addStateChangedCallback(this);
        mAttached = true;
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        mAttached = false;
        getContext().unregisterReceiver(mTracker);
        mBatteryController.removeStateChangedCallback(this);
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mPowerSaveEnabled = mBatteryController.isPowerSave();
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        // TODO: Use this callback instead of own broadcast receiver.
    }

    @Override
    public void onPowerSaveChanged() {
        mPowerSaveEnabled = mBatteryController.isPowerSave();
        invalidate();
    }

    public void setAnimationsEnabled(boolean enabled) {
        if (mAnimationsEnabled != enabled) {
            mAnimationsEnabled = enabled;
            setLayerType(mAnimationsEnabled ? LAYER_TYPE_HARDWARE : LAYER_TYPE_NONE, null);
            invalidate();
        }
    }

    @Override
    public void onBatteryStyleChanged(int style, int percentMode) {
        boolean showInsidePercent = percentMode == BatteryController.PERCENTAGE_MODE_INSIDE;
        BatteryMeterMode meterMode = BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT;

        switch (style) {
            case BatteryController.STYLE_CIRCLE:
                meterMode = BatteryMeterMode.BATTERY_METER_CIRCLE;
                break;
            case BatteryController.STYLE_GONE:
                meterMode = BatteryMeterMode.BATTERY_METER_GONE;
                showInsidePercent = false;
                break;
            case BatteryController.STYLE_TEXT:
                meterMode = BatteryMeterMode.BATTERY_METER_TEXT;
                showInsidePercent = false;
                break;
            default:
                break;
        }

        setMode(meterMode);
        mShowPercent = showInsidePercent;
        invalidateIfVisible();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        mHeight = h;
        mWidth = w;
        synchronized (mLock) {
            if (mBatteryMeterDrawable != null) {
                mBatteryMeterDrawable.onSizeChanged(w, h, oldw, oldh);
            }
        }
    }

    protected void invalidateIfVisible() {
        if (getVisibility() == View.VISIBLE && mAttached) {
            if (mAttached) {
                postInvalidate();
            } else {
                invalidate();
            }
        }
    }

    public void setMode(BatteryMeterMode mode) {
        if (mMeterMode == mode) {
            return;
        }

        mMeterMode = mode;
        BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
        if (mode == BatteryMeterMode.BATTERY_METER_GONE ||
                mode == BatteryMeterMode.BATTERY_METER_TEXT) {
            setVisibility(View.GONE);
            synchronized (mLock) {
                mBatteryMeterDrawable = null;
            }
        } else {
            synchronized (mLock) {
                if (mBatteryMeterDrawable != null) {
                    mBatteryMeterDrawable.onDispose();
                }
                mBatteryMeterDrawable = createBatteryMeterDrawable(mode);
            }
            if (mMeterMode == BatteryMeterMode.BATTERY_METER_ICON_PORTRAIT) {
                ((NormalBatteryMeterDrawable)mBatteryMeterDrawable).loadBoltPoints(
                        mContext.getResources());
            }
            if (tracker.present) {
                setVisibility(View.VISIBLE);
                postInvalidate();
                requestLayout();
            } else {
                setVisibility(View.GONE);
            }
        }
    }

    public int getColorForLevel(int percent) {

        // If we are in power save mode, always use the normal color.
        if (mPowerSaveEnabled) {
            return mColors[mColors.length-1];
        }
        int thresh, color = 0;
        for (int i=0; i<mColors.length; i+=2) {
            thresh = mColors[i];
            color = mColors[i+1];
            if (percent <= thresh) {

                // Respect tinting for "normal" level
                if (i == mColors.length-2) {
                    return mIconTint;
                } else {
                    return color;
                }
            }
        }
        return color;
    }

    public void setDarkIntensity(float darkIntensity) {
        int backgroundColor = getBackgroundColor(darkIntensity);
        int fillColor = getFillColor(darkIntensity);
        mIconTint = fillColor;
        // TODO: Fix this.
        // mFramePaint.setColor(backgroundColor);
        // mBoltPaint.setColor(fillColor);
        // mChargeColor = fillColor;
        invalidate();
    }

    private int getBackgroundColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeBackgroundColor, mDarkModeBackgroundColor);
    }

    private int getFillColor(float darkIntensity) {
        return getColorForDarkIntensity(
                darkIntensity, mLightModeFillColor, mDarkModeFillColor);
    }

    private int getColorForDarkIntensity(float darkIntensity, int lightColor, int darkColor) {
        return (int) ArgbEvaluator.getInstance().evaluate(darkIntensity, lightColor, darkColor);
    }

    @Override
    public void draw(Canvas c) {
        synchronized (mLock) {
            if (mBatteryMeterDrawable != null) {
                BatteryTracker tracker = mDemoMode ? mDemoTracker : mTracker;
                mBatteryMeterDrawable.onDraw(c, tracker);
            }
        }
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (getVisibility() == View.VISIBLE) {
            if (!mDemoMode && command.equals(COMMAND_ENTER)) {
                mDemoMode = true;
                mDemoTracker.level = mTracker.level;
                mDemoTracker.plugged = mTracker.plugged;
            } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
                mDemoMode = false;
                postInvalidate();
            } else if (mDemoMode && command.equals(COMMAND_BATTERY)) {
               String level = args.getString("level");
               String plugged = args.getString("plugged");
               if (level != null) {
                   mDemoTracker.level = Math.min(Math.max(Integer.parseInt(level), 0), 100);
               }
               if (plugged != null) {
                   mDemoTracker.plugged = Boolean.parseBoolean(plugged);
               }
               postInvalidate();
            }
        }
    }

    protected interface BatteryMeterDrawable {
        void onDraw(Canvas c, BatteryTracker tracker);
        void onSizeChanged(int w, int h, int oldw, int oldh);
        void onDispose();
    }

    protected class NormalBatteryMeterDrawable implements BatteryMeterDrawable {

        private boolean mDisposed;

        private final Paint mFramePaint, mBatteryPaint, mWarningTextPaint, mTextPaint, mBoltPaint;
        private float mTextHeight, mWarningTextHeight;

        private int mChargeColor;
        private final float[] mBoltPoints;
        private final Path mBoltPath = new Path();

        private final RectF mFrame = new RectF();
        private final RectF mButtonFrame = new RectF();
        private final RectF mBoltFrame = new RectF();

        public NormalBatteryMeterDrawable(Resources res) {
            super();

            mDisposed = false;

            mFramePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFramePaint.setColor(mFrameColor);
            mFramePaint.setDither(true);
            mFramePaint.setStrokeWidth(0);
            mFramePaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mBatteryPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBatteryPaint.setDither(true);
            mBatteryPaint.setStrokeWidth(0);
            mBatteryPaint.setStyle(Paint.Style.FILL_AND_STROKE);

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(font);
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mWarningTextPaint.setColor(mColors[1]);
            font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

            mChargeColor = mContext.getColor(R.color.batterymeter_charge_color);

            mBoltPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBoltPaint.setColor(mContext.getColor(R.color.batterymeter_bolt_color));
            mBoltPoints = loadBoltPoints(res);

            mDarkModeBackgroundColor =
                    mContext.getColor(R.color.dark_mode_icon_color_dual_tone_background);
            mDarkModeFillColor = mContext.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
            mLightModeBackgroundColor =
                    mContext.getColor(R.color.light_mode_icon_color_dual_tone_background);
            mLightModeFillColor = mContext.getColor(R.color.light_mode_icon_color_dual_tone_fill);
        }

        @Override
        public void onDraw(Canvas c, BatteryTracker tracker) {
            if (mDisposed) return;

            final int level = tracker.level;

            if (level == BatteryTracker.UNKNOWN_LEVEL) return;

            float drawFrac = (float) level / 100f;
            final int pt = getPaddingTop();
            final int pl = getPaddingLeft();
            final int pr = getPaddingRight();
            final int pb = getPaddingBottom();
            final int height = mHeight - pt - pb;
            final int width = mWidth - pl - pr;

            final int buttonHeight = (int) (height * mButtonHeightFraction);

            mFrame.set(0, 0, width, height);
            mFrame.offset(pl, pt);

                // button-frame: area above the battery body
            mButtonFrame.set(
                    mFrame.left + Math.round(width * 0.25f),
                    mFrame.top,
                    mFrame.right - Math.round(width * 0.25f),
                    mFrame.top + buttonHeight);

            mButtonFrame.top += mSubpixelSmoothingLeft;
            mButtonFrame.left += mSubpixelSmoothingLeft;
            mButtonFrame.right -= mSubpixelSmoothingRight;

            // frame: battery body area

            mFrame.top += buttonHeight;
            mFrame.left += mSubpixelSmoothingLeft;
            mFrame.top += mSubpixelSmoothingLeft;
            mFrame.right -= mSubpixelSmoothingRight;
            mFrame.bottom -= mSubpixelSmoothingRight;

            // set the battery charging color
            mBatteryPaint.setColor(tracker.plugged ? mChargeColor : getColorForLevel(level));

            if (level >= FULL) {
                drawFrac = 1f;
            } else if (level <= mCriticalLevel) {
                drawFrac = 0f;
            }

            final float levelTop;

            if (drawFrac == 1f) {
                levelTop = mButtonFrame.top;
            } else {
                levelTop = (mFrame.top + (mFrame.height() * (1f - drawFrac)));
            }

            // define the battery shape
            mShapePath.reset();
            mShapePath.moveTo(mButtonFrame.left, mButtonFrame.top);
            mShapePath.lineTo(mButtonFrame.right, mButtonFrame.top);
            mShapePath.lineTo(mButtonFrame.right, mFrame.top);
            mShapePath.lineTo(mFrame.right, mFrame.top);
            mShapePath.lineTo(mFrame.right, mFrame.bottom);
            mShapePath.lineTo(mFrame.left, mFrame.bottom);
            mShapePath.lineTo(mFrame.left, mFrame.top);
            mShapePath.lineTo(mButtonFrame.left, mFrame.top);
            mShapePath.lineTo(mButtonFrame.left, mButtonFrame.top);

            if (tracker.plugged) {
                // define the bolt shape
                final float bl = mFrame.left + mFrame.width() / 4.5f;
                final float bt = mFrame.top + mFrame.height() / 6f;
                final float br = mFrame.right - mFrame.width() / 7f;
                final float bb = mFrame.bottom - mFrame.height() / 10f;
                if (mBoltFrame.left != bl || mBoltFrame.top != bt
                        || mBoltFrame.right != br || mBoltFrame.bottom != bb) {
                    mBoltFrame.set(bl, bt, br, bb);
                    mBoltPath.reset();
                    mBoltPath.moveTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                    for (int i = 2; i < mBoltPoints.length; i += 2) {
                        mBoltPath.lineTo(
                                mBoltFrame.left + mBoltPoints[i] * mBoltFrame.width(),
                                mBoltFrame.top + mBoltPoints[i + 1] * mBoltFrame.height());
                    }
                    mBoltPath.lineTo(
                            mBoltFrame.left + mBoltPoints[0] * mBoltFrame.width(),
                            mBoltFrame.top + mBoltPoints[1] * mBoltFrame.height());
                }

                float boltPct =
                        (mBoltFrame.bottom - levelTop) / (mBoltFrame.bottom - mBoltFrame.top);
                boltPct = Math.min(Math.max(boltPct, 0), 1);
                if (boltPct <= BOLT_LEVEL_THRESHOLD) {
                    // draw the bolt if opaque
                    c.drawPath(mBoltPath, mBoltPaint);
                } else {
                    // otherwise cut the bolt out of the overall shape
                    mShapePath.op(mBoltPath, Path.Op.DIFFERENCE);
                }
            }

            // compute percentage text
            boolean pctOpaque = false;
            float pctX = 0, pctY = 0;
            String pctText = null;
            if (!tracker.plugged && level > mCriticalLevel && (mShowPercent
                    && !(tracker.level == 100 && !SHOW_100_PERCENT))) {
                mTextPaint.setColor(getColorForLevel(level));
                final float full = 0.45f;
                final float nofull = 0.6f;
                final float single = 0.75f;
                mTextPaint.setTextSize(height *
                        (SINGLE_DIGIT_PERCENT ? single
                                : (tracker.level == 100 ? full : nofull)));
                mTextHeight = -mTextPaint.getFontMetrics().ascent;
                pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (level/10) : level);
                pctX = mWidth * 0.5f;
                pctY = (mHeight + mTextHeight) * 0.47f;
                pctOpaque = levelTop > pctY;
                if (!pctOpaque) {
                    mTextPath.reset();
                    mTextPaint.getTextPath(pctText, 0, pctText.length(), pctX, pctY, mTextPath);
                    // cut the percentage text out of the overall shape
                    mShapePath.op(mTextPath, Path.Op.DIFFERENCE);
                }
            }

            // draw the battery shape background
            c.drawPath(mShapePath, mFramePaint);

            // draw the battery shape, clipped to charging level
            mFrame.top = levelTop;
            mClipPath.reset();
            mClipPath.addRect(mFrame,  Path.Direction.CCW);
            mShapePath.op(mClipPath, Path.Op.INTERSECT);
            c.drawPath(mShapePath, mBatteryPaint);

            if (!tracker.plugged) {
                if (level <= mCriticalLevel) {
                    // draw the warning text
                    final float x = mWidth * 0.5f;
                    final float y = (mHeight + mWarningTextHeight) * 0.48f;
                    c.drawText(mWarningString, x, y, mWarningTextPaint);
                } else if (pctOpaque) {
                    // draw the percentage text
                    c.drawText(pctText, pctX, pctY, mTextPaint);
                }
            }
        }

        @Override
        public void onDispose() {
            mHandler.removeCallbacks(mInvalidate);
            mDisposed = true;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            mHeight = h;
            mWidth = w;
            mWarningTextPaint.setTextSize(h * 0.75f);
            mWarningTextHeight = -mWarningTextPaint.getFontMetrics().ascent;
        }

        private float[] loadBoltPoints(Resources res) {
            final int[] pts = res.getIntArray(R.array.batterymeter_bolt_points);
            int maxX = 0, maxY = 0;
            for (int i = 0; i < pts.length; i += 2) {
                maxX = Math.max(maxX, pts[i]);
                maxY = Math.max(maxY, pts[i + 1]);
            }
            final float[] ptsF = new float[pts.length];
            for (int i = 0; i < pts.length; i += 2) {
                ptsF[i] = (float)pts[i] / maxX;
                ptsF[i + 1] = (float)pts[i + 1] / maxY;
            }
            return ptsF;
        }
    }

    protected class CircleBatteryMeterDrawable implements BatteryMeterDrawable {
        private boolean mDisposed;

        private int mCircleSize;
        private int mLevel;

        private float mTextX, mTextY;

        private Paint mTextPaint;
        private Paint mFrontPaint;
        private Paint mBackPaint;
        private Paint mWarningTextPaint;

        private ValueAnimator mAlphaAnimator;

        public CircleBatteryMeterDrawable(Resources res) {
            super();
            mDisposed = false;

            mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Typeface font = Typeface.create("sans-serif-condensed", Typeface.BOLD);
            mTextPaint.setTypeface(font);
            mTextPaint.setTextAlign(Paint.Align.CENTER);

            mFrontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mFrontPaint.setDither(true);

            mBackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mBackPaint.setColor(mFrameColor);
            mBackPaint.setDither(true);

            mWarningTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            mWarningTextPaint.setColor(mColors[1]);
            font = Typeface.create("sans-serif", Typeface.BOLD);
            mWarningTextPaint.setTypeface(font);
            mWarningTextPaint.setTextAlign(Paint.Align.CENTER);

            mDarkModeBackgroundColor =
                    mContext.getColor(R.color.dark_mode_icon_color_dual_tone_background);
            mDarkModeFillColor = mContext.getColor(R.color.dark_mode_icon_color_dual_tone_fill);
            mLightModeBackgroundColor =
                    mContext.getColor(R.color.light_mode_icon_color_dual_tone_background);
            mLightModeFillColor = mContext.getColor(R.color.light_mode_icon_color_dual_tone_fill);
        }

        @Override
        public void onDraw(Canvas canvas, BatteryTracker tracker) {
            if (mDisposed) return;

            boolean unknownStatus = tracker.status == BatteryManager.BATTERY_STATUS_UNKNOWN;
            mLevel = tracker.level;
            Paint paint;

            if (unknownStatus) {
                paint = mBackPaint;
                mLevel = 100; // Draw all the circle;
            } else {
                paint = mFrontPaint;
                if (tracker.status == BatteryManager.BATTERY_STATUS_FULL) {
                    mLevel = 100;
                }

                if (tracker.plugged && mLevel < 100) {
                    startAnimation();
                } else {
                    paint.setColor(getColorForLevel(mLevel));
                    paint.setAlpha(255);
                    stopAnimation();
                }
            }

            canvas.drawCircle(mCircleSize / 2f, mCircleSize / 2f, mCircleSize / 2f, mBackPaint);
            canvas.drawCircle(mCircleSize / 2f, mCircleSize / 2f, mCircleSize / 2f * mLevel / 100f, paint);
            if (unknownStatus) {
                canvas.drawText("?", mTextX, mTextY, mTextPaint);
            } else if (!tracker.plugged) {
                if (mLevel > mCriticalLevel
                        && (mShowPercent && !(mLevel == 100 && !SHOW_100_PERCENT))) {
                    // draw the percentage text
                    String pctText = String.valueOf(SINGLE_DIGIT_PERCENT ? (mLevel/10) : mLevel);
                    canvas.drawText(pctText, mTextX, mTextY, mTextPaint);
                } else if (mLevel <= mCriticalLevel) {
                    // draw the warning text
                    canvas.drawText(mWarningString, mTextX, mTextY, mWarningTextPaint);
                }
            }
        }

        private void stopAnimation() {
            mHandler.removeCallbacks(mInvalidate);
            mHandler.removeCallbacks(mStartAnimRunnable);
            if (mAlphaAnimator != null) {
                mAlphaAnimator.cancel();
                mAlphaAnimator = null;
            }
        }

        private void startAnimation() {
            if (mAlphaAnimator != null) return;

            mFrontPaint.setColor(getColorForLevel(mLevel));
            mFrontPaint.setAlpha(0);
            mAlphaAnimator = ValueAnimator.ofFloat(0f, 1f, 0f);
            mAlphaAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mFrontPaint.setColor(getColorForLevel(mLevel));
                    mFrontPaint.setAlpha((int) (255f * (float) animation.getAnimatedValue()));
                    mHandler.post(mInvalidate);
                }
            });
            mAlphaAnimator.addListener(new AnimatorListenerAdapter() {
                boolean mCancelled;

                @Override
                public void onAnimationCancel(Animator animation) {
                    mCancelled = true;
                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    mAlphaAnimator = null;
                    if (!mCancelled) {
                        postDelayed(mStartAnimRunnable, 1500);
                    }
                }
            });
            mAlphaAnimator.setDuration(2000);
            mAlphaAnimator.start();
        }

        private final Runnable mStartAnimRunnable = new Runnable() {
            @Override
            public void run() {
                startAnimation();
            }
        };

        @Override
        public void onDispose() {
            stopAnimation();
            mDisposed = true;
        }

        @Override
        public void onSizeChanged(int w, int h, int oldw, int oldh) {
            mCircleSize = Math.min(w, h);
            mTextPaint.setTextSize(mCircleSize / 1.75f);
            mWarningTextPaint.setTextSize(mCircleSize / 2f);

            // calculate Y position for text
            Rect bounds = new Rect();
            mTextPaint.getTextBounds("99", 0, "99".length(), bounds);
            mTextX = mCircleSize / 2.0f + getPaddingLeft();
            // the +1dp at end of formula balances out rounding issues.works out on all resolutions
            mTextY = mCircleSize / 2.0f + bounds.height() / 2.0f - bounds.bottom;

            stopAnimation();
        }
    }

    private final class BatteryTracker extends BroadcastReceiver {
        public static final int UNKNOWN_LEVEL = -1;

        // current battery status
        boolean present = true;
        int level = UNKNOWN_LEVEL;
        String percentStr;
        int plugType;
        boolean plugged;
        int health;
        int status;
        String technology;
        int voltage;
        int temperature;
        boolean testmode = false;

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                if (testmode && ! intent.getBooleanExtra("testmode", false)) return;

                level = (int)(100f
                        * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                        / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));

                present = intent.getBooleanExtra(BatteryManager.EXTRA_PRESENT, true);
                plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
                plugged = plugType != 0;
                health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH,
                        BatteryManager.BATTERY_HEALTH_UNKNOWN);
                status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                        BatteryManager.BATTERY_STATUS_UNKNOWN);
                technology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);
                voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
                temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0);

                setContentDescription(
                        context.getString(R.string.accessibility_battery_level, level));
                synchronized (mLock) {
                    if (mBatteryMeterDrawable != null) {
                        setVisibility(View.VISIBLE);
                        invalidateIfVisible();
                    }
                }
            } else if (action.equals(ACTION_LEVEL_TEST)) {
                testmode = true;
                post(new Runnable() {
                    int curLevel = 0;
                    int incr = 1;
                    int saveLevel = level;
                    int savePlugged = plugType;
                    Intent dummy = new Intent(Intent.ACTION_BATTERY_CHANGED);
                    @Override
                    public void run() {
                        if (curLevel < 0) {
                            testmode = false;
                            dummy.putExtra("level", saveLevel);
                            dummy.putExtra("plugged", savePlugged);
                            dummy.putExtra("testmode", false);
                        } else {
                            dummy.putExtra("level", curLevel);
                            dummy.putExtra("plugged", incr > 0
                                    ? BatteryManager.BATTERY_PLUGGED_AC : 0);
                            dummy.putExtra("testmode", true);
                        }
                        getContext().sendBroadcast(dummy);

                        if (!testmode) return;

                        curLevel += incr;
                        if (curLevel == 100) {
                            incr *= -1;
                        }
                        postDelayed(this, 200);
                    }
                });
            }
        }

        protected boolean shouldIndicateCharging() {
            if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                return true;
            }
            if (plugged) {
                return status == BatteryManager.BATTERY_STATUS_FULL;
            }
            return false;
        }
    }

    private final Runnable mInvalidate = new Runnable() {
        public void run() {
            invalidateIfVisible();
        }
    };
}
