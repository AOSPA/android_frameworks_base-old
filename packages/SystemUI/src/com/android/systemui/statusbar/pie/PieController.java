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
import android.app.StatusBarManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.gesture.EdgeGestureManager;
import android.view.*;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
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
public class PieController extends EdgeGestureManager.EdgeGestureActivationListener implements OnTouchListener, OnClickListener {

    static final String BACK_BUTTON = "##back##";
    static final String HOME_BUTTON = "##home##";
    static final String MENU_BUTTON = "##menu##";
    static final String RECENT_BUTTON = "##recent##";

    private static PieController mInstance;
    private static AudioManager mAudioManager;
    private BaseStatusBar mBar;
    private Context mContext;
    private Handler mHandler;
    private WindowManager mWindowManager;
    private EdgeGestureManager mPieManager;
    private Point mEdgeGestureTouchPos = new Point(0, 0);
    private int mPiePosition;
    private PieControlPanel mPanel;
    private PieControlPanel mPieControlPanel;
    private PieItem mBack;
    private PieItem mHome;
    private PieItem mMenu;
    private PieItem mRecent;
    private PieMenu mPie;
    private int mItemSize;
    private boolean mPieAttached;
    private boolean mForcePieCentered;

    private PieController() {
        super(Looper.getMainLooper());
    }

    private static AudioManager getAudioManager(Context context) {
        if (mAudioManager == null)
            mAudioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        return mAudioManager;
    }

    public static PieController getInstance() {
        if (mInstance == null) mInstance = new PieController();
        return mInstance;
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
        PieHelper pieHelper = PieHelper.getInstance();
        pieHelper.init(mContext, mBar);
        mItemSize = (int) context.getResources().getDimension(R.dimen.pie_item_size);

        mPieManager = EdgeGestureManager.getInstance();
        mPieManager.setEdgeGestureActivationListener(this);
        mForcePieCentered = mContext.getResources().getBoolean(R.bool.config_forcePieCentered);

        mAudioManager = getAudioManager(context);
    }

    public void detachPie() {
        if (mPieControlPanel != null) {
            mBar.updatePieControls(!mPieAttached);
        }
    }

    public void resetPie(boolean enabled, int gravity) {
        if (mPieAttached) {
            if (mPieControlPanel != null) mWindowManager.removeView(mPieControlPanel);
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

    private void addPieInLocation(int gravity) {
        if (mPieAttached) return;

        // pie panel
        mPieControlPanel = (PieControlPanel) View.inflate(mContext,
                R.layout.pie_control_panel, null);

        // init panel
        mPieControlPanel.init(mHandler, mBar, gravity);
        mPieControlPanel.setOnTouchListener(this);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);

        // Turn on hardware acceleration for high end gfx devices.
        if (ActivityManager.isHighEndGfx()) {
            lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
            lp.privateFlags |=
                    WindowManager.LayoutParams.PRIVATE_FLAG_FORCE_HARDWARE_ACCELERATED;
        }

        lp.setTitle("PieControlPanel");
        lp.windowAnimations = android.R.style.Animation;

        // pie edge gesture
        mPiePosition = mPieControlPanel.getOrientation();
        setupEdgeGesture(mPiePosition);

        mWindowManager.addView(mPieControlPanel, lp);
        mPieAttached = true;
    }

    private boolean activateFromListener(int touchX, int touchY) {
        if (!mPieControlPanel.isShowing()) {
            mEdgeGestureTouchPos.x = touchX;
            mEdgeGestureTouchPos.y = touchY;
            mPieControlPanel.show();
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

    public PieControlPanel getControlPanel() {
        return mPieControlPanel;
    }

    public void setControlPanel(PieControlPanel panel) {
        mPanel = panel;
        mPie = new PieMenu(mContext, mPanel);
        LayoutParams lp = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);
        mPie.setLayoutParams(lp);
        populateMenu();
        mPanel.addView(mPie);
    }

    private void populateMenu() {
        mBack = makeItem(R.drawable.ic_sysbar_back, 1, BACK_BUTTON, false);
        mHome = makeItem(R.drawable.ic_sysbar_home, 1, HOME_BUTTON, false);
        mRecent = makeItem(R.drawable.ic_sysbar_recent, 1, RECENT_BUTTON, false);
        mMenu = makeItem(R.drawable.ic_sysbar_menu, 1, MENU_BUTTON, true);

        mPie.addItem(mMenu);
        mPie.addItem(mRecent);
        mPie.addItem(mHome);
        mPie.addItem(mBack);
    }

    public void setNavigationIconHints(int hints) {
        if (mBack == null) return;

        boolean alt = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        mBack.setIcon(alt ? R.drawable.ic_sysbar_back_ime : R.drawable.ic_sysbar_back);
    }

    @Override
    public void onClick(View v) {
        if (mPanel != null) {
            mPanel.onNavButtonPressed((String) v.getTag());
            playSoundEffect(SoundEffectConstants.CLICK);
        }
    }

    private void playSoundEffect(int soundConstant) {
        mAudioManager.playSoundEffect(soundConstant, ActivityManager.getCurrentUser());
    }

    private PieItem makeItem(int image, int l, String name, boolean lesser) {
        ImageView view = new ImageView(mContext);
        view.setImageResource(image);
        view.setMinimumWidth(mItemSize);
        view.setMinimumHeight(mItemSize);
        view.setScaleType(ScaleType.FIT_XY);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        view.setOnClickListener(this);
        return new PieItem(view, l, name, lesser, mItemSize);
    }

    public void show(boolean show) {
        mPie.show(show);
    }

    public void setCenter(int x, int y) {
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

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        mPie.onTouchEvent(event);
        return true;
    }

    @Override
    public void onEdgeGestureActivation(int touchX, int touchY, EdgeGesturePosition position,
                                        int flags) {
        if (mPieControlPanel != null
                && activateFromListener(touchX, touchY)) {
            // give the main thread some time to do the bookkeeping
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (!gainTouchFocus(mPieControlPanel.getWindowToken())) {
                        detachPie();
                    }
                    restoreListenerState();
                }
            });
        } else {
            restoreListenerState();
        }
    }

    interface OnNavButtonPressedListener {
        void onNavButtonPressed(String buttonName);
    }
}
