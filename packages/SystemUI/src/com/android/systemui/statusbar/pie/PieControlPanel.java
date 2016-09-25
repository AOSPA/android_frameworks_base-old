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

import android.content.Context;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.*;
import android.widget.FrameLayout;
import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

/**
 * Pie control panel
 * Handles displaying pie and handling key codes
 * Must be initialized
 * On phones: Stores absolute gravity of Pie. All query methods return only
 * relative gravity (depending on screen rotation).
 */
public class PieControlPanel extends FrameLayout {

    /* Analogous to NAVBAR_ALWAYS_AT_RIGHT */
    private final static boolean PIE_ALWAYS_AT_RIGHT = false;
    private final PieController mPieController;
    private final boolean mRelocatePieOnRotation;
    private Context mContext;
    private Handler mHandler;
    private BaseStatusBar mStatusBar;
    private int mInjectKeycode;
    private long mDownTime;
    private final Runnable onInjectKeyDelayed = new Runnable() {
        public void run() {
            final long eventTime = SystemClock.uptimeMillis();
            InputManager.getInstance().injectInputEvent(
                    new KeyEvent(mDownTime, eventTime - 100,
                            KeyEvent.ACTION_DOWN, mInjectKeycode, 0),
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            InputManager.getInstance().injectInputEvent(
                    new KeyEvent(mDownTime, eventTime - 50, KeyEvent.ACTION_UP, mInjectKeycode, 0),
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    };
    private boolean mShowing;
    private int mOrientation;
    private int mWidth;
    private int mHeight;
    private int mRotation;
    private boolean mMenuButton;

    public PieControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mPieController = PieController.getInstance();
        mOrientation = Gravity.BOTTOM;
        mMenuButton = false;
        mRelocatePieOnRotation = mContext.getResources().getBoolean(
                R.bool.config_relocatePieOnRotation);
        mRotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation();
    }

    boolean currentAppUsesMenu() {
        return mMenuButton;
    }

    public void setMenu(boolean state) {
        mMenuButton = state;
    }

    private int convertAbsoluteToRelativeGravity(int gravity) {
        if (mRelocatePieOnRotation) {
            if (isLandScape()) {
                // only mess around with Pie in landscape
                if (PIE_ALWAYS_AT_RIGHT) {
                    // no questions asked if right is preferred
                    gravity = Gravity.RIGHT;
                } else if (gravity == Gravity.BOTTOM) {
                    // bottom is now right/left (depends on the direction of rotation)
                    gravity = mRotation == Surface.ROTATION_90 ? Gravity.RIGHT : Gravity.LEFT;
                } else if (isTablet()) {
                    // top can't be used so default to bottom
                    gravity = Gravity.BOTTOM;
                }
            }
        }
        return gravity;
    }

    protected boolean isTablet() {
        return mContext.getResources().getBoolean(R.bool.config_isTablet);
    }

    protected boolean isLandScape() {
        return mRotation == Surface.ROTATION_90 || mRotation == Surface.ROTATION_270;
    }

    private int convertRelativeToAbsoluteGravity(int gravity) {
        if (mRelocatePieOnRotation) {
            if (isLandScape()) {
                // only mess around with Pie in landscape
                if (PIE_ALWAYS_AT_RIGHT) {
                    // no questions asked if right is preferred
                    gravity = Gravity.RIGHT;
                } else {
                    // just stick to the edge when possible
                    switch (gravity) {
                        case Gravity.LEFT:
                            gravity = mRotation == Surface.ROTATION_90 ? Gravity.NO_GRAVITY : Gravity.BOTTOM;
                            break;
                        case Gravity.RIGHT:
                            gravity = mRotation == Surface.ROTATION_90 ? Gravity.BOTTOM : Gravity.NO_GRAVITY;
                            break;
                        case Gravity.BOTTOM:
                            gravity = mRotation == Surface.ROTATION_90 ? Gravity.LEFT : Gravity.RIGHT;
                            break;
                    }
                }
            }
        }

        return gravity;
    }

    protected int getOrientation() {
        return convertAbsoluteToRelativeGravity(mOrientation);
    }

    public int getDegree() {
        switch (convertAbsoluteToRelativeGravity(mOrientation)) {
            case Gravity.RIGHT:
                return 0;
            case Gravity.BOTTOM:
                return 90;
            case Gravity.LEFT:
                return 180;
        }
        return 0;
    }

    /**
     * Check whether the requested relative gravity is possible.
     *
     * @param gravity the Gravity value to check
     * @return whether the requested relative Gravity is possible
     * @see #isGravityPossible(int)
     */
    boolean isGravityPossible(int gravity) {
        if (mRelocatePieOnRotation) {
            if (isLandScape()) {
                if (PIE_ALWAYS_AT_RIGHT) return gravity == Gravity.RIGHT;
            }
        }

        return convertRelativeToAbsoluteGravity(gravity) != Gravity.NO_GRAVITY;
    }

    public BaseStatusBar getBar() {
        return mStatusBar;
    }

    public void init(Handler handler, BaseStatusBar statusbar, int orientation) {
        mHandler = handler;
        mStatusBar = statusbar;
        mOrientation = orientation;
        // Default to bottom if no pie gravity is set
        if (mOrientation != Gravity.BOTTOM && mOrientation != Gravity.RIGHT
                && mOrientation != Gravity.LEFT) {
            mOrientation = Gravity.BOTTOM;
        }
    }

    void reorient(int orientation) {
        mOrientation = convertRelativeToAbsoluteGravity(orientation);
        show(mShowing);
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.PIE_GRAVITY, mOrientation);
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        setWillNotDraw(false);
        mPieController.setControlPanel(this);
        show(false);
    }

    public boolean isShowing() {
        return mShowing;
    }

    protected void show(boolean show) {
        mShowing = show;
        setVisibility(show ? View.VISIBLE : View.GONE);
        mPieController.show(show);
    }

    // we show pie always centered
    public void show() {
        mShowing = true;
        mStatusBar.preloadRecentApps();
        setVisibility(View.VISIBLE);
        Point outSize = new Point(0, 0);
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(outSize);
        mWidth = outSize.x;
        mHeight = outSize.y;
        switch (getOrientation()) {
            case Gravity.LEFT:
                mPieController.setCenter(0, mHeight / 2);
                break;
            case Gravity.RIGHT:
                mPieController.setCenter(mWidth, mHeight / 2);
                break;
            default:
                mPieController.setCenter(mWidth / 2, mHeight);
                break;
        }
        mPieController.show(true);
    }

    public void onNavButtonPressed(String buttonName) {
        switch (buttonName) {
            case PieController.BACK_BUTTON:
                injectKeyDelayed(KeyEvent.KEYCODE_BACK);
                break;
            case PieController.HOME_BUTTON:
                injectKeyDelayed(KeyEvent.KEYCODE_HOME);
                break;
            case PieController.MENU_BUTTON:
                injectKeyDelayed(KeyEvent.KEYCODE_MENU);
                break;
            case PieController.RECENT_BUTTON:
                mStatusBar.toggleRecentApps();
                break;
        }
    }

    private void injectKeyDelayed(int keycode) {
        mInjectKeycode = keycode;
        mDownTime = SystemClock.uptimeMillis();
        mHandler.removeCallbacks(onInjectKeyDelayed);
        mHandler.postDelayed(onInjectKeyDelayed, 100);
        mStatusBar.cancelPreloadRecentApps();
    }
}
