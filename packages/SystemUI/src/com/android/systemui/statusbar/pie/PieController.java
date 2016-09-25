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

import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.app.StatusBarManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.gesture.EdgeGestureManager;
import android.view.*;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;

import com.android.internal.util.gesture.EdgeGesturePosition;
import com.android.internal.util.gesture.EdgeServiceConstants;

import com.android.systemui.R;
import com.android.systemui.statusbar.BaseStatusBar;

/**
 * Pie Controller
 * Sets and controls the pie menu and associated pie items
 * Singleton must be initialized.
 */
public class PieController extends EdgeGestureManager.EdgeGestureActivationListener {

    static final String BACK_BUTTON = "back";
    static final String HOME_BUTTON = "home";
    static final String RECENT_BUTTON = "recent";

    /* Analogous to NAVBAR_ALWAYS_AT_RIGHT */
    private final static boolean PIE_ALWAYS_AT_RIGHT = true;
    private boolean mRelocatePieOnRotation;

    private static PieController sInstance;
    private static AudioManager mAudioManager;
    private BaseStatusBar mBar;
    private Context mContext;
    private Handler mHandler;
    private WindowManager mWindowManager;
    private EdgeGestureManager mPieManager;
    private Point mEdgeGestureTouchPos = new Point(0, 0);
    private PieItem mBack;
    private PieItem mHome;
    private PieItem mRecent;
    private PieMenu mPie;
    private boolean mPieAttached;
    private boolean mForcePieCentered;

    private int mOrientation;
    private int mWidth;
    private int mHeight;
    private int mRotation;

    private int mPiePosition;
    private int mInjectKeycode;
    private long mDownTime;

    private PieController() {
        super(Looper.getMainLooper());
    }

    public static PieController getInstance() {
        if (sInstance == null)  {
            sInstance = new PieController();
        }
        return sInstance;
    }

    /**
     * Creates a new instance of pie
     *
     * @Param context the current Context
     * @Param wm the current Window Manager
     * @Param bar the current BaseStatusBar
     */
    public void init(Context context, WindowManager wm, BaseStatusBar bar) {
        mContext = context;
        mHandler = new Handler();
        mWindowManager = wm;
        mBar = bar;

        mOrientation = Gravity.BOTTOM;
        mRelocatePieOnRotation = mContext.getResources().getBoolean(
                R.bool.config_relocatePieOnRotation);
        mRotation = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE))
                .getDefaultDisplay().getRotation();

        mPieManager = EdgeGestureManager.getInstance();
        mPieManager.setEdgeGestureActivationListener(this);
        mForcePieCentered = mContext.getResources().getBoolean(R.bool.config_forcePieCentered);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void detachPie() {
         mBar.updatePieControls(!mPieAttached);
    }

    public void resetPie(boolean enabled, int gravity) {
        if (mPieAttached) {
            // Remove the view
            if (mPie != null) {
                mWindowManager.removeView(mPie);
            }
            mPieAttached = false;
        }
        if (enabled && showPie()) addPieInLocation(gravity);
    }

    private boolean showPie() {
        final boolean pieEnabled = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.PIE_STATE, 0, UserHandle.USER_CURRENT) == 1;
        final int immersiveMode = Settings.Secure.getIntForUser(mContext.getContentResolver(),
                Settings.Secure.SYSTEM_DESIGN_FLAGS, 0, UserHandle.USER_CURRENT);
        return pieEnabled && immersiveMode != 0
                && immersiveMode != View.SYSTEM_DESIGN_FLAG_IMMERSIVE_STATUS;
    }

    protected void initOrientation(int orientation) {
        mOrientation = orientation;
        // Default to bottom if no pie gravity is set
        if (mOrientation != Gravity.BOTTOM && mOrientation != Gravity.RIGHT
                && mOrientation != Gravity.LEFT) {
            mOrientation = Gravity.BOTTOM;
        }
        // Default to bottom on keyguard
        if (isKeyguardLocked() &&
                (mOrientation == Gravity.RIGHT || mOrientation == Gravity.LEFT)) {
            mOrientation = Gravity.BOTTOM;
        }
    }

    protected void reorient(int orientation) {
        mOrientation = convertRelativeToAbsoluteGravity(orientation);
        mPie.show(mPie.isShowing());
        Settings.Secure.putInt(mContext.getContentResolver(),
                Settings.Secure.PIE_GRAVITY, mOrientation);
    }

    protected boolean isKeyguardLocked() {
        final KeyguardManager mKeyguardManager = (KeyguardManager)
                      mContext.getSystemService(Context.KEYGUARD_SERVICE);
        return mKeyguardManager.isKeyguardLocked();
    }

    private void addPieInLocation(int gravity) {
        if (mPieAttached) return;

        // pie menu
        mPie = new PieMenu(mContext, sInstance, mBar);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
        lp.setTitle("PieMenu");
        lp.windowAnimations = android.R.style.Animation;

        createItems();

        // set gravity and touch
        initOrientation(gravity);

        // pie edge gesture
        mPiePosition = getOrientation();
        setupEdgeGesture(mPiePosition);

        // add pie view to windowmanager
        mWindowManager.addView(mPie, lp);
        mPieAttached = true;
    }

    private boolean activateFromListener(int touchX, int touchY) {
        if (!mPie.isShowing()) {
            mEdgeGestureTouchPos.x = touchX;
            mEdgeGestureTouchPos.y = touchY;
            mBar.preloadRecentApps();
            mPie.show(true);
            return true;
        }
        return false;
    }

    private void setupEdgeGesture(int gravity) {
        int triggerSlot = convertToEdgeGesturePosition(gravity);

        int sensitivity = mContext.getResources().getInteger(R.integer.pie_gesture_sensivity);
        if (sensitivity < EdgeServiceConstants.SENSITIVITY_LOWEST
                || sensitivity > EdgeServiceConstants.SENSITIVITY_HIGHEST) {
            sensitivity = EdgeServiceConstants.SENSITIVITY_DEFAULT;
        }

        mPieManager.updateEdgeGestureActivationListener(this,
                sensitivity << EdgeServiceConstants.SENSITIVITY_SHIFT
                        | triggerSlot
                        | EdgeServiceConstants.LONG_LIVING
                        | EdgeServiceConstants.UNRESTRICTED);
    }

    private int convertToEdgeGesturePosition(int gravity) {
        switch (gravity) {
            case Gravity.LEFT:
                return EdgeGesturePosition.LEFT.FLAG;
            case Gravity.RIGHT:
                return EdgeGesturePosition.RIGHT.FLAG;
            case Gravity.BOTTOM:
            default: // fall back
                return EdgeGesturePosition.BOTTOM.FLAG;
        }
    }

    private void createItems() {
        mBack = makeItem(R.drawable.ic_sysbar_back, BACK_BUTTON, false);
        mHome = makeItem(R.drawable.ic_sysbar_home, HOME_BUTTON, false);
        mRecent = makeItem(R.drawable.ic_sysbar_recent, RECENT_BUTTON, false);

        mPie.addItem(mRecent);
        mPie.addItem(mHome);
        mPie.addItem(mBack);
    }

    public void setNavigationIconHints(int hints) {
        if (mBack == null) return;

        boolean alt = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        mBack.setIcon(alt ? R.drawable.ic_sysbar_back_ime : R.drawable.ic_sysbar_back);
    }

    private int convertAbsoluteToRelativeGravity(int gravity) {
        if (mRelocatePieOnRotation) {
            if (isLandScape()) {
                // only mess around with Pie in landscape
                if (PIE_ALWAYS_AT_RIGHT && !isTablet()) {
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

    /**
     * Check whether the requested relative gravity is possible.
     *
     * @param gravity the Gravity value to check
     * @return whether the requested relative Gravity is possible
     * @see #isGravityPossible(int)
     */
    protected boolean isGravityPossible(int gravity) {
        if (mRelocatePieOnRotation && isLandScape() && PIE_ALWAYS_AT_RIGHT) {
            return gravity == Gravity.RIGHT;
        }

        return convertRelativeToAbsoluteGravity(gravity) != Gravity.NO_GRAVITY;
    }

    private PieItem makeItem(int image, String name, boolean lesser) {
        int mItemSize = (int) mContext.getResources().getDimension(R.dimen.pie_item_size);
        ImageView view = new ImageView(mContext);
        view.setImageResource(image);
        view.setMinimumWidth(mItemSize);
        view.setMinimumHeight(mItemSize);
        view.setScaleType(ScaleType.CENTER);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        view.setOnClickListener(mOnClickListener);
        return new PieItem(view, name, lesser, mItemSize);
    }

    protected void setCenter(int x, int y) {
        if (!mForcePieCentered) {
            switch (mPiePosition) {
                case Gravity.LEFT:
                case Gravity.RIGHT:
                    y = mEdgeGestureTouchPos.y;
                    break;
                case Gravity.BOTTOM:
                    x = mEdgeGestureTouchPos.x;
                    break;
            }
        }
        mPie.setCenter(x, y);
    }

    public void updateNotifications() {
        if (mPie != null) {
            mPie.updateNotifications(true);
        }
    }

    protected void onNavButtonPressed(String buttonName) {
        switch (buttonName) {
            case PieController.BACK_BUTTON:
                injectKeyDelayed(KeyEvent.KEYCODE_BACK);
                break;
            case PieController.HOME_BUTTON:
                injectKeyDelayed(KeyEvent.KEYCODE_HOME);
                break;
            case PieController.RECENT_BUTTON:
                mBar.toggleRecentApps();
                break;
        }
    }

    private Runnable onInjectKeyDelayed = new Runnable() {
        @Override
        public void run() {
            final long eventTime = SystemClock.uptimeMillis();
            if (mInjectKeycode != KeyEvent.KEYCODE_HOME) {
                InputManager.getInstance().injectInputEvent(
                        new KeyEvent(mDownTime, eventTime - 100,
                                KeyEvent.ACTION_DOWN, mInjectKeycode, 0),
                                        InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
            }
            InputManager.getInstance().injectInputEvent(
                    new KeyEvent(mDownTime, eventTime - 50, KeyEvent.ACTION_UP, mInjectKeycode, 0),
                    InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    };

    private void injectKeyDelayed(int keycode) {
        mInjectKeycode = keycode;
        mDownTime = SystemClock.uptimeMillis();
        mHandler.removeCallbacks(onInjectKeyDelayed);
        mHandler.postDelayed(onInjectKeyDelayed, 100);
        mBar.cancelPreloadRecentApps();
    }

    @Override
    public void onEdgeGestureActivation(int touchX, int touchY,
            EdgeGesturePosition position, int flags) {
        if (mPie != null
                && activateFromListener(touchX, touchY)) {
            // give the main thread some time to do the bookkeeping
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!gainTouchFocus(mPie.getWindowToken())) {
                        detachPie();
                    }
                    restoreListenerState();
                }
            });
        } else {
            restoreListenerState();
        }
    }

    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            onNavButtonPressed((String) v.getTag());
            mAudioManager.playSoundEffect(SoundEffectConstants.CLICK,
                    ActivityManager.getCurrentUser());
        }
    };
}
