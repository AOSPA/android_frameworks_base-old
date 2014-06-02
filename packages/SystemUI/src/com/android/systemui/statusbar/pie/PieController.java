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
import android.app.StatusBarManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ImageView.ScaleType;

import com.android.systemui.R;
import com.android.systemui.recent.RecentsActivity;
import com.android.systemui.recent.NavigationCallback;
import com.android.systemui.statusbar.BaseStatusBar;

import static com.android.systemui.statusbar.phone.QuickSettingsModel.IMMERSIVE_MODE_OFF;
import static com.android.systemui.statusbar.phone.QuickSettingsModel.IMMERSIVE_MODE_FULL;
import static com.android.systemui.statusbar.phone.QuickSettingsModel.IMMERSIVE_MODE_HIDE_ONLY_NAVBAR;
import static com.android.systemui.statusbar.phone.QuickSettingsModel.IMMERSIVE_MODE_HIDE_ONLY_STATUSBAR;

/**
 * Pie Controller
 * Sets and controls the pie menu and associated pie items
 * Singleton must be intilized.
 */
public class PieController implements OnClickListener, NavigationCallback {

    public static final String BACK_BUTTON = "##back##";
    public static final String HOME_BUTTON = "##home##";
    public static final String MENU_BUTTON = "##menu##";
    public static final String RECENT_BUTTON = "##recent##";
    public static final String CLEAR_ALL_BUTTON = "##clear##";

    private static PieController mInstance;
    private static PieHelper mPieHelper;

    private BaseStatusBar mBar;
    private Context mContext;
    private Handler mHandler;

    private View mTriggerView;
    private KeyguardManager mKeyguardManager;
    private WindowManager mWindowManager;

    private PieControlPanel mPanel;
    private PieControlPanel mPieControlPanel;

    private PieItem mBack;
    private PieItem mHome;
    private PieItem mMenu;
    private PieItem mRecent;

    protected PieMenu mPie;
    protected int mItemSize;

    private int mNavigationIconHints;
    private boolean mPieAttached;

    private PieController() {
    }


    /**
     * Creates a new instance of pie
     * @Param context the current Context
     * @Param wm the current Window Manager
     * @Param bar the current BaseStusBar
     */
    public void init(Context context, WindowManager wm, BaseStatusBar bar) {
        mContext = context;
        mHandler = new Handler();
        mWindowManager = wm;
        mBar = bar;
        mPieHelper = PieHelper.getInstance();
        mPieHelper.init(mContext, mBar);
        mItemSize = (int) context.getResources().getDimension(R.dimen.pie_item_size);
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        RecentsActivity.addNavigationCallback(this);
    }

    public static PieController getInstance() {
        if (mInstance == null) mInstance = new PieController();
        return mInstance;
    }

    public void detachPie() {
        if(mPieControlPanel != null) {
            mBar.updatePieControls(!mPieAttached);
        }
    }

    public void resetPie(boolean enabled, int gravity) {
        if(mPieAttached) {
            if (mPieControlPanel != null) mWindowManager.removeView(mPieControlPanel);
            if (mTriggerView != null) mWindowManager.removeView(mTriggerView);
            mPieAttached = false;
        }
        if(enabled) attachPie(gravity);
    }

    private boolean showPie() {
        final boolean pieEnabled = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PIE_STATE, 0) == 1;
        final int immersiveMode = Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.IMMERSIVE_MODE, 0);
        return pieEnabled && immersiveMode != IMMERSIVE_MODE_OFF
                && immersiveMode != IMMERSIVE_MODE_HIDE_ONLY_STATUSBAR;
    }

    public void attachPie(int gravity) {
        if(showPie()) {
            // want some slice?
            switch(gravity) {
                // this is just main gravity, the trigger is centered later
                case 1:
                    addPieInLocation(Gravity.LEFT);
                    break;
                case 2:
                    addPieInLocation(Gravity.RIGHT);
                    break;
                default:
                    addPieInLocation(Gravity.BOTTOM);
                    break;
            }
        }
    }

    private void addPieInLocation(int gravity) {
        if(mPieAttached) return;

        // pie panel
        mPieControlPanel = (PieControlPanel) View.inflate(mContext,
                R.layout.pie_control_panel, null);

        // pie trigger area
        mTriggerView = new View(mContext);
        mTriggerView.setOnTouchListener(new PieTouchListener());
        mWindowManager.addView(mTriggerView, getPieTriggerLayoutParams(mContext, gravity));

        // init panel
        mPieControlPanel.init(mHandler, mBar, gravity);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT);
        lp.setTitle("PieControlPanel");
        lp.windowAnimations = android.R.style.Animation;

        mWindowManager.addView(mPieControlPanel, lp);
        mPieAttached = true;
    }

    public WindowManager.LayoutParams getPieTriggerLayoutParams(Context context, int gravity) {
        final int defaultTriggerHeight =
                context.getResources().getDimensionPixelSize(R.dimen.pie_trigger_height);
        final int defaultTriggerWidth =
                context.getResources().getDimensionPixelSize(R.dimen.pie_trigger_width);
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
              (isVerticalGravity(gravity) ?
                    defaultTriggerWidth : defaultTriggerHeight),
              (!isVerticalGravity(gravity) ?
                    defaultTriggerWidth : defaultTriggerHeight),
              WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL,
                      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                              | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                              | WindowManager.LayoutParams.FLAG_SPLIT_TOUCH
                              | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
              PixelFormat.TRANSLUCENT);
        lp.softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_UNCHANGED
                | WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING;
        lp.gravity = centerTriggerView(gravity);
        return lp;
    }

    // pie gravity helpers
    private int centerTriggerView(int gravity) {
        int newGravity = gravity;
        switch(gravity) {
            case Gravity.BOTTOM:
                newGravity |= Gravity.CENTER_HORIZONTAL;
                break;
            case Gravity.LEFT:
            case Gravity.RIGHT:
                newGravity |= Gravity.CENTER_VERTICAL;
                break;
        }
        return newGravity;
    }

    private boolean isVerticalGravity(int gravity) {
        return gravity == Gravity.BOTTOM;
    }

    public PieControlPanel getControlPanel() {
        return mPieControlPanel;
    }

    public void populateMenu() {
        mBack = makeItem(R.drawable.ic_sysbar_back, 1, BACK_BUTTON, false);
        mHome = makeItem(R.drawable.ic_sysbar_home, 1, HOME_BUTTON, false);
        mRecent = makeItem(R.drawable.ic_sysbar_recent, 1, RECENT_BUTTON, false);
        mMenu = makeItem(R.drawable.ic_sysbar_menu, 1, MENU_BUTTON, true);

        mPie.addItem(mMenu);
        mPie.addItem(mRecent);
        mPie.addItem(mHome);
        mPie.addItem(mBack);
    }

    @Override
    public void setNavigationIconHints(int button, int hints, boolean force) {
        if (mRecent == null || mBack == null) return;
        mNavigationIconHints = hints;

        if (button == NavigationCallback.NAVBAR_RECENTS_HINT) {
            boolean alt = (0 != (hints &
                    StatusBarManager.NAVIGATION_HINT_RECENT_ALT)
                            && !mKeyguardManager.isKeyguardLocked()
                            && Settings.System.getInt(mContext.getContentResolver(), Settings.System.NAVBAR_RECENTS_CLEAR_ALL, 0) != 2);
            mRecent.setIcon(alt ? R.drawable.ic_sysbar_recent_clear
                    : R.drawable.ic_sysbar_recent);
            mRecent.setName(alt ? CLEAR_ALL_BUTTON : RECENT_BUTTON);
        } else if (button == NavigationCallback.NAVBAR_BACK_HINT) {
            boolean alt = (0 != (hints &
                    StatusBarManager.NAVIGATION_HINT_BACK_ALT));
            mBack.setIcon(alt ? R.drawable.ic_sysbar_back_ime
                    : R.drawable.ic_sysbar_back);
        }
    }

    @Override
    public int getNavigationIconHints() {
        return mNavigationIconHints;
    }

    @Override
    public void onClick(View v) {
        if (mPanel != null) {
            mPanel.onNavButtonPressed((String) v.getTag());
        }
    }

    protected PieItem makeItem(int image, int l, String name, boolean lesser) {
        ImageView view = new ImageView(mContext);
        view.setImageResource(image);
        view.setMinimumWidth(mItemSize);
        view.setMinimumHeight(mItemSize);
        view.setScaleType(ScaleType.CENTER);
        LayoutParams lp = new LayoutParams(mItemSize, mItemSize);
        view.setLayoutParams(lp);
        view.setOnClickListener(this);
        return new PieItem(view, mContext, l, name, lesser);
    }

    public void show(boolean show) {
        mPie.show(show);
    }

    public void setCenter(int x, int y) {
        mPie.setCenter(x, y);
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

    public interface OnNavButtonPressedListener {
        public void onNavButtonPressed(String buttonName);
    }

    // touch handling for trigger
    private class PieTouchListener implements View.OnTouchListener {

        private float initialX = 0;
        private float initialY = 0;
        private boolean down = false;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            final int action = event.getAction();
            final int orient = mPieControlPanel.getOrientation();
            if (!mPieControlPanel.isShowing()) {
                switch(action) {
                    case MotionEvent.ACTION_DOWN:
                        down = true;
                        initialX = event.getX();
                        initialY = event.getY();
                        break;
                    case MotionEvent.ACTION_MOVE:
                        if (down != true) {
                            detachPie();
                            break;
                        }
                        float deltaX = Math.abs(event.getX() - initialX);
                        float deltaY = Math.abs(event.getY() - initialY);
                        float distance = isVerticalGravity(orient) ?
                        deltaY : deltaX;
                        // swipe up
                        if (distance > 25) {
                            mPieControlPanel.show();
                            event.setAction(MotionEvent.ACTION_DOWN);
                            mPie.onTouchEvent(event);
                            down = false;
                        }
                }
            } else {
                mPie.onTouchEvent(event);
            }
            return false;
        }
    }
}
