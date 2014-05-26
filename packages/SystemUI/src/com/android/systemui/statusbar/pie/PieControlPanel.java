/*
 * Copyright (C) 2014, ParanoidAndroid Project
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

package com.android.systemui.statusbar.pie;

import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Slog;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.FrameLayout;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.phone.PanelBar;
import com.android.systemui.statusbar.pie.PieController.OnNavButtonPressedListener;
import com.android.internal.statusbar.IStatusBarService;

/**
 * Pie control panel
 * Handles displaying pie and handling key codes
 * Must be initilized
 * On phones: Stores absolute gravity of Pie. All query methods return only
 *            relative gravity (depending on screen rotation).
 */
public class PieControlPanel extends FrameLayout implements OnNavButtonPressedListener {

    private boolean mShowing;
    private boolean mMenuButton;

    private int mInjectKeycode;
    private long mDownTime;
    private int mOrientation;
    private int mWidth;
    private int mHeight;

    private BaseStatusBar mStatusBar;
    private Rect mContentArea;
    private Context mContext;
    private Handler mHandler;
    private KeyguardManager mKeyguardManger;
    private ViewGroup mPieContentFrame;
    private PieController mPieController;
    private boolean mStickPieToScreenEdge;

    /* Analogous to NAVBAR_ALWAYS_AT_RIGHT */
    final static boolean PIE_ALWAYS_AT_RIGHT = true;

    public PieControlPanel(Context context) {
        this(context, null);
    }

    public PieControlPanel(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        mKeyguardManger = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mPieController = PieController.getInstance();
        mContentArea = new Rect();
        mOrientation = Gravity.BOTTOM;
        mMenuButton = false;
        mStickPieToScreenEdge = mContext.getResources().getBoolean(R.bool.config_stickPieToScreenEdge);
    }

    public boolean currentAppUsesMenu() {
        return mMenuButton;
    }

    public void setMenu(boolean state) {
        mMenuButton = state;
    }

    private int convertAbsoluteToRelativeGravity(int gravity) {
        if (mStickPieToScreenEdge) {
            int rot = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getRotation();

            if (rot == Surface.ROTATION_90 || rot == Surface.ROTATION_270) {
                switch (gravity) {
                    case Gravity.LEFT:
                    case Gravity.RIGHT:
                        gravity = Gravity.BOTTOM;
                        break;

                    case Gravity.BOTTOM:
                        gravity = PIE_ALWAYS_AT_RIGHT || rot == Surface.ROTATION_90 ?
                                Gravity.RIGHT : Gravity.LEFT;
                        break;
                }
            }
        }

        return gravity;
    }

    private int convertRelativeToAbsoluteGravity(int gravity) {
        if (mStickPieToScreenEdge) {
            int rot = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                    .getDefaultDisplay().getRotation();

            if (rot == Surface.ROTATION_90) {
                switch (gravity) {
                    case Gravity.LEFT:
                        gravity = Gravity.NO_GRAVITY;
                        break;

                    case Gravity.RIGHT:
                        gravity = Gravity.BOTTOM;
                        break;

                    case Gravity.BOTTOM:
                        gravity = Gravity.LEFT;
                        break;
                }
            } else if (rot == Surface.ROTATION_270) {
                switch (gravity) {
                    case Gravity.LEFT:
                        gravity = PIE_ALWAYS_AT_RIGHT ?
                                Gravity.NO_GRAVITY : Gravity.BOTTOM;
                        break;

                    case Gravity.RIGHT:
                        gravity = PIE_ALWAYS_AT_RIGHT ?
                                Gravity.BOTTOM : Gravity.NO_GRAVITY;
                        break;

                    case Gravity.BOTTOM:
                        gravity = Gravity.RIGHT;
                        break;
                }
            }
        }

        return gravity;
    }

    public int getOrientation() {
        return convertAbsoluteToRelativeGravity(mOrientation);
    }

    public int getDegree() {
        switch(convertAbsoluteToRelativeGravity(mOrientation)) {
            case Gravity.RIGHT:
                return 0;
            case Gravity.BOTTOM:
                return 90;
            case Gravity.LEFT:
                return 180;
        }
        return 0;
    }

    public boolean isGravityPossible(int gravity) {
        return convertRelativeToAbsoluteGravity(gravity) != Gravity.NO_GRAVITY;
    }

    public BaseStatusBar getBar() {
        return mStatusBar;
    }

    public void init(Handler h, BaseStatusBar statusbar, int orientation) {
        mHandler = h;
        mStatusBar = statusbar;
        mOrientation = orientation;
    }

    public static int convertGravitytoPieGravity(int gravity) {
        switch(gravity) {
            case Gravity.LEFT:
                return 1;
            case Gravity.RIGHT:
                return 2;
            default:
                return 0;
        }
    }

    public static int convertPieGravitytoGravity(int gravity) {
        switch(gravity) {
            case 1:
                return Gravity.LEFT;
            case 2:
                return Gravity.RIGHT;
            default:
                return Gravity.BOTTOM;
        }
    }

    public void reorient(int orientation) {
        mOrientation = convertRelativeToAbsoluteGravity(orientation);
        show(mShowing);
        Settings.System.putInt(mContext.getContentResolver(),
                    Settings.System.PIE_GRAVITY,
                            convertGravitytoPieGravity(mOrientation));
    }

    @Override
    public void onFinishInflate() {
        super.onFinishInflate();
        mPieContentFrame = (ViewGroup)findViewById(R.id.pie_content_frame);
        setWillNotDraw(false);
        mPieController.setControlPanel(this);
        show(false);
    }

    public boolean isShowing() {
        return mShowing;
    }

    public PointF getSize() {
        return new PointF(mWidth, mHeight);
    }

    public void show(boolean show) {
        mShowing = show;
        setVisibility(show ? View.VISIBLE : View.GONE);
        mPieController.show(show);
    }

    // we show pie always centered
    public void show() {
        mShowing = true;
        mStatusBar.preloadRecentApps();
        setVisibility(View.VISIBLE);
        Point outSize = new Point(0,0);
        WindowManager windowManager =
                (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        windowManager.getDefaultDisplay().getRealSize(outSize);
        mWidth = outSize.x;
        mHeight = outSize.y;
        switch(getOrientation()) {
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

    public boolean isInContentArea(int x, int y) {
        mContentArea.left = mPieContentFrame.getLeft() + mPieContentFrame.getPaddingLeft();
        mContentArea.right = mPieContentFrame.getRight() - mPieContentFrame.getPaddingRight();
        mContentArea.bottom = mPieContentFrame.getBottom() - mPieContentFrame.getPaddingBottom();

        return mContentArea.contains(x, y);
    }

    public void onNavButtonPressed(String buttonName) {
        if (buttonName.equals(PieController.BACK_BUTTON)) {
            injectKeyDelayed(KeyEvent.KEYCODE_BACK);
        } else if (buttonName.equals(PieController.HOME_BUTTON)) {
            injectKeyDelayed(KeyEvent.KEYCODE_HOME);
        } else if (buttonName.equals(PieController.MENU_BUTTON)) {
            injectKeyDelayed(KeyEvent.KEYCODE_MENU);
        } else if (buttonName.equals(PieController.RECENT_BUTTON)) {
            mStatusBar.toggleRecentApps();
        } else if (buttonName.equals(PieController.CLEAR_ALL_BUTTON)) {
            mStatusBar.clearRecentApps();
        }
    }

    public void injectKeyDelayed(int keycode){
        mInjectKeycode = keycode;
        mDownTime = SystemClock.uptimeMillis();
        mHandler.removeCallbacks(onInjectKeyDelayed);
        mHandler.postDelayed(onInjectKeyDelayed, 100);
        mStatusBar.cancelPreloadRecentApps();
    }

    final Runnable onInjectKeyDelayed = new Runnable() {
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

    public boolean isKeyguardSecureShowing() {
        return mKeyguardManger.isKeyguardLocked() && mKeyguardManger.isKeyguardSecure();
    }
}
