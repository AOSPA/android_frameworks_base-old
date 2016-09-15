/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
<<<<<<< HEAD
=======
import android.graphics.Rect;
import android.graphics.Region;
import android.os.PowerManager;
import android.provider.Settings;
>>>>>>> 709513c2460f (SystemUI: add quick settings pull down with one finger)
import android.util.AttributeSet;

import com.android.systemui.R;
<<<<<<< HEAD
=======
import com.android.systemui.classifier.FalsingManagerFactory;
import com.android.systemui.fragments.FragmentHostManager;
import com.android.systemui.fragments.FragmentHostManager.FragmentListener;
import com.android.systemui.plugins.FalsingManager;
import com.android.systemui.plugins.qs.QS;
import com.android.systemui.plugins.statusbar.StatusBarStateController;
import com.android.systemui.plugins.statusbar.StatusBarStateController.StateListener;
import com.android.systemui.qs.QSFragment;
import com.android.systemui.settings.SettingConfirmationHelper;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.FlingAnimationUtils;
import com.android.systemui.statusbar.GestureRecorder;
import com.android.systemui.statusbar.KeyguardAffordanceView;
import com.android.systemui.statusbar.KeyguardIndicationController;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationShelf;
import com.android.systemui.statusbar.PulseExpansionHandler;
import com.android.systemui.statusbar.RemoteInputController;
import com.android.systemui.statusbar.SettingConfirmationSnackbarViewCreator;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.notification.ActivityLaunchAnimator;
import com.android.systemui.statusbar.notification.AnimatableProperty;
import com.android.systemui.statusbar.notification.DynamicPrivacyController;
import com.android.systemui.statusbar.notification.NotificationEntryManager;
import com.android.systemui.statusbar.notification.NotificationWakeUpCoordinator;
import com.android.systemui.statusbar.notification.PropertyAnimator;
import com.android.systemui.statusbar.notification.collection.NotificationEntry;
import com.android.systemui.statusbar.notification.row.ActivatableNotificationView;
import com.android.systemui.statusbar.notification.row.ExpandableNotificationRow;
import com.android.systemui.statusbar.notification.row.ExpandableView;
import com.android.systemui.statusbar.notification.stack.AnimationProperties;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayout;
import com.android.systemui.statusbar.notification.stack.StackStateAnimator;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.statusbar.policy.KeyguardUserSwitcher;
import com.android.systemui.statusbar.policy.OnHeadsUpChangedListener;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.util.InjectionInflationController;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
>>>>>>> 709513c2460f (SystemUI: add quick settings pull down with one finger)

public class NotificationPanelView extends PanelView {

    private static final boolean DEBUG = false;

    /**
     * Fling expanding QS.
     */
    public static final int FLING_EXPAND = 0;

    static final String COUNTER_PANEL_OPEN = "panel_open";
    static final String COUNTER_PANEL_OPEN_QS = "panel_open_qs";

    private int mCurrentPanelAlpha;
    private final Paint mAlphaPaint = new Paint();
    private boolean mDozing;
    private RtlChangeListener mRtlChangeListener;

    public NotificationPanelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setWillNotDraw(!DEBUG);
        mAlphaPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.MULTIPLY));
<<<<<<< HEAD

        setBackgroundColor(Color.TRANSPARENT);
=======
        setPanelAlpha(255, false /* animate */);
        mCommandQueue = getComponent(context, CommandQueue.class);
        mDisplayId = context.getDisplayId();
        mPulseExpansionHandler = pulseExpansionHandler;
        mThemeResId = context.getThemeResId();
        dynamicPrivacyController.addListener(this);

        mBottomAreaShadeAlphaAnimator = ValueAnimator.ofFloat(1f, 0);
        mBottomAreaShadeAlphaAnimator.addUpdateListener(animation -> {
            mBottomAreaShadeAlpha = (float) animation.getAnimatedValue();
            updateKeyguardBottomAreaAlpha();
        });
        mBottomAreaShadeAlphaAnimator.setDuration(160);
        mBottomAreaShadeAlphaAnimator.setInterpolator(Interpolators.ALPHA_OUT);
    }

    /**
     * Returns if there's a custom clock being presented.
     */
    public boolean hasCustomClock() {
        return mKeyguardStatusView.hasCustomClock();
    }

    private void setStatusBar(StatusBar bar) {
        mStatusBar = bar;
        mKeyguardBottomArea.setStatusBar(mStatusBar);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mKeyguardStatusBar = findViewById(R.id.keyguard_header);
        mKeyguardStatusView = findViewById(R.id.keyguard_status_view);

        KeyguardClockSwitch keyguardClockSwitch = findViewById(R.id.keyguard_clock_container);
        mBigClockContainer = findViewById(R.id.big_clock_container);
        keyguardClockSwitch.setBigClockContainer(mBigClockContainer);

        mNotificationContainerParent = findViewById(R.id.notification_container_parent);
        mNotificationStackScroller = findViewById(R.id.notification_stack_scroller);
        mNotificationStackScroller.setOnHeightChangedListener(this);
        mNotificationStackScroller.setOverscrollTopChangedListener(this);
        mNotificationStackScroller.setOnEmptySpaceClickListener(this);
        addTrackingHeadsUpListener(mNotificationStackScroller::setTrackingHeadsUp);
        mKeyguardBottomArea = findViewById(R.id.keyguard_bottom_area);
        mQsNavbarScrim = findViewById(R.id.qs_navbar_scrim);
        mLastOrientation = getResources().getConfiguration().orientation;

        initBottomArea();

        mWakeUpCoordinator.setStackScroller(mNotificationStackScroller);
        mQsFrame = findViewById(R.id.qs_frame);
        mPulseExpansionHandler.setUp(mNotificationStackScroller, this, mShadeController);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        FragmentHostManager.get(this).addTagListener(QS.TAG, mFragmentListener);
        Dependency.get(StatusBarStateController.class).addCallback(this);
        Dependency.get(ZenModeController.class).addCallback(this);
        Dependency.get(ConfigurationController.class).addCallback(this);
        // Theme might have changed between inflating this view and attaching it to the window, so
        // force a call to onThemeChanged
        onThemeChanged();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        FragmentHostManager.get(this).removeTagListener(QS.TAG, mFragmentListener);
        Dependency.get(StatusBarStateController.class).removeCallback(this);
        Dependency.get(ZenModeController.class).removeCallback(this);
        Dependency.get(ConfigurationController.class).removeCallback(this);
    }

    @Override
    protected void loadDimens() {
        super.loadDimens();
        mFlingAnimationUtils = new FlingAnimationUtils(getContext(), 0.4f);
        mStatusBarMinHeight = getResources().getDimensionPixelSize(
                com.android.internal.R.dimen.status_bar_height);
        mQsPeekHeight = getResources().getDimensionPixelSize(R.dimen.qs_peek_height);
        mNotificationsHeaderCollideDistance =
                getResources().getDimensionPixelSize(R.dimen.header_notifications_collide_distance);
        mUnlockMoveDistance = getResources().getDimensionPixelOffset(R.dimen.unlock_move_distance);
        mClockPositionAlgorithm.loadDimens(getResources());
        mQsFalsingThreshold = getResources().getDimensionPixelSize(
                R.dimen.qs_falsing_threshold);
        mPositionMinSideMargin = getResources().getDimensionPixelSize(
                R.dimen.notification_panel_min_side_margin);
        mMaxFadeoutHeight = getResources().getDimensionPixelSize(
                R.dimen.max_notification_fadeout_height);
        mIndicationBottomPadding = getResources().getDimensionPixelSize(
                R.dimen.keyguard_indication_bottom_padding);
        mQsNotificationTopPadding = getResources().getDimensionPixelSize(
                R.dimen.qs_notification_padding);
    }

    /**
     * @see #launchCamera(boolean, int)
     * @see #setLaunchingAffordance(boolean)
     */
    public void setLaunchAffordanceListener(Consumer<Boolean> listener) {
        mAffordanceLaunchListener = listener;
    }

    public void updateResources() {
        Resources res = getResources();
        int qsWidth = res.getDimensionPixelSize(R.dimen.qs_panel_width);
        int panelGravity = getResources().getInteger(R.integer.notification_panel_layout_gravity);
        FrameLayout.LayoutParams lp =
                (FrameLayout.LayoutParams) mQsFrame.getLayoutParams();
        if (lp.width != qsWidth || lp.gravity != panelGravity) {
            lp.width = qsWidth;
            lp.gravity = panelGravity;
            mQsFrame.setLayoutParams(lp);
        }

        int panelWidth = res.getDimensionPixelSize(R.dimen.notification_panel_width);
        lp = (FrameLayout.LayoutParams) mNotificationStackScroller.getLayoutParams();
        if (lp.width != panelWidth || lp.gravity != panelGravity) {
            lp.width = panelWidth;
            lp.gravity = panelGravity;
            mNotificationStackScroller.setLayoutParams(lp);
        }
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        updateShowEmptyShadeView();
    }

    @Override
    public void onThemeChanged() {
        final int themeResId = getContext().getThemeResId();
        if (mThemeResId == themeResId) {
            return;
        }
        mThemeResId = themeResId;

        reInflateViews();
    }

    @Override
    public void onOverlayChanged() {
        reInflateViews();
    }

    private void reInflateViews() {
        updateShowEmptyShadeView();

        // Re-inflate the status view group.
        int index = indexOfChild(mKeyguardStatusView);
        removeView(mKeyguardStatusView);
        mKeyguardStatusView = (KeyguardStatusView) mInjectionInflationController
                .injectable(LayoutInflater.from(mContext)).inflate(
                        R.layout.keyguard_status_view,
                        this,
                        false);
        addView(mKeyguardStatusView, index);

        // Re-associate the clock container with the keyguard clock switch.
        mBigClockContainer.removeAllViews();
        KeyguardClockSwitch keyguardClockSwitch = findViewById(R.id.keyguard_clock_container);
        keyguardClockSwitch.setBigClockContainer(mBigClockContainer);

        // Update keyguard bottom area
        index = indexOfChild(mKeyguardBottomArea);
        removeView(mKeyguardBottomArea);
        KeyguardBottomAreaView oldBottomArea = mKeyguardBottomArea;
        mKeyguardBottomArea = (KeyguardBottomAreaView) mInjectionInflationController
                .injectable(LayoutInflater.from(mContext)).inflate(
                        R.layout.keyguard_bottom_area,
                        this,
                        false);
        mKeyguardBottomArea.initFrom(oldBottomArea);
        addView(mKeyguardBottomArea, index);
        initBottomArea();
        mKeyguardIndicationController.setIndicationArea(mKeyguardBottomArea);
        onDozeAmountChanged(mStatusBarStateController.getDozeAmount(),
                mStatusBarStateController.getInterpolatedDozeAmount());

        if (mKeyguardStatusBar != null) {
            mKeyguardStatusBar.onThemeChanged();
        }

        setKeyguardStatusViewVisibility(mBarState, false, false);
        setKeyguardBottomAreaVisibility(mBarState, false);
    }

    private void initBottomArea() {
        mAffordanceHelper = new KeyguardAffordanceHelper(this, getContext());
        mKeyguardBottomArea.setAffordanceHelper(mAffordanceHelper);
        mKeyguardBottomArea.setStatusBar(mStatusBar);
        mKeyguardBottomArea.setUserSetupComplete(mUserSetupComplete);
    }

    public void setKeyguardIndicationController(KeyguardIndicationController indicationController) {
        mKeyguardIndicationController = indicationController;
        mKeyguardIndicationController.setIndicationArea(mKeyguardBottomArea);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        setIsFullWidth(mNotificationStackScroller.getWidth() == getWidth());

        // Update Clock Pivot
        mKeyguardStatusView.setPivotX(getWidth() / 2);
        mKeyguardStatusView.setPivotY((FONT_HEIGHT - CAP_HEIGHT) / 2048f *
                mKeyguardStatusView.getClockTextSize());

        // Calculate quick setting heights.
        int oldMaxHeight = mQsMaxExpansionHeight;
        if (mQs != null) {
            mQsMinExpansionHeight = mKeyguardShowing ? 0 : mQs.getQsMinExpansionHeight();
            mQsMaxExpansionHeight = mQs.getDesiredHeight();
            mNotificationStackScroller.setMaxTopPadding(
                    mQsMaxExpansionHeight + mQsNotificationTopPadding);
        }
        positionClockAndNotifications();
        if (mQsExpanded && mQsFullyExpanded) {
            mQsExpansionHeight = mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false /* animate */);
            requestPanelHeightUpdate();

            // Size has changed, start an animation.
            if (mQsMaxExpansionHeight != oldMaxHeight) {
                startQsSizeChangeAnimation(oldMaxHeight, mQsMaxExpansionHeight);
            }
        } else if (!mQsExpanded) {
            setQsExpansion(mQsMinExpansionHeight + mLastOverscroll);
        }
        updateExpandedHeight(getExpandedHeight());
        updateHeader();

        // If we are running a size change animation, the animation takes care of the height of
        // the container. However, if we are not animating, we always need to make the QS container
        // the desired height so when closing the QS detail, it stays smaller after the size change
        // animation is finished but the detail view is still being animated away (this animation
        // takes longer than the size change animation).
        if (mQsSizeChangeAnimator == null && mQs != null) {
            mQs.setHeightOverride(mQs.getDesiredHeight());
        }
        updateMaxHeadsUpTranslation();
        updateGestureExclusionRect();
    }

    private void updateGestureExclusionRect() {
        Rect exclusionRect = calculateGestureExclusionRect();
        setSystemGestureExclusionRects(exclusionRect.isEmpty()
                ? Collections.EMPTY_LIST
                : Collections.singletonList(exclusionRect));
    }

    private Rect calculateGestureExclusionRect() {
        Rect exclusionRect = null;
        Region touchableRegion = mHeadsUpManager.calculateTouchableRegion();
        if (isFullyCollapsed() && touchableRegion != null) {
            // Note: The heads up manager also calculates the non-pinned touchable region
            exclusionRect = touchableRegion.getBounds();
        }
        return exclusionRect != null
                ? exclusionRect
                : mEmptyRect;
    }

    private void setIsFullWidth(boolean isFullWidth) {
        mIsFullWidth = isFullWidth;
        mNotificationStackScroller.setIsFullWidth(isFullWidth);
    }

    private void startQsSizeChangeAnimation(int oldHeight, final int newHeight) {
        if (mQsSizeChangeAnimator != null) {
            oldHeight = (int) mQsSizeChangeAnimator.getAnimatedValue();
            mQsSizeChangeAnimator.cancel();
        }
        mQsSizeChangeAnimator = ValueAnimator.ofInt(oldHeight, newHeight);
        mQsSizeChangeAnimator.setDuration(300);
        mQsSizeChangeAnimator.setInterpolator(Interpolators.FAST_OUT_SLOW_IN);
        mQsSizeChangeAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                requestScrollerTopPaddingUpdate(false /* animate */);
                requestPanelHeightUpdate();
                int height = (int) mQsSizeChangeAnimator.getAnimatedValue();
                mQs.setHeightOverride(height);
            }
        });
        mQsSizeChangeAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mQsSizeChangeAnimator = null;
            }
        });
        mQsSizeChangeAnimator.start();
    }

    /**
     * Positions the clock and notifications dynamically depending on how many notifications are
     * showing.
     */
    private void positionClockAndNotifications() {
        boolean animate = mNotificationStackScroller.isAddOrRemoveAnimationPending();
        boolean animateClock = animate || mAnimateNextPositionUpdate;
        int stackScrollerPadding;
        if (mBarState != StatusBarState.KEYGUARD) {
            stackScrollerPadding = (mQs != null ? mQs.getHeader().getHeight() : 0) + mQsPeekHeight
                    + mQsNotificationTopPadding;
        } else {
            int totalHeight = getHeight();
            int bottomPadding = Math.max(mIndicationBottomPadding, mAmbientIndicationBottomPadding);
            int clockPreferredY = mKeyguardStatusView.getClockPreferredY(totalHeight);
            mClockPositionAlgorithm.setup(
                    mStatusBarMinHeight,
                    totalHeight - bottomPadding,
                    mNotificationStackScroller.getIntrinsicContentHeight(),
                    getExpandedFraction(),
                    totalHeight,
                    mKeyguardStatusView.getHeight(),
                    clockPreferredY,
                    hasCustomClock(),
                    mNotificationStackScroller.getVisibleNotificationCount() != 0,
                    mInterpolatedDarkAmount,
                    mEmptyDragAmount);
            mClockPositionAlgorithm.run(mClockPositionResult);
            PropertyAnimator.setProperty(mKeyguardStatusView, AnimatableProperty.X,
                    mClockPositionResult.clockX, CLOCK_ANIMATION_PROPERTIES, animateClock);
            PropertyAnimator.setProperty(mKeyguardStatusView, AnimatableProperty.Y,
                    mClockPositionResult.clockY, CLOCK_ANIMATION_PROPERTIES, animateClock);
            updateNotificationTranslucency();
            updateClock();
            stackScrollerPadding = mClockPositionResult.stackScrollerPadding;
        }
        mNotificationStackScroller.setIntrinsicPadding(stackScrollerPadding);
        mNotificationStackScroller.setAntiBurnInOffsetX(mClockPositionResult.clockX);
        mKeyguardBottomArea.setAntiBurnInOffsetX(mClockPositionResult.clockX);

        mStackScrollerMeasuringPass++;
        requestScrollerTopPaddingUpdate(animate);
        mStackScrollerMeasuringPass = 0;
        mAnimateNextPositionUpdate = false;
    }

    /**
     * @param maximum the maximum to return at most
     * @return the maximum keyguard notifications that can fit on the screen
     */
    public int computeMaxKeyguardNotifications(int maximum) {
        float minPadding = mClockPositionAlgorithm.getMinStackScrollerPadding();
        int notificationPadding = Math.max(1, getResources().getDimensionPixelSize(
                R.dimen.notification_divider_height));
        NotificationShelf shelf = mNotificationStackScroller.getNotificationShelf();
        float shelfSize = shelf.getVisibility() == GONE ? 0
                : shelf.getIntrinsicHeight() + notificationPadding;
        float availableSpace = mNotificationStackScroller.getHeight() - minPadding - shelfSize
                - Math.max(mIndicationBottomPadding, mAmbientIndicationBottomPadding)
                - mKeyguardStatusView.getLogoutButtonHeight();
        int count = 0;
        for (int i = 0; i < mNotificationStackScroller.getChildCount(); i++) {
            ExpandableView child = (ExpandableView) mNotificationStackScroller.getChildAt(i);
            if (!(child instanceof ExpandableNotificationRow)) {
                continue;
            }
            ExpandableNotificationRow row = (ExpandableNotificationRow) child;
            boolean suppressedSummary = mGroupManager != null
                    && mGroupManager.isSummaryOfSuppressedGroup(row.getStatusBarNotification());
            if (suppressedSummary) {
                continue;
            }
            if (!mLockscreenUserManager.shouldShowOnKeyguard(row.getEntry())) {
                continue;
            }
            if (row.isRemoved()) {
                continue;
            }
            availableSpace -= child.getMinHeight(true /* ignoreTemporaryStates */)
                    + notificationPadding;
            if (availableSpace >= 0 && count < maximum) {
                count++;
            } else if (availableSpace > -shelfSize) {
                // if we are exactly the last view, then we can show us still!
                for (int j = i + 1; j < mNotificationStackScroller.getChildCount(); j++) {
                    if (mNotificationStackScroller.getChildAt(j)
                            instanceof ExpandableNotificationRow) {
                        return count;
                    }
                }
                count++;
                return count;
            } else {
                return count;
            }
        }
        return count;
    }

    private void updateClock() {
        if (!mKeyguardStatusViewAnimating) {
            mKeyguardStatusView.setAlpha(mClockPositionResult.clockAlpha);
        }
    }

    public void animateToFullShade(long delay) {
        mNotificationStackScroller.goToFullShade(delay);
        requestLayout();
        mAnimateNextPositionUpdate = true;
    }

    public void setQsExpansionEnabled(boolean qsExpansionEnabled) {
        mQsExpansionEnabled = qsExpansionEnabled;
        if (mQs == null) return;
        mQs.setHeaderClickable(qsExpansionEnabled);
    }

    @Override
    public void resetViews(boolean animate) {
        mIsLaunchTransitionFinished = false;
        mBlockTouches = false;
        if (!mLaunchingAffordance) {
            mAffordanceHelper.reset(false);
            mLastCameraLaunchSource = KeyguardBottomAreaView.CAMERA_LAUNCH_SOURCE_AFFORDANCE;
        }
        mStatusBar.getGutsManager().closeAndSaveGuts(true /* leavebehind */, true /* force */,
                true /* controls */, -1 /* x */, -1 /* y */, true /* resetMenu */);
        if (animate) {
            animateCloseQs(true /* animateAway */);
        } else {
            closeQs();
        }
        mNotificationStackScroller.setOverScrollAmount(0f, true /* onTop */, animate,
                !animate /* cancelAnimators */);
        mNotificationStackScroller.resetScrollPosition();
    }

    @Override
    public void collapse(boolean delayed, float speedUpFactor) {
        if (!canPanelBeCollapsed()) {
            return;
        }

        if (mQsExpanded) {
            mQsExpandImmediate = true;
            mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        super.collapse(delayed, speedUpFactor);
    }

    public void closeQs() {
        cancelQsAnimation();
        setQsExpansion(mQsMinExpansionHeight);
    }

    /**
     * Animate QS closing by flinging it.
     * If QS is expanded, it will collapse into QQS and stop.
     *
     * @param animateAway Do not stop when QS becomes QQS. Fling until QS isn't visible anymore.
     */
    public void animateCloseQs(boolean animateAway) {
        if (mQsExpansionAnimator != null) {
            if (!mQsAnimatorExpand) {
                return;
            }
            float height = mQsExpansionHeight;
            mQsExpansionAnimator.cancel();
            setQsExpansion(height);
        }
        flingSettings(0 /* vel */, animateAway ? FLING_HIDE : FLING_COLLAPSE);
    }

    public void expandWithQs() {
        if (mQsExpansionEnabled) {
            mQsExpandImmediate = true;
            mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        if (isFullyCollapsed()) {
            expand(true /* animate */);
        } else {
            flingSettings(0 /* velocity */, FLING_EXPAND);
        }
    }

    public void expandWithoutQs() {
        if (isQsExpanded()) {
            flingSettings(0 /* velocity */, FLING_COLLAPSE);
        } else {
            expand(true /* animate */);
        }
    }

    @Override
    public void fling(float vel, boolean expand) {
        GestureRecorder gr = ((PhoneStatusBarView) mBar).mBar.getGestureRecorder();
        if (gr != null) {
            gr.tag("fling " + ((vel > 0) ? "open" : "closed"), "notifications,v=" + vel);
        }
        super.fling(vel, expand);
    }

    @Override
    protected void flingToHeight(float vel, boolean expand, float target,
            float collapseSpeedUpFactor, boolean expandBecauseOfFalsing) {
        mHeadsUpTouchHelper.notifyFling(!expand);
        setClosingWithAlphaFadeout(!expand && getFadeoutAlpha() == 1.0f);
        super.flingToHeight(vel, expand, target, collapseSpeedUpFactor, expandBecauseOfFalsing);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (mBlockTouches || mQsFullyExpanded && mQs.onInterceptTouchEvent(event)) {
            return false;
        }
        initDownStates(event);
        // Do not let touches go to shade or QS if the bouncer is visible,
        // but still let user swipe down to expand the panel, dismissing the bouncer.
        if (mStatusBar.isBouncerShowing()) {
            return true;
        }
        if (mBar.panelEnabled() && mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN, 1);
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN_PEEK, 1);
            return true;
        }
        if (mPulseExpansionHandler.onInterceptTouchEvent(event)) {
            return true;
        }

        if (!isFullyCollapsed() && onQsIntercept(event)) {
            return true;
        }
        return super.onInterceptTouchEvent(event);
    }

    private boolean onQsIntercept(MotionEvent event) {
        int pointerIndex = event.findPointerIndex(mTrackingPointer);
        if (pointerIndex < 0) {
            pointerIndex = 0;
            mTrackingPointer = event.getPointerId(pointerIndex);
        }
        final float x = event.getX(pointerIndex);
        final float y = event.getY(pointerIndex);

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                mIntercepting = true;
                mInitialTouchY = y;
                mInitialTouchX = x;
                initVelocityTracker();
                trackMovement(event);
                if (shouldQuickSettingsIntercept(mInitialTouchX, mInitialTouchY, 0, true, false)) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                if (mQsExpansionAnimator != null) {
                    onQsExpansionStarted();
                    mInitialHeightOnTouch = mQsExpansionHeight;
                    mQsTracking = true;
                    mIntercepting = false;
                    mNotificationStackScroller.cancelLongPress();
                }
                break;
            case MotionEvent.ACTION_POINTER_UP:
                final int upPointer = event.getPointerId(event.getActionIndex());
                if (mTrackingPointer == upPointer) {
                    // gesture is ongoing, find a new pointer to track
                    final int newIndex = event.getPointerId(0) != upPointer ? 0 : 1;
                    mTrackingPointer = event.getPointerId(newIndex);
                    mInitialTouchX = event.getX(newIndex);
                    mInitialTouchY = event.getY(newIndex);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                final float h = y - mInitialTouchY;
                trackMovement(event);
                if (mQsTracking) {

                    // Already tracking because onOverscrolled was called. We need to update here
                    // so we don't stop for a frame until the next touch event gets handled in
                    // onTouchEvent.
                    setQsExpansion(h + mInitialHeightOnTouch);
                    trackMovement(event);
                    mIntercepting = false;
                    return true;
                }
                if (Math.abs(h) > mTouchSlop && Math.abs(h) > Math.abs(x - mInitialTouchX)
                        && shouldQuickSettingsIntercept(mInitialTouchX, mInitialTouchY, h,
                                true, false)) {
                    mQsTracking = true;
                    onQsExpansionStarted();
                    notifyExpandingFinished();
                    mInitialHeightOnTouch = mQsExpansionHeight;
                    mInitialTouchY = y;
                    mInitialTouchX = x;
                    mIntercepting = false;
                    mNotificationStackScroller.cancelLongPress();
                    return true;
                }
                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP:
                trackMovement(event);
                if (mQsTracking) {
                    flingQsWithCurrentVelocity(y,
                            event.getActionMasked() == MotionEvent.ACTION_CANCEL);
                    mQsTracking = false;
                }
                mIntercepting = false;
                break;
        }
        return false;
>>>>>>> 709513c2460f (SystemUI: add quick settings pull down with one finger)
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (mRtlChangeListener != null) {
            mRtlChangeListener.onRtlPropertielsChanged(layoutDirection);
        }
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    @Override
<<<<<<< HEAD
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mCurrentPanelAlpha != 255) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mAlphaPaint);
=======
    public boolean onTouchEvent(MotionEvent event) {
        if (mBlockTouches || (mQs != null && mQs.isCustomizing())) {
            return false;
        }

        // Do not allow panel expansion if bouncer is scrimmed, otherwise user would be able to
        // pull down QS or expand the shade.
        if (mStatusBar.isBouncerShowingScrimmed()) {
            return false;
        }

        initDownStates(event);
        // Make sure the next touch won't the blocked after the current ends.
        if (event.getAction() == MotionEvent.ACTION_UP
                || event.getAction() == MotionEvent.ACTION_CANCEL) {
            mBlockingExpansionForCurrentTouch = false;
        }
        if (!mIsExpanding && mPulseExpansionHandler.onTouchEvent(event)) {
            // We're expanding all the other ones shouldn't get this anymore
            return true;
        }
        if (mListenForHeadsUp && !mHeadsUpTouchHelper.isTrackingHeadsUp()
                && mHeadsUpTouchHelper.onInterceptTouchEvent(event)) {
            mIsExpansionFromHeadsUp = true;
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN_PEEK, 1);
        }
        boolean handled = false;
        if ((!mIsExpanding || mHintAnimationRunning)
                && !mQsExpanded
                && mBarState != StatusBarState.SHADE
                && !mDozing) {
            handled |= mAffordanceHelper.onTouchEvent(event);
        }
        if (mOnlyAffordanceInThisMotion) {
            return true;
        }
        handled |= mHeadsUpTouchHelper.onTouchEvent(event);

        if (!mHeadsUpTouchHelper.isTrackingHeadsUp() && handleQsTouch(event)) {
            return true;
        }
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN && isFullyCollapsed()) {
            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN, 1);
            updateVerticalPanelPosition(event.getX());
            handled = true;
        }
        handled |= super.onTouchEvent(event);
        return !mDozing || mPulsing || handled;
    }

    private boolean handleQsTouch(MotionEvent event) {
        final int action = event.getActionMasked();
        if (action == MotionEvent.ACTION_DOWN && getExpandedFraction() == 1f
                && mBarState != StatusBarState.KEYGUARD && !mQsExpanded
                && mQsExpansionEnabled) {

            // Down in the empty area while fully expanded - go to QS.
            mQsTracking = true;
            mConflictingQsExpansionGesture = true;
            onQsExpansionStarted();
            mInitialHeightOnTouch = mQsExpansionHeight;
            mInitialTouchY = event.getX();
            mInitialTouchX = event.getY();
        }
        if (!isFullyCollapsed()) {
            handleQsDown(event);
        }
        if (!mQsExpandImmediate && mQsTracking) {
            onQsTouch(event);
            if (!mConflictingQsExpansionGesture) {
                return true;
            }
        }
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mConflictingQsExpansionGesture = false;
        }
        if (action == MotionEvent.ACTION_DOWN && isFullyCollapsed()
                && mQsExpansionEnabled) {
            mTwoFingerQsExpandPossible = true;
        }
        boolean twoFingerQsEvent = mTwoFingerQsExpandPossible && isOpenQsEvent(event);
        boolean oneFingerQsOverride = action == MotionEvent.ACTION_DOWN
                && shouldQuickSettingsIntercept(event.getX(), event.getY(), -1, false, true);
        if ((twoFingerQsEvent || oneFingerQsOverride)
                && event.getY(event.getActionIndex()) < mStatusBarMinHeight
                && mExpandedHeight <= mQsPeekHeight) {
            if (oneFingerQsOverride) {
                final SettingConfirmationSnackbarViewCreator
                        mSnackbarViewCreator = new
                        SettingConfirmationSnackbarViewCreator(mContext);
                SettingConfirmationHelper.prompt(
                        mSnackbarViewCreator.getSnackbarView(),
                        Settings.Secure.QUICK_SETTINGS_QUICK_PULL_DOWN,
                        true,
                        getContext().getString(R.string.quick_settings_quick_pull_down),
                        new SettingConfirmationHelper.OnSettingChoiceListener() {
                            @Override
                            public void onSettingConfirm(final String settingName) {
                            }

                            @Override
                            public void onSettingDeny(final String settingName) {
                                closeQs();
                            }
                        },
                        null);
            }

            MetricsLogger.count(mContext, COUNTER_PANEL_OPEN_QS, 1);
            mQsExpandImmediate = true;
            mNotificationStackScroller.setShouldShowShelfOnly(true);
            requestPanelHeightUpdate();

            // Normally, we start listening when the panel is expanded, but here we need to start
            // earlier so the state is already up to date when dragging down.
            setListening(true);
>>>>>>> 709513c2460f (SystemUI: add quick settings pull down with one finger)
        }
    }

    float getCurrentPanelAlpha() {
        return mCurrentPanelAlpha;
    }

<<<<<<< HEAD
    void setPanelAlphaInternal(float alpha) {
        mCurrentPanelAlpha = (int) alpha;
        mAlphaPaint.setARGB(mCurrentPanelAlpha, 255, 255, 255);
        invalidate();
=======
    private void handleQsDown(MotionEvent event) {
        if (event.getActionMasked() == MotionEvent.ACTION_DOWN
                && shouldQuickSettingsIntercept(event.getX(), event.getY(), -1, true, false)) {
            mFalsingManager.onQsDown();
            mQsTracking = true;
            onQsExpansionStarted();
            mInitialHeightOnTouch = mQsExpansionHeight;
            mInitialTouchY = event.getX();
            mInitialTouchX = event.getY();

            // If we interrupt an expansion gesture here, make sure to update the state correctly.
            notifyExpandingFinished();
        }
>>>>>>> 709513c2460f (SystemUI: add quick settings pull down with one finger)
    }

    public void setDozing(boolean dozing) {
        mDozing = dozing;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return !mDozing;
    }

    void setRtlChangeListener(RtlChangeListener listener) {
        mRtlChangeListener = listener;
    }

    public TapAgainView getTapAgainView() {
        return findViewById(R.id.shade_falsing_tap_again);
    }

<<<<<<< HEAD
    interface RtlChangeListener {
        void onRtlPropertielsChanged(int layoutDirection);
=======
    private int getFalsingThreshold() {
        float factor = mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
        return (int) (mQsFalsingThreshold * factor);
    }

    @Override
    public void onOverscrollTopChanged(float amount, boolean isRubberbanded) {
        cancelQsAnimation();
        if (!mQsExpansionEnabled) {
            amount = 0f;
        }
        float rounded = amount >= 1f ? amount : 0f;
        setOverScrolling(rounded != 0f && isRubberbanded);
        mQsExpansionFromOverscroll = rounded != 0f;
        mLastOverscroll = rounded;
        updateQsState();
        setQsExpansion(mQsMinExpansionHeight + rounded);
    }

    @Override
    public void flingTopOverscroll(float velocity, boolean open) {
        mLastOverscroll = 0f;
        mQsExpansionFromOverscroll = false;
        setQsExpansion(mQsExpansionHeight);
        flingSettings(!mQsExpansionEnabled && open ? 0f : velocity,
                open && mQsExpansionEnabled ? FLING_EXPAND : FLING_COLLAPSE,
                new Runnable() {
                    @Override
                    public void run() {
                        mStackScrollerOverscrolling = false;
                        setOverScrolling(false);
                        updateQsState();
                    }
                }, false /* isClick */);
    }

    private void setOverScrolling(boolean overscrolling) {
        mStackScrollerOverscrolling = overscrolling;
        if (mQs == null) return;
        mQs.setOverscrolling(overscrolling);
    }

    private void onQsExpansionStarted() {
        onQsExpansionStarted(0);
    }

    protected void onQsExpansionStarted(int overscrollAmount) {
        cancelQsAnimation();
        cancelHeightAnimator();

        // Reset scroll position and apply that position to the expanded height.
        float height = mQsExpansionHeight - overscrollAmount;
        setQsExpansion(height);
        requestPanelHeightUpdate();
        mNotificationStackScroller.checkSnoozeLeavebehind();

        // When expanding QS, let's authenticate the user if possible,
        // this will speed up notification actions.
        if (height == 0) {
            mStatusBar.requestFaceAuth();
        }
    }

    private void setQsExpanded(boolean expanded) {
        boolean changed = mQsExpanded != expanded;
        if (changed) {
            mQsExpanded = expanded;
            updateQsState();
            requestPanelHeightUpdate();
            mFalsingManager.setQsExpanded(expanded);
            mStatusBar.setQsExpanded(expanded);
            mNotificationContainerParent.setQsExpanded(expanded);
        }
    }

    @Override
    public void onStateChanged(int statusBarState) {
        boolean goingToFullShade = mStatusBarStateController.goingToFullShade();
        boolean keyguardFadingAway = mKeyguardMonitor.isKeyguardFadingAway();
        int oldState = mBarState;
        boolean keyguardShowing = statusBarState == StatusBarState.KEYGUARD;
        setKeyguardStatusViewVisibility(statusBarState, keyguardFadingAway, goingToFullShade);
        setKeyguardBottomAreaVisibility(statusBarState, goingToFullShade);

        mBarState = statusBarState;
        mKeyguardShowing = keyguardShowing;
        if (mQs != null) {
            mQs.setKeyguardShowing(mKeyguardShowing);
        }

        if (oldState == StatusBarState.KEYGUARD
                && (goingToFullShade || statusBarState == StatusBarState.SHADE_LOCKED)) {
            animateKeyguardStatusBarOut();
            long delay = mBarState == StatusBarState.SHADE_LOCKED
                    ? 0 : mKeyguardMonitor.calculateGoingToFullShadeDelay();
            mQs.animateHeaderSlidingIn(delay);
        } else if (oldState == StatusBarState.SHADE_LOCKED
                && statusBarState == StatusBarState.KEYGUARD) {
            animateKeyguardStatusBarIn(StackStateAnimator.ANIMATION_DURATION_STANDARD);
            // Only animate header if the header is visible. If not, it will partially animate out
            // the top of QS
            if (!mQsExpanded) {
                mQs.animateHeaderSlidingOut();
            }
        } else {
            mKeyguardStatusBar.setAlpha(1f);
            mKeyguardStatusBar.setVisibility(keyguardShowing ? View.VISIBLE : View.INVISIBLE);
            if (keyguardShowing && oldState != mBarState) {
                if (mQs != null) {
                    mQs.hideImmediately();
                }
            }
        }
        if (keyguardShowing) {
            updateDozingVisibilities(false /* animate */);
        }

        maybeAnimateBottomAreaAlpha();
        resetHorizontalPanelPosition();
        updateQsState();
    }

    private void maybeAnimateBottomAreaAlpha() {
        mBottomAreaShadeAlphaAnimator.cancel();
        if (mBarState == StatusBarState.SHADE_LOCKED) {
            mBottomAreaShadeAlphaAnimator.start();
        } else {
            mBottomAreaShadeAlpha = 1f;
        }
    }

    private final Runnable mAnimateKeyguardStatusViewInvisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardStatusViewAnimating = false;
            mKeyguardStatusView.setVisibility(View.INVISIBLE);
        }
    };

    private final Runnable mAnimateKeyguardStatusViewGoneEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardStatusViewAnimating = false;
            mKeyguardStatusView.setVisibility(View.GONE);
        }
    };

    private final Runnable mAnimateKeyguardStatusViewVisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardStatusViewAnimating = false;
        }
    };

    private final Runnable mAnimateKeyguardStatusBarInvisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardStatusBar.setVisibility(View.INVISIBLE);
            mKeyguardStatusBar.setAlpha(1f);
            mKeyguardStatusBarAnimateAlpha = 1f;
        }
    };

    private void animateKeyguardStatusBarOut() {
        ValueAnimator anim = ValueAnimator.ofFloat(mKeyguardStatusBar.getAlpha(), 0f);
        anim.addUpdateListener(mStatusBarAnimateAlphaListener);
        anim.setStartDelay(mKeyguardMonitor.isKeyguardFadingAway()
                ? mKeyguardMonitor.getKeyguardFadingAwayDelay()
                : 0);
        anim.setDuration(mKeyguardMonitor.isKeyguardFadingAway()
                ? mKeyguardMonitor.getKeyguardFadingAwayDuration() / 2
                : StackStateAnimator.ANIMATION_DURATION_STANDARD);
        anim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        anim.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mAnimateKeyguardStatusBarInvisibleEndRunnable.run();
            }
        });
        anim.start();
    }

    private final ValueAnimator.AnimatorUpdateListener mStatusBarAnimateAlphaListener =
            new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mKeyguardStatusBarAnimateAlpha = (float) animation.getAnimatedValue();
                    updateHeaderKeyguardAlpha();
                }
            };

    private void animateKeyguardStatusBarIn(long duration) {
        mKeyguardStatusBar.setVisibility(View.VISIBLE);
        mKeyguardStatusBar.setAlpha(0f);
        ValueAnimator anim = ValueAnimator.ofFloat(0f, 1f);
        anim.addUpdateListener(mStatusBarAnimateAlphaListener);
        anim.setDuration(duration);
        anim.setInterpolator(Interpolators.LINEAR_OUT_SLOW_IN);
        anim.start();
    }

    private final Runnable mAnimateKeyguardBottomAreaInvisibleEndRunnable = new Runnable() {
        @Override
        public void run() {
            mKeyguardBottomArea.setVisibility(View.GONE);
        }
    };

    private void setKeyguardBottomAreaVisibility(int statusBarState, boolean goingToFullShade) {
        mKeyguardBottomArea.animate().cancel();
        if (goingToFullShade) {
            mKeyguardBottomArea.animate()
                    .alpha(0f)
                    .setStartDelay(mKeyguardMonitor.getKeyguardFadingAwayDelay())
                    .setDuration(mKeyguardMonitor.getKeyguardFadingAwayDuration() / 2)
                    .setInterpolator(Interpolators.ALPHA_OUT)
                    .withEndAction(mAnimateKeyguardBottomAreaInvisibleEndRunnable)
                    .start();
        } else if (statusBarState == StatusBarState.KEYGUARD
                || statusBarState == StatusBarState.SHADE_LOCKED) {
            mKeyguardBottomArea.setVisibility(View.VISIBLE);
            mKeyguardBottomArea.setAlpha(1f);
        } else {
            mKeyguardBottomArea.setVisibility(View.GONE);
        }
    }

    private void setKeyguardStatusViewVisibility(int statusBarState, boolean keyguardFadingAway,
            boolean goingToFullShade) {
        mKeyguardStatusView.animate().cancel();
        mKeyguardStatusViewAnimating = false;
        if ((!keyguardFadingAway && mBarState == StatusBarState.KEYGUARD
                && statusBarState != StatusBarState.KEYGUARD) || goingToFullShade) {
            mKeyguardStatusViewAnimating = true;
            mKeyguardStatusView.animate()
                    .alpha(0f)
                    .setStartDelay(0)
                    .setDuration(160)
                    .setInterpolator(Interpolators.ALPHA_OUT)
                    .withEndAction(mAnimateKeyguardStatusViewGoneEndRunnable);
            if (keyguardFadingAway) {
                mKeyguardStatusView.animate()
                        .setStartDelay(mKeyguardMonitor.getKeyguardFadingAwayDelay())
                        .setDuration(mKeyguardMonitor.getKeyguardFadingAwayDuration() / 2)
                        .start();
            }
        } else if (mBarState == StatusBarState.SHADE_LOCKED
                && statusBarState == StatusBarState.KEYGUARD) {
            mKeyguardStatusView.setVisibility(View.VISIBLE);
            mKeyguardStatusViewAnimating = true;
            mKeyguardStatusView.setAlpha(0f);
            mKeyguardStatusView.animate()
                    .alpha(1f)
                    .setStartDelay(0)
                    .setDuration(320)
                    .setInterpolator(Interpolators.ALPHA_IN)
                    .withEndAction(mAnimateKeyguardStatusViewVisibleEndRunnable);
        } else if (statusBarState == StatusBarState.KEYGUARD) {
            if (keyguardFadingAway) {
                mKeyguardStatusViewAnimating = true;
                mKeyguardStatusView.animate()
                        .alpha(0)
                        .translationYBy(-getHeight() * 0.05f)
                        .setInterpolator(Interpolators.FAST_OUT_LINEAR_IN)
                        .setDuration(125)
                        .setStartDelay(0)
                        .withEndAction(mAnimateKeyguardStatusViewInvisibleEndRunnable)
                        .start();
            } else {
                mKeyguardStatusView.setVisibility(View.VISIBLE);
                mKeyguardStatusView.setAlpha(1f);
            }
        } else {
            mKeyguardStatusView.setVisibility(View.GONE);
            mKeyguardStatusView.setAlpha(1f);
        }
    }

    private void updateQsState() {
        mNotificationStackScroller.setQsExpanded(mQsExpanded);
        mNotificationStackScroller.setScrollingEnabled(
                mBarState != StatusBarState.KEYGUARD && (!mQsExpanded
                        || mQsExpansionFromOverscroll));
        updateEmptyShadeView();
        mQsNavbarScrim.setVisibility(mBarState == StatusBarState.SHADE && mQsExpanded
                && !mStackScrollerOverscrolling && mQsScrimEnabled
                ? View.VISIBLE
                : View.INVISIBLE);
        if (mKeyguardUserSwitcher != null && mQsExpanded && !mStackScrollerOverscrolling) {
            mKeyguardUserSwitcher.hideIfNotSimple(true /* animate */);
        }
        if (mQs == null) return;
        mQs.setExpanded(mQsExpanded);
    }

    private void setQsExpansion(float height) {
        height = Math.min(Math.max(height, mQsMinExpansionHeight), mQsMaxExpansionHeight);
        mQsFullyExpanded = height == mQsMaxExpansionHeight && mQsMaxExpansionHeight != 0;
        if (height > mQsMinExpansionHeight && !mQsExpanded && !mStackScrollerOverscrolling) {
            setQsExpanded(true);
        } else if (height <= mQsMinExpansionHeight && mQsExpanded) {
            setQsExpanded(false);
        }
        mQsExpansionHeight = height;
        updateQsExpansion();
        requestScrollerTopPaddingUpdate(false /* animate */);
        updateHeaderKeyguardAlpha();
        if (mBarState == StatusBarState.SHADE_LOCKED
                || mBarState == StatusBarState.KEYGUARD) {
            updateKeyguardBottomAreaAlpha();
            updateBigClockAlpha();
        }
        if (mBarState == StatusBarState.SHADE && mQsExpanded
                && !mStackScrollerOverscrolling && mQsScrimEnabled) {
            mQsNavbarScrim.setAlpha(getQsExpansionFraction());
        }

        if (mAccessibilityManager.isEnabled()) {
            setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        }

        if (!mFalsingManager.isUnlockingDisabled() && mQsFullyExpanded
                && mFalsingManager.shouldEnforceBouncer()) {
            mStatusBar.executeRunnableDismissingKeyguard(null, null /* cancelAction */,
                    false /* dismissShade */, true /* afterKeyguardGone */, false /* deferred */);
        }
        if (mExpansionListener != null) {
            mExpansionListener.onQsExpansionChanged(mQsMaxExpansionHeight != 0
                    ? mQsExpansionHeight / mQsMaxExpansionHeight : 0);
        }
        if (DEBUG) {
            invalidate();
        }
    }

    protected void updateQsExpansion() {
        if (mQs == null) return;
        float qsExpansionFraction = getQsExpansionFraction();
        mQs.setQsExpansion(qsExpansionFraction, getHeaderTranslation());
        mNotificationStackScroller.setQsExpansionFraction(qsExpansionFraction);
    }

    private String determineAccessibilityPaneTitle() {
        if (mQs != null && mQs.isCustomizing()) {
            return getContext().getString(R.string.accessibility_desc_quick_settings_edit);
        } else if (mQsExpansionHeight != 0.0f && mQsFullyExpanded) {
            // Upon initialisation when we are not layouted yet we don't want to announce that we
            // are fully expanded, hence the != 0.0f check.
            return getContext().getString(R.string.accessibility_desc_quick_settings);
        } else if (mBarState == StatusBarState.KEYGUARD) {
            return getContext().getString(R.string.accessibility_desc_lock_screen);
        } else {
            return getContext().getString(R.string.accessibility_desc_notification_shade);
        }
    }

    private float calculateQsTopPadding() {
        if (mKeyguardShowing
                && (mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted)) {

            // Either QS pushes the notifications down when fully expanded, or QS is fully above the
            // notifications (mostly on tablets). maxNotificationPadding denotes the normal top
            // padding on Keyguard, maxQsPadding denotes the top padding from the quick settings
            // panel. We need to take the maximum and linearly interpolate with the panel expansion
            // for a nice motion.
            int maxNotificationPadding = mClockPositionResult.stackScrollerPadding;
            int maxQsPadding = mQsMaxExpansionHeight + mQsNotificationTopPadding;
            int max = mBarState == StatusBarState.KEYGUARD
                    ? Math.max(maxNotificationPadding, maxQsPadding)
                    : maxQsPadding;
            return (int) MathUtils.lerp((float) mQsMinExpansionHeight, (float) max,
                    getExpandedFraction());
        } else if (mQsSizeChangeAnimator != null) {
            return (int) mQsSizeChangeAnimator.getAnimatedValue();
        } else if (mKeyguardShowing) {
            // We can only do the smoother transition on Keyguard when we also are not collapsing
            // from a scrolled quick settings.
            return MathUtils.lerp((float) mNotificationStackScroller.getIntrinsicPadding(),
                    (float) (mQsMaxExpansionHeight + mQsNotificationTopPadding),
                    getQsExpansionFraction());
        } else {
            return mQsExpansionHeight + mQsNotificationTopPadding;
        }
    }

    protected void requestScrollerTopPaddingUpdate(boolean animate) {
        mNotificationStackScroller.updateTopPadding(calculateQsTopPadding(),
                animate, mKeyguardShowing
                        && (mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted));
    }

    private void trackMovement(MotionEvent event) {
        if (mQsVelocityTracker != null) mQsVelocityTracker.addMovement(event);
        mLastTouchX = event.getX();
        mLastTouchY = event.getY();
    }

    private void initVelocityTracker() {
        if (mQsVelocityTracker != null) {
            mQsVelocityTracker.recycle();
        }
        mQsVelocityTracker = VelocityTracker.obtain();
    }

    private float getCurrentQSVelocity() {
        if (mQsVelocityTracker == null) {
            return 0;
        }
        mQsVelocityTracker.computeCurrentVelocity(1000);
        return mQsVelocityTracker.getYVelocity();
    }

    private void cancelQsAnimation() {
        if (mQsExpansionAnimator != null) {
            mQsExpansionAnimator.cancel();
        }
    }

    /**
     * @see #flingSettings(float, int, Runnable, boolean)
     */
    public void flingSettings(float vel, int type) {
        flingSettings(vel, type, null, false /* isClick */);
    }

    /**
     * Animates QS or QQS as if the user had swiped up or down.
     *
     * @param vel Finger velocity or 0 when not initiated by touch events.
     * @param type Either {@link #FLING_EXPAND}, {@link #FLING_COLLAPSE} or {@link #FLING_HIDE}.
     * @param onFinishRunnable Runnable to be executed at the end of animation.
     * @param isClick If originated by click (different interpolator and duration.)
     */
    protected void flingSettings(float vel, int type, final Runnable onFinishRunnable,
            boolean isClick) {
        float target;
        switch (type) {
            case FLING_EXPAND:
                target = mQsMaxExpansionHeight;
                break;
            case FLING_COLLAPSE:
                target = mQsMinExpansionHeight;
                break;
            case FLING_HIDE:
            default:
                target = 0;
        }
        if (target == mQsExpansionHeight) {
            if (onFinishRunnable != null) {
                onFinishRunnable.run();
            }
            return;
        }

        // If we move in the opposite direction, reset velocity and use a different duration.
        boolean oppositeDirection = false;
        boolean expanding = type == FLING_EXPAND;
        if (vel > 0 && !expanding || vel < 0 && expanding) {
            vel = 0;
            oppositeDirection = true;
        }
        ValueAnimator animator = ValueAnimator.ofFloat(mQsExpansionHeight, target);
        if (isClick) {
            animator.setInterpolator(Interpolators.TOUCH_RESPONSE);
            animator.setDuration(368);
        } else {
            mFlingAnimationUtils.apply(animator, mQsExpansionHeight, target, vel);
        }
        if (oppositeDirection) {
            animator.setDuration(350);
        }
        animator.addUpdateListener(animation -> {
            setQsExpansion((Float) animation.getAnimatedValue());
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mNotificationStackScroller.resetCheckSnoozeLeavebehind();
                mQsExpansionAnimator = null;
                if (onFinishRunnable != null) {
                    onFinishRunnable.run();
                }
            }
        });
        animator.start();
        mQsExpansionAnimator = animator;
        mQsAnimatorExpand = expanding;
    }

    /**
     * @return Whether we should intercept a gesture to open Quick Settings.
     */
    private boolean shouldQuickSettingsIntercept(float x, float y, float yDiff, boolean useHeader,
            boolean notSetFallback) {
        if (!mQsExpansionEnabled || (useHeader && mCollapsedOnDown)) {
            return false;
        }
        View header = mKeyguardShowing || mQs == null ? mKeyguardStatusBar : mQs.getHeader();
        final boolean onHeader = useHeader && x >= mQsFrame.getX()
                && x <= mQsFrame.getX() + mQsFrame.getWidth()
                && y >= header.getTop() && y <= header.getBottom();

        final float w = getMeasuredWidth();
        float region = (w * (1.f/4.f)); // TODO overlay region fraction?
        final boolean showQsOverride = (isLayoutRtl() ? (x < region) : (w - region < x))
            && !mKeyguardShowing && SettingConfirmationHelper.get(
                    getContext().getContentResolver(),
                    Settings.Secure.QUICK_SETTINGS_QUICK_PULL_DOWN,
                    notSetFallback);

        if (mQsExpanded) {
            return onHeader || (yDiff < 0 && isInQsArea(x, y));
        } else {
            return onHeader || showQsOverride;
        }
    }

    @Override
    protected boolean isScrolledToBottom() {
        if (!isInSettings()) {
            return mBarState == StatusBarState.KEYGUARD
                    || mNotificationStackScroller.isScrolledToBottom();
        } else {
            return true;
        }
    }

    @Override
    protected int getMaxPanelHeight() {
        int min = mStatusBarMinHeight;
        if (mBarState != StatusBarState.KEYGUARD
                && mNotificationStackScroller.getNotGoneChildCount() == 0) {
            int minHeight = (int) (mQsMinExpansionHeight + getOverExpansionAmount());
            min = Math.max(min, minHeight);
        }
        int maxHeight;
        if (mQsExpandImmediate || mQsExpanded || mIsExpanding && mQsExpandedWhenExpandingStarted
                || mPulsing) {
            maxHeight = calculatePanelHeightQsExpanded();
        } else {
            maxHeight = calculatePanelHeightShade();
        }
        maxHeight = Math.max(maxHeight, min);
        return maxHeight;
    }

    public boolean isInSettings() {
        return mQsExpanded;
    }

    public boolean isExpanding() {
        return mIsExpanding;
    }

    @Override
    protected void onHeightUpdated(float expandedHeight) {
        if (!mQsExpanded || mQsExpandImmediate || mIsExpanding && mQsExpandedWhenExpandingStarted) {
            // Updating the clock position will set the top padding which might
            // trigger a new panel height and re-position the clock.
            // This is a circular dependency and should be avoided, otherwise we'll have
            // a stack overflow.
            if (mStackScrollerMeasuringPass > 2) {
                if (DEBUG) Log.d(TAG, "Unstable notification panel height. Aborting.");
            } else {
                positionClockAndNotifications();
            }
        }
        if (mQsExpandImmediate || mQsExpanded && !mQsTracking && mQsExpansionAnimator == null
                && !mQsExpansionFromOverscroll) {
            float t;
            if (mKeyguardShowing) {

                // On Keyguard, interpolate the QS expansion linearly to the panel expansion
                t = expandedHeight / (getMaxPanelHeight());
            } else {
                // In Shade, interpolate linearly such that QS is closed whenever panel height is
                // minimum QS expansion + minStackHeight
                float panelHeightQsCollapsed = mNotificationStackScroller.getIntrinsicPadding()
                        + mNotificationStackScroller.getLayoutMinHeight();
                float panelHeightQsExpanded = calculatePanelHeightQsExpanded();
                t = (expandedHeight - panelHeightQsCollapsed)
                        / (panelHeightQsExpanded - panelHeightQsCollapsed);
            }
            setQsExpansion(mQsMinExpansionHeight
                    + t * (mQsMaxExpansionHeight - mQsMinExpansionHeight));
        }
        updateExpandedHeight(expandedHeight);
        updateHeader();
        updateNotificationTranslucency();
        updatePanelExpanded();
        updateGestureExclusionRect();
        if (DEBUG) {
            invalidate();
        }
    }

    private void updatePanelExpanded() {
        boolean isExpanded = !isFullyCollapsed();
        if (mPanelExpanded != isExpanded) {
            mHeadsUpManager.setIsPanelExpanded(isExpanded);
            mStatusBar.setPanelExpanded(isExpanded);
            mPanelExpanded = isExpanded;
        }
    }

    private int calculatePanelHeightShade() {
        int emptyBottomMargin = mNotificationStackScroller.getEmptyBottomMargin();
        int maxHeight = mNotificationStackScroller.getHeight() - emptyBottomMargin;
        maxHeight += mNotificationStackScroller.getTopPaddingOverflow();

        if (mBarState == StatusBarState.KEYGUARD) {
            int minKeyguardPanelBottom = mClockPositionAlgorithm.getExpandedClockPosition()
                    + mKeyguardStatusView.getHeight()
                    + mNotificationStackScroller.getIntrinsicContentHeight();
            return Math.max(maxHeight, minKeyguardPanelBottom);
        } else {
            return maxHeight;
        }
    }

    private int calculatePanelHeightQsExpanded() {
        float notificationHeight = mNotificationStackScroller.getHeight()
                - mNotificationStackScroller.getEmptyBottomMargin()
                - mNotificationStackScroller.getTopPadding();

        // When only empty shade view is visible in QS collapsed state, simulate that we would have
        // it in expanded QS state as well so we don't run into troubles when fading the view in/out
        // and expanding/collapsing the whole panel from/to quick settings.
        if (mNotificationStackScroller.getNotGoneChildCount() == 0
                && mShowEmptyShadeView) {
            notificationHeight = mNotificationStackScroller.getEmptyShadeViewHeight();
        }
        int maxQsHeight = mQsMaxExpansionHeight;

        if (mKeyguardShowing) {
            maxQsHeight += mQsNotificationTopPadding;
        }

        // If an animation is changing the size of the QS panel, take the animated value.
        if (mQsSizeChangeAnimator != null) {
            maxQsHeight = (int) mQsSizeChangeAnimator.getAnimatedValue();
        }
        float totalHeight = Math.max(
                maxQsHeight, mBarState == StatusBarState.KEYGUARD
                        ? mClockPositionResult.stackScrollerPadding : 0)
                + notificationHeight + mNotificationStackScroller.getTopPaddingOverflow();
        if (totalHeight > mNotificationStackScroller.getHeight()) {
            float fullyCollapsedHeight = maxQsHeight
                    + mNotificationStackScroller.getLayoutMinHeight();
            totalHeight = Math.max(fullyCollapsedHeight, mNotificationStackScroller.getHeight());
        }
        return (int) totalHeight;
    }

    private void updateNotificationTranslucency() {
        float alpha = 1f;
        if (mClosingWithAlphaFadeOut && !mExpandingFromHeadsUp &&
                !mHeadsUpManager.hasPinnedHeadsUp()) {
            alpha = getFadeoutAlpha();
        }
        if (mBarState == StatusBarState.KEYGUARD && !mHintAnimationRunning) {
            alpha *= mClockPositionResult.clockAlpha;
        }
        mNotificationStackScroller.setAlpha(alpha);
    }

    private float getFadeoutAlpha() {
        float alpha = (getNotificationsTopY() + mNotificationStackScroller.getFirstItemMinHeight())
                / mQsMinExpansionHeight;
        alpha = Math.max(0, Math.min(alpha, 1));
        alpha = (float) Math.pow(alpha, 0.75);
        return alpha;
    }

    @Override
    protected float getOverExpansionAmount() {
        return mNotificationStackScroller.getCurrentOverScrollAmount(true /* top */);
    }

    @Override
    protected float getOverExpansionPixels() {
        return mNotificationStackScroller.getCurrentOverScrolledPixels(true /* top */);
    }

    /**
     * Hides the header when notifications are colliding with it.
     */
    private void updateHeader() {
        if (mBarState == StatusBarState.KEYGUARD) {
            updateHeaderKeyguardAlpha();
        }
        updateQsExpansion();
    }

    protected float getHeaderTranslation() {
        if (mBarState == StatusBarState.KEYGUARD) {
            return 0;
        }
        float translation = MathUtils.lerp(-mQsMinExpansionHeight, 0,
                Math.min(1.0f, mNotificationStackScroller.getAppearFraction(mExpandedHeight)))
                + mExpandOffset;
        return Math.min(0, translation);
    }

    /**
     * @return the alpha to be used to fade out the contents on Keyguard (status bar, bottom area)
     *         during swiping up
     */
    private float getKeyguardContentsAlpha() {
        float alpha;
        if (mBarState == StatusBarState.KEYGUARD) {

            // When on Keyguard, we hide the header as soon as the top card of the notification
            // stack scroller is close enough (collision distance) to the bottom of the header.
            alpha = getNotificationsTopY()
                    /
                    (mKeyguardStatusBar.getHeight() + mNotificationsHeaderCollideDistance);
        } else {

            // In SHADE_LOCKED, the top card is already really close to the header. Hide it as
            // soon as we start translating the stack.
            alpha = getNotificationsTopY() / mKeyguardStatusBar.getHeight();
        }
        alpha = MathUtils.constrain(alpha, 0, 1);
        alpha = (float) Math.pow(alpha, 0.75);
        return alpha;
    }

    private void updateHeaderKeyguardAlpha() {
        if (!mKeyguardShowing) {
            return;
        }
        float alphaQsExpansion = 1 - Math.min(1, getQsExpansionFraction() * 2);
        float newAlpha = Math.min(getKeyguardContentsAlpha(), alphaQsExpansion)
                * mKeyguardStatusBarAnimateAlpha;
        mKeyguardStatusBar.setAlpha(newAlpha);
        mKeyguardStatusBar.setVisibility(newAlpha != 0f && !mDozing ? VISIBLE : INVISIBLE);
    }

    private void updateKeyguardBottomAreaAlpha() {
        // There are two possible panel expansion behaviors:
        // • User dragging up to unlock: we want to fade out as quick as possible
        //   (ALPHA_EXPANSION_THRESHOLD) to avoid seeing the bouncer over the bottom area.
        // • User tapping on lock screen: bouncer won't be visible but panel expansion will
        //   change due to "unlock hint animation." In this case, fading out the bottom area
        //   would also hide the message that says "swipe to unlock," we don't want to do that.
        float expansionAlpha = MathUtils.map(isUnlockHintRunning()
                        ? 0 : KeyguardBouncer.ALPHA_EXPANSION_THRESHOLD, 1f,
                0f, 1f, getExpandedFraction());
        float alpha = Math.min(expansionAlpha, 1 - getQsExpansionFraction());
        alpha *= mBottomAreaShadeAlpha;
        mKeyguardBottomArea.setAffordanceAlpha(alpha);
        mKeyguardBottomArea.setImportantForAccessibility(alpha == 0f
                ? IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
                : IMPORTANT_FOR_ACCESSIBILITY_AUTO);
        View ambientIndicationContainer = mStatusBar.getAmbientIndicationContainer();
        if (ambientIndicationContainer != null) {
            ambientIndicationContainer.setAlpha(alpha);
        }
    }

    /**
     * Custom clock fades away when user drags up to unlock or pulls down quick settings.
     *
     * Updates alpha of custom clock to match the alpha of the KeyguardBottomArea. See
     * {@link updateKeyguardBottomAreaAlpha}.
     */
    private void updateBigClockAlpha() {
        float expansionAlpha = MathUtils.map(isUnlockHintRunning()
                ? 0 : KeyguardBouncer.ALPHA_EXPANSION_THRESHOLD, 1f, 0f, 1f, getExpandedFraction());
        float alpha = Math.min(expansionAlpha, 1 - getQsExpansionFraction());
        mBigClockContainer.setAlpha(alpha);
    }

    private float getNotificationsTopY() {
        if (mNotificationStackScroller.getNotGoneChildCount() == 0) {
            return getExpandedHeight();
        }
        return mNotificationStackScroller.getNotificationsTopY();
    }

    @Override
    protected void onExpandingStarted() {
        super.onExpandingStarted();
        mNotificationStackScroller.onExpansionStarted();
        mIsExpanding = true;
        mQsExpandedWhenExpandingStarted = mQsFullyExpanded;
        if (mQsExpanded) {
            onQsExpansionStarted();
        }
        // Since there are QS tiles in the header now, we need to make sure we start listening
        // immediately so they can be up to date.
        if (mQs == null) return;
        mQs.setHeaderListening(true);
    }

    @Override
    protected void onExpandingFinished() {
        super.onExpandingFinished();
        mNotificationStackScroller.onExpansionStopped();
        mHeadsUpManager.onExpandingFinished();
        mIsExpanding = false;
        if (isFullyCollapsed()) {
            DejankUtils.postAfterTraversal(new Runnable() {
                @Override
                public void run() {
                    setListening(false);
                }
            });

            // Workaround b/22639032: Make sure we invalidate something because else RenderThread
            // thinks we are actually drawing a frame put in reality we don't, so RT doesn't go
            // ahead with rendering and we jank.
            postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    getParent().invalidateChild(NotificationPanelView.this, mDummyDirtyRect);
                }
            });
        } else {
            setListening(true);
        }
        mQsExpandImmediate = false;
        mNotificationStackScroller.setShouldShowShelfOnly(false);
        mTwoFingerQsExpandPossible = false;
        mIsExpansionFromHeadsUp = false;
        notifyListenersTrackingHeadsUp(null);
        mExpandingFromHeadsUp = false;
        setPanelScrimMinFraction(0.0f);
    }

    private void notifyListenersTrackingHeadsUp(ExpandableNotificationRow pickedChild) {
        for (int i = 0; i < mTrackingHeadsUpListeners.size(); i++) {
            Consumer<ExpandableNotificationRow> listener
                    = mTrackingHeadsUpListeners.get(i);
            listener.accept(pickedChild);
        }
    }

    private void setListening(boolean listening) {
        mKeyguardStatusBar.setListening(listening);
        if (mQs == null) return;
        mQs.setListening(listening);
    }

    @Override
    public void expand(boolean animate) {
        super.expand(animate);
        setListening(true);
    }

    @Override
    protected void setOverExpansion(float overExpansion, boolean isPixels) {
        if (mConflictingQsExpansionGesture || mQsExpandImmediate) {
            return;
        }
        if (mBarState != StatusBarState.KEYGUARD) {
            mNotificationStackScroller.setOnHeightChangedListener(null);
            if (isPixels) {
                mNotificationStackScroller.setOverScrolledPixels(
                        overExpansion, true /* onTop */, false /* animate */);
            } else {
                mNotificationStackScroller.setOverScrollAmount(
                        overExpansion, true /* onTop */, false /* animate */);
            }
            mNotificationStackScroller.setOnHeightChangedListener(this);
        }
    }

    @Override
    protected void onTrackingStarted() {
        mFalsingManager.onTrackingStarted(mStatusBar.isKeyguardCurrentlySecure());
        super.onTrackingStarted();
        if (mQsFullyExpanded) {
            mQsExpandImmediate = true;
            mNotificationStackScroller.setShouldShowShelfOnly(true);
        }
        if (mBarState == StatusBarState.KEYGUARD
                || mBarState == StatusBarState.SHADE_LOCKED) {
            mAffordanceHelper.animateHideLeftRightIcon();
        }
        mNotificationStackScroller.onPanelTrackingStarted();
    }

    @Override
    protected void onTrackingStopped(boolean expand) {
        mFalsingManager.onTrackingStopped();
        super.onTrackingStopped(expand);
        if (expand) {
            mNotificationStackScroller.setOverScrolledPixels(
                    0.0f, true /* onTop */, true /* animate */);
        }
        mNotificationStackScroller.onPanelTrackingStopped();
        if (expand && (mBarState == StatusBarState.KEYGUARD
                || mBarState == StatusBarState.SHADE_LOCKED)) {
            if (!mHintAnimationRunning) {
                mAffordanceHelper.reset(true);
            }
        }
    }

    @Override
    public void onHeightChanged(ExpandableView view, boolean needsAnimation) {

        // Block update if we are in quick settings and just the top padding changed
        // (i.e. view == null).
        if (view == null && mQsExpanded) {
            return;
        }
        if (needsAnimation && mInterpolatedDarkAmount == 0) {
            mAnimateNextPositionUpdate = true;
        }
        ExpandableView firstChildNotGone = mNotificationStackScroller.getFirstChildNotGone();
        ExpandableNotificationRow firstRow = firstChildNotGone instanceof ExpandableNotificationRow
                ? (ExpandableNotificationRow) firstChildNotGone
                : null;
        if (firstRow != null
                && (view == firstRow || (firstRow.getNotificationParent() == firstRow))) {
            requestScrollerTopPaddingUpdate(false /* animate */);
        }
        requestPanelHeightUpdate();
    }

    @Override
    public void onReset(ExpandableView view) {
    }

    public void onQsHeightChanged() {
        mQsMaxExpansionHeight = mQs != null ? mQs.getDesiredHeight() : 0;
        if (mQsExpanded && mQsFullyExpanded) {
            mQsExpansionHeight = mQsMaxExpansionHeight;
            requestScrollerTopPaddingUpdate(false /* animate */);
            requestPanelHeightUpdate();
        }
        if (mAccessibilityManager.isEnabled()) {
            setAccessibilityPaneTitle(determineAccessibilityPaneTitle());
        }
        mNotificationStackScroller.setMaxTopPadding(
                mQsMaxExpansionHeight + mQsNotificationTopPadding);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mAffordanceHelper.onConfigurationChanged();
        if (newConfig.orientation != mLastOrientation) {
            resetHorizontalPanelPosition();
        }
        mLastOrientation = newConfig.orientation;
    }

    @Override
    public WindowInsets onApplyWindowInsets(WindowInsets insets) {
        mNavigationBarBottomHeight = insets.getStableInsetBottom();
        updateMaxHeadsUpTranslation();
        return insets;
    }

    private void updateMaxHeadsUpTranslation() {
        mNotificationStackScroller.setHeadsUpBoundaries(getHeight(), mNavigationBarBottomHeight);
    }

    @Override
    public void onRtlPropertiesChanged(int layoutDirection) {
        if (layoutDirection != mOldLayoutDirection) {
            mAffordanceHelper.onRtlPropertiesChanged();
            mOldLayoutDirection = layoutDirection;
        }
    }

    @Override
    public void onClick(View v) {
        onQsExpansionStarted();
        if (mQsExpanded) {
            flingSettings(0 /* vel */, FLING_COLLAPSE, null /* onFinishRunnable */,
                    true /* isClick */);
        } else if (mQsExpansionEnabled) {
            mLockscreenGestureLogger.write(MetricsEvent.ACTION_SHADE_QS_TAP, 0, 0);
            flingSettings(0 /* vel */, FLING_EXPAND, null /* onFinishRunnable */,
                    true /* isClick */);
        }
    }

    @Override
    public void onAnimationToSideStarted(boolean rightPage, float translation, float vel) {
        boolean start = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? rightPage : !rightPage;
        mIsLaunchTransitionRunning = true;
        mLaunchAnimationEndRunnable = null;
        float displayDensity = mStatusBar.getDisplayDensity();
        int lengthDp = Math.abs((int) (translation / displayDensity));
        int velocityDp = Math.abs((int) (vel / displayDensity));
        if (start) {
            mLockscreenGestureLogger.write(MetricsEvent.ACTION_LS_DIALER, lengthDp, velocityDp);

            mFalsingManager.onLeftAffordanceOn();
            if (mFalsingManager.shouldEnforceBouncer()) {
                mStatusBar.executeRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public void run() {
                        mKeyguardBottomArea.launchLeftAffordance();
                    }
                }, null, true /* dismissShade */, false /* afterKeyguardGone */,
                        true /* deferred */);
            } else {
                mKeyguardBottomArea.launchLeftAffordance();
            }
        } else {
            if (KeyguardBottomAreaView.CAMERA_LAUNCH_SOURCE_AFFORDANCE.equals(
                    mLastCameraLaunchSource)) {
                mLockscreenGestureLogger.write(MetricsEvent.ACTION_LS_CAMERA, lengthDp, velocityDp);
            }
            mFalsingManager.onCameraOn();
            if (mFalsingManager.shouldEnforceBouncer()) {
                mStatusBar.executeRunnableDismissingKeyguard(new Runnable() {
                    @Override
                    public void run() {
                        mKeyguardBottomArea.launchCamera(mLastCameraLaunchSource);
                    }
                }, null, true /* dismissShade */, false /* afterKeyguardGone */,
                    true /* deferred */);
            }
            else {
                mKeyguardBottomArea.launchCamera(mLastCameraLaunchSource);
            }
        }
        mStatusBar.startLaunchTransitionTimeout();
        mBlockTouches = true;
    }

    @Override
    public void onAnimationToSideEnded() {
        mIsLaunchTransitionRunning = false;
        mIsLaunchTransitionFinished = true;
        if (mLaunchAnimationEndRunnable != null) {
            mLaunchAnimationEndRunnable.run();
            mLaunchAnimationEndRunnable = null;
        }
        mStatusBar.readyForKeyguardDone();
    }

    @Override
    protected void startUnlockHintAnimation() {
        if (mPowerManager.isPowerSaveMode()) {
            onUnlockHintStarted();
            onUnlockHintFinished();
            return;
        }
        super.startUnlockHintAnimation();
    }

    @Override
    public float getMaxTranslationDistance() {
        return (float) Math.hypot(getWidth(), getHeight());
    }

    @Override
    public void onSwipingStarted(boolean rightIcon) {
        mFalsingManager.onAffordanceSwipingStarted(rightIcon);
        boolean camera = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? !rightIcon
                : rightIcon;
        if (camera) {
            mKeyguardBottomArea.bindCameraPrewarmService();
        }
        requestDisallowInterceptTouchEvent(true);
        mOnlyAffordanceInThisMotion = true;
        mQsTracking = false;
    }

    @Override
    public void onSwipingAborted() {
        mFalsingManager.onAffordanceSwipingAborted();
        mKeyguardBottomArea.unbindCameraPrewarmService(false /* launched */);
    }

    @Override
    public void onIconClicked(boolean rightIcon) {
        if (mHintAnimationRunning) {
            return;
        }
        mHintAnimationRunning = true;
        mAffordanceHelper.startHintAnimation(rightIcon, new Runnable() {
            @Override
            public void run() {
                mHintAnimationRunning = false;
                mStatusBar.onHintFinished();
            }
        });
        rightIcon = getLayoutDirection() == LAYOUT_DIRECTION_RTL ? !rightIcon : rightIcon;
        if (rightIcon) {
            mStatusBar.onCameraHintStarted();
        } else {
            if (mKeyguardBottomArea.isLeftVoiceAssist()) {
                mStatusBar.onVoiceAssistHintStarted();
            } else {
                mStatusBar.onPhoneHintStarted();
            }
        }
    }

    @Override
    protected void onUnlockHintFinished() {
        super.onUnlockHintFinished();
        mNotificationStackScroller.setUnlockHintRunning(false);
    }

    @Override
    protected void onUnlockHintStarted() {
        super.onUnlockHintStarted();
        mNotificationStackScroller.setUnlockHintRunning(true);
    }

    @Override
    public KeyguardAffordanceView getLeftIcon() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getRightView()
                : mKeyguardBottomArea.getLeftView();
    }

    @Override
    public KeyguardAffordanceView getRightIcon() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getLeftView()
                : mKeyguardBottomArea.getRightView();
    }

    @Override
    public View getLeftPreview() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getRightPreview()
                : mKeyguardBottomArea.getLeftPreview();
    }

    @Override
    public View getRightPreview() {
        return getLayoutDirection() == LAYOUT_DIRECTION_RTL
                ? mKeyguardBottomArea.getLeftPreview()
                : mKeyguardBottomArea.getRightPreview();
    }

    @Override
    public float getAffordanceFalsingFactor() {
        return mStatusBar.isWakeUpComingFromTouch() ? 1.5f : 1.0f;
    }

    @Override
    public boolean needsAntiFalsing() {
        return mBarState == StatusBarState.KEYGUARD;
    }

    @Override
    protected float getPeekHeight() {
        if (mNotificationStackScroller.getNotGoneChildCount() > 0) {
            return mNotificationStackScroller.getPeekHeight();
        } else {
            return mQsMinExpansionHeight;
        }
    }

    @Override
    protected boolean shouldUseDismissingAnimation() {
        return mBarState != StatusBarState.SHADE
                && (!mStatusBar.isKeyguardCurrentlySecure() || !isTracking());
    }

    @Override
    protected boolean fullyExpandedClearAllVisible() {
        return mNotificationStackScroller.isFooterViewNotGone()
                && mNotificationStackScroller.isScrolledToBottom() && !mQsExpandImmediate;
    }

    @Override
    protected boolean isClearAllVisible() {
        return mNotificationStackScroller.isFooterViewContentVisible();
    }

    @Override
    protected int getClearAllHeight() {
        return mNotificationStackScroller.getFooterViewHeight();
    }

    @Override
    protected boolean isTrackingBlocked() {
        return mConflictingQsExpansionGesture && mQsExpanded || mBlockingExpansionForCurrentTouch;
    }

    public boolean isQsExpanded() {
        return mQsExpanded;
    }

    public boolean isQsDetailShowing() {
        return mQs.isShowingDetail();
    }

    public void closeQsDetail() {
        mQs.closeDetail();
    }

    @Override
    public boolean shouldDelayChildPressedState() {
        return true;
    }

    public boolean isLaunchTransitionFinished() {
        return mIsLaunchTransitionFinished;
    }

    public boolean isLaunchTransitionRunning() {
        return mIsLaunchTransitionRunning;
    }

    public void setLaunchTransitionEndRunnable(Runnable r) {
        mLaunchAnimationEndRunnable = r;
    }

    public void setEmptyDragAmount(float amount) {
        mEmptyDragAmount = amount * 0.2f;
        positionClockAndNotifications();
    }

    private void updateDozingVisibilities(boolean animate) {
        mKeyguardBottomArea.setDozing(mDozing, animate);
        if (!mDozing && animate) {
            animateKeyguardStatusBarIn(StackStateAnimator.ANIMATION_DURATION_STANDARD);
        }
    }

    @Override
    public boolean isDozing() {
        return mDozing;
    }

    public void showEmptyShadeView(boolean emptyShadeViewVisible) {
        mShowEmptyShadeView = emptyShadeViewVisible;
        updateEmptyShadeView();
    }

    private void updateEmptyShadeView() {
        // Hide "No notifications" in QS.
        mNotificationStackScroller.updateEmptyShadeView(mShowEmptyShadeView && !mQsExpanded);
    }

    public void setQsScrimEnabled(boolean qsScrimEnabled) {
        boolean changed = mQsScrimEnabled != qsScrimEnabled;
        mQsScrimEnabled = qsScrimEnabled;
        if (changed) {
            updateQsState();
        }
    }

    public void setKeyguardUserSwitcher(KeyguardUserSwitcher keyguardUserSwitcher) {
        mKeyguardUserSwitcher = keyguardUserSwitcher;
    }

    public void onScreenTurningOn() {
        mKeyguardStatusView.dozeTimeTick();
    }

    @Override
    public void onEmptySpaceClicked(float x, float y) {
        onEmptySpaceClick(x);
    }

    @Override
    protected boolean onMiddleClicked() {
        switch (mBarState) {
            case StatusBarState.KEYGUARD:
                if (!mDozingOnDown) {
                    mLockscreenGestureLogger.write(
                            MetricsEvent.ACTION_LS_HINT,
                            0 /* lengthDp - N/A */, 0 /* velocityDp - N/A */);
                    startUnlockHintAnimation();
                }
                return true;
            case StatusBarState.SHADE_LOCKED:
                if (!mQsExpanded) {
                    mShadeController.goToKeyguard();
                }
                return true;
            case StatusBarState.SHADE:

                // This gets called in the middle of the touch handling, where the state is still
                // that we are tracking the panel. Collapse the panel after this is done.
                post(mPostCollapseRunnable);
                return false;
            default:
                return true;
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mCurrentPanelAlpha != 255) {
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mAlphaPaint);
        }
    }

    public float getCurrentPanelAlpha() {
        return mCurrentPanelAlpha;
    }

    public boolean setPanelAlpha(int alpha, boolean animate) {
        if (mPanelAlpha != alpha) {
            mPanelAlpha = alpha;
            PropertyAnimator.setProperty(this, PANEL_ALPHA, alpha,
                    alpha == 255 ? PANEL_ALPHA_IN_PROPERTIES : PANEL_ALPHA_OUT_PROPERTIES, animate);
            return true;
        }
        return false;
    }

    public void setPanelAlphaInternal(float alpha) {
        mCurrentPanelAlpha = (int) alpha;
        mAlphaPaint.setARGB(mCurrentPanelAlpha, 255, 255, 255);
        invalidate();
    }

    public void setPanelAlphaEndAction(Runnable r) {
        mPanelAlphaEndAction = r;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (DEBUG) {
            Paint p = new Paint();
            p.setColor(Color.RED);
            p.setStrokeWidth(2);
            p.setStyle(Paint.Style.STROKE);
            canvas.drawLine(0, getMaxPanelHeight(), getWidth(), getMaxPanelHeight(), p);
            p.setColor(Color.BLUE);
            canvas.drawLine(0, getExpandedHeight(), getWidth(), getExpandedHeight(), p);
            p.setColor(Color.GREEN);
            canvas.drawLine(0, calculatePanelHeightQsExpanded(), getWidth(),
                    calculatePanelHeightQsExpanded(), p);
            p.setColor(Color.YELLOW);
            canvas.drawLine(0, calculatePanelHeightShade(), getWidth(),
                    calculatePanelHeightShade(), p);
            p.setColor(Color.MAGENTA);
            canvas.drawLine(0, calculateQsTopPadding(), getWidth(),
                    calculateQsTopPadding(), p);
            p.setColor(Color.CYAN);
            canvas.drawLine(0, mClockPositionResult.stackScrollerPadding, getWidth(),
                    mNotificationStackScroller.getTopPadding(), p);
            p.setColor(Color.GRAY);
            canvas.drawLine(0, mClockPositionResult.clockY, getWidth(),
                    mClockPositionResult.clockY, p);
        }
    }

    @Override
    public void onHeadsUpPinnedModeChanged(final boolean inPinnedMode) {
        mNotificationStackScroller.setInHeadsUpPinnedMode(inPinnedMode);
        if (inPinnedMode) {
            mHeadsUpExistenceChangedRunnable.run();
            updateNotificationTranslucency();
        } else {
            setHeadsUpAnimatingAway(true);
            mNotificationStackScroller.runAfterAnimationFinished(
                    mHeadsUpExistenceChangedRunnable);
        }
        updateGestureExclusionRect();
    }

    public void setHeadsUpAnimatingAway(boolean headsUpAnimatingAway) {
        mHeadsUpAnimatingAway = headsUpAnimatingAway;
        mNotificationStackScroller.setHeadsUpAnimatingAway(headsUpAnimatingAway);
    }

    @Override
    public void onHeadsUpPinned(NotificationEntry entry) {
        mNotificationStackScroller.generateHeadsUpAnimation(entry.getHeadsUpAnimationView(), true);
    }

    @Override
    public void onHeadsUpUnPinned(NotificationEntry entry) {

        // When we're unpinning the notification via active edge they remain heads-upped,
        // we need to make sure that an animation happens in this case, otherwise the notification
        // will stick to the top without any interaction.
        if (isFullyCollapsed() && entry.isRowHeadsUp()) {
            mNotificationStackScroller.generateHeadsUpAnimation(
                    entry.getHeadsUpAnimationView(), false);
            entry.setHeadsUpIsVisible();
        }
    }

    @Override
    public void onHeadsUpStateChanged(NotificationEntry entry, boolean isHeadsUp) {
        mNotificationStackScroller.generateHeadsUpAnimation(entry, isHeadsUp);
    }

    @Override
    public void setHeadsUpManager(HeadsUpManagerPhone headsUpManager) {
        super.setHeadsUpManager(headsUpManager);
        mHeadsUpTouchHelper = new HeadsUpTouchHelper(headsUpManager,
                mNotificationStackScroller.getHeadsUpCallback(), this);
    }

    public void setTrackedHeadsUp(ExpandableNotificationRow pickedChild) {
        if (pickedChild != null) {
            notifyListenersTrackingHeadsUp(pickedChild);
            mExpandingFromHeadsUp = true;
        }
        // otherwise we update the state when the expansion is finished
    }

    @Override
    protected void onClosingFinished() {
        super.onClosingFinished();
        resetHorizontalPanelPosition();
        setClosingWithAlphaFadeout(false);
    }

    private void setClosingWithAlphaFadeout(boolean closing) {
        mClosingWithAlphaFadeOut = closing;
        mNotificationStackScroller.forceNoOverlappingRendering(closing);
    }

    /**
     * Updates the vertical position of the panel so it is positioned closer to the touch
     * responsible for opening the panel.
     *
     * @param x the x-coordinate the touch event
     */
    protected void updateVerticalPanelPosition(float x) {
        if (mNotificationStackScroller.getWidth() * 1.75f > getWidth()) {
            resetHorizontalPanelPosition();
            return;
        }
        float leftMost = mPositionMinSideMargin + mNotificationStackScroller.getWidth() / 2;
        float rightMost = getWidth() - mPositionMinSideMargin
                - mNotificationStackScroller.getWidth() / 2;
        if (Math.abs(x - getWidth() / 2) < mNotificationStackScroller.getWidth() / 4) {
            x = getWidth() / 2;
        }
        x = Math.min(rightMost, Math.max(leftMost, x));
        float center =
                mNotificationStackScroller.getLeft() + mNotificationStackScroller.getWidth() / 2;
        setHorizontalPanelTranslation(x - center);
    }

    private void resetHorizontalPanelPosition() {
        setHorizontalPanelTranslation(0f);
    }

    protected void setHorizontalPanelTranslation(float translation) {
        mNotificationStackScroller.setHorizontalPanelTranslation(translation);
        mQsFrame.setTranslationX(translation);
        int size = mVerticalTranslationListener.size();
        for (int i = 0; i < size; i++) {
            mVerticalTranslationListener.get(i).run();
        }
    }

    protected void updateExpandedHeight(float expandedHeight) {
        if (mTracking) {
            mNotificationStackScroller.setExpandingVelocity(getCurrentExpandVelocity());
        }
        mNotificationStackScroller.setExpandedHeight(expandedHeight);
        updateKeyguardBottomAreaAlpha();
        updateBigClockAlpha();
        updateStatusBarIcons();
    }

    /**
     * @return whether the notifications are displayed full width and don't have any margins on
     *         the side.
     */
    public boolean isFullWidth() {
        return mIsFullWidth;
    }

    private void updateStatusBarIcons() {
        boolean showIconsWhenExpanded = (isPanelVisibleBecauseOfHeadsUp() || isFullWidth())
                && getExpandedHeight() < getOpeningHeight();
        if (showIconsWhenExpanded && mNoVisibleNotifications && isOnKeyguard()) {
            showIconsWhenExpanded = false;
        }
        if (showIconsWhenExpanded != mShowIconsWhenExpanded) {
            mShowIconsWhenExpanded = showIconsWhenExpanded;
            mCommandQueue.recomputeDisableFlags(mDisplayId, false);
        }
    }

    private boolean isOnKeyguard() {
        return mBarState == StatusBarState.KEYGUARD;
    }

    public void setPanelScrimMinFraction(float minFraction) {
        mBar.panelScrimMinFractionChanged(minFraction);
    }

    public void clearNotificationEffects() {
        mStatusBar.clearNotificationEffects();
    }

    @Override
    protected boolean isPanelVisibleBecauseOfHeadsUp() {
        return mHeadsUpManager.hasPinnedHeadsUp() || mHeadsUpAnimatingAway;
    }

    @Override
    public boolean hasOverlappingRendering() {
        return !mDozing;
    }

    public void launchCamera(boolean animate, int source) {
        if (source == StatusBarManager.CAMERA_LAUNCH_SOURCE_POWER_DOUBLE_TAP) {
            mLastCameraLaunchSource = KeyguardBottomAreaView.CAMERA_LAUNCH_SOURCE_POWER_DOUBLE_TAP;
        } else if (source == StatusBarManager.CAMERA_LAUNCH_SOURCE_WIGGLE) {
            mLastCameraLaunchSource = KeyguardBottomAreaView.CAMERA_LAUNCH_SOURCE_WIGGLE;
        } else if (source == StatusBarManager.CAMERA_LAUNCH_SOURCE_LIFT_TRIGGER) {
            mLastCameraLaunchSource = KeyguardBottomAreaView.CAMERA_LAUNCH_SOURCE_LIFT_TRIGGER;
        } else {

            // Default.
            mLastCameraLaunchSource = KeyguardBottomAreaView.CAMERA_LAUNCH_SOURCE_AFFORDANCE;
        }

        // If we are launching it when we are occluded already we don't want it to animate,
        // nor setting these flags, since the occluded state doesn't change anymore, hence it's
        // never reset.
        if (!isFullyCollapsed()) {
            mLaunchingAffordance = true;
            setLaunchingAffordance(true);
        } else {
            animate = false;
        }
        mAffordanceHasPreview = mKeyguardBottomArea.getRightPreview() != null;
        mAffordanceHelper.launchAffordance(animate, getLayoutDirection() == LAYOUT_DIRECTION_RTL);
    }

    public void onAffordanceLaunchEnded() {
        mLaunchingAffordance = false;
        setLaunchingAffordance(false);
    }

    /**
     * Set whether we are currently launching an affordance. This is currently only set when
     * launched via a camera gesture.
     */
    private void setLaunchingAffordance(boolean launchingAffordance) {
        getLeftIcon().setLaunchingAffordance(launchingAffordance);
        getRightIcon().setLaunchingAffordance(launchingAffordance);
        if (mAffordanceLaunchListener != null) {
            mAffordanceLaunchListener.accept(launchingAffordance);
        }
    }

    /**
     * Return true when a bottom affordance is launching an occluded activity with a splash screen.
     */
    public boolean isLaunchingAffordanceWithPreview() {
        return mLaunchingAffordance && mAffordanceHasPreview;
    }

    /**
     * Whether the camera application can be launched for the camera launch gesture.
     *
     * @param keyguardIsShowing whether keyguard is being shown
     */
    public boolean canCameraGestureBeLaunched(boolean keyguardIsShowing) {
        if (!mStatusBar.isCameraAllowedByAdmin()) {
            return false;
        }

        ResolveInfo resolveInfo = mKeyguardBottomArea.resolveCameraIntent();
        String packageToLaunch = (resolveInfo == null || resolveInfo.activityInfo == null)
                ? null : resolveInfo.activityInfo.packageName;
        return packageToLaunch != null &&
                (keyguardIsShowing || !isForegroundApp(packageToLaunch))
                && !mAffordanceHelper.isSwipingInProgress();
    }

    /**
     * Return true if the applications with the package name is running in foreground.
     *
     * @param pkgName application package name.
     */
    private boolean isForegroundApp(String pkgName) {
        ActivityManager am = getContext().getSystemService(ActivityManager.class);
        List<ActivityManager.RunningTaskInfo> tasks = am.getRunningTasks(1);
        return !tasks.isEmpty() && pkgName.equals(tasks.get(0).topActivity.getPackageName());
    }

    private void setGroupManager(NotificationGroupManager groupManager) {
        mGroupManager = groupManager;
    }

    public boolean hideStatusBarIconsWhenExpanded() {
        if (mLaunchingNotification) {
            return mHideIconsDuringNotificationLaunch;
        }
        if (mHeadsUpAppearanceController != null
                && mHeadsUpAppearanceController.shouldBeVisible()) {
            return false;
        }
        return !isFullWidth() || !mShowIconsWhenExpanded;
    }

    private final FragmentListener mFragmentListener = new FragmentListener() {
        @Override
        public void onFragmentViewCreated(String tag, Fragment fragment) {
            mQs = (QS) fragment;
            mQs.setPanelView(NotificationPanelView.this);
            mQs.setExpandClickListener(NotificationPanelView.this);
            mQs.setHeaderClickable(mQsExpansionEnabled);
            mQs.setKeyguardShowing(mKeyguardShowing);
            mQs.setOverscrolling(mStackScrollerOverscrolling);

            // recompute internal state when qspanel height changes
            mQs.getView().addOnLayoutChangeListener(
                    (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
                        final int height = bottom - top;
                        final int oldHeight = oldBottom - oldTop;
                        if (height != oldHeight) {
                            onQsHeightChanged();
                        }
                    });
            mNotificationStackScroller.setQsContainer((ViewGroup) mQs.getView());
            if (mQs instanceof QSFragment) {
                mKeyguardStatusBar.setQSPanel(((QSFragment) mQs).getQsPanel());
            }
            updateQsExpansion();
        }

        @Override
        public void onFragmentViewDestroyed(String tag, Fragment fragment) {
            // Manual handling of fragment lifecycle is only required because this bridges
            // non-fragment and fragment code. Once we are using a fragment for the notification
            // panel, mQs will not need to be null cause it will be tied to the same lifecycle.
            if (fragment == mQs) {
                mQs = null;
            }
        }
    };

    @Override
    public void setTouchAndAnimationDisabled(boolean disabled) {
        super.setTouchAndAnimationDisabled(disabled);
        if (disabled && mAffordanceHelper.isSwipingInProgress() && !mIsLaunchTransitionRunning) {
            mAffordanceHelper.reset(false /* animate */);
        }
        mNotificationStackScroller.setAnimationsEnabled(!disabled);
    }

    /**
     * Sets the dozing state.
     *
     * @param dozing {@code true} when dozing.
     * @param animate if transition should be animated.
     * @param wakeUpTouchLocation touch event location - if woken up by SLPI sensor.
     */
    public void setDozing(boolean dozing, boolean animate, PointF wakeUpTouchLocation) {
        if (dozing == mDozing) return;
        mDozing = dozing;
        mNotificationStackScroller.setDark(mDozing, animate, wakeUpTouchLocation);
        mKeyguardBottomArea.setDozing(mDozing, animate);

        if (dozing) {
            mBottomAreaShadeAlphaAnimator.cancel();
        }

        if (mBarState == StatusBarState.KEYGUARD
                || mBarState == StatusBarState.SHADE_LOCKED) {
            updateDozingVisibilities(animate);
        }

        final float darkAmount = dozing ? 1 : 0;
        mStatusBarStateController.setDozeAmount(darkAmount, animate);
    }

    @Override
    public void onDozeAmountChanged(float linearAmount, float amount) {
        mInterpolatedDarkAmount = amount;
        mLinearDarkAmount = linearAmount;
        mKeyguardStatusView.setDarkAmount(mInterpolatedDarkAmount);
        mKeyguardBottomArea.setDarkAmount(mInterpolatedDarkAmount);
        positionClockAndNotifications();
    }

    public void setPulsing(boolean pulsing) {
        mPulsing = pulsing;
        DozeParameters dozeParameters = DozeParameters.getInstance(mContext);
        final boolean animatePulse = !dozeParameters.getDisplayNeedsBlanking()
                && dozeParameters.getAlwaysOn();
        if (animatePulse) {
            mAnimateNextPositionUpdate = true;
        }
        // Do not animate the clock when waking up from a pulse.
        // The height callback will take care of pushing the clock to the right position.
        if (!mPulsing && !mDozing) {
            mAnimateNextPositionUpdate = false;
        }
        mNotificationStackScroller.setPulsing(pulsing, animatePulse);
        mKeyguardStatusView.setPulsing(pulsing);
    }

    public void setAmbientIndicationBottomPadding(int ambientIndicationBottomPadding) {
        if (mAmbientIndicationBottomPadding != ambientIndicationBottomPadding) {
            mAmbientIndicationBottomPadding = ambientIndicationBottomPadding;
            mStatusBar.updateKeyguardMaxNotifications();
        }
    }

    public void dozeTimeTick() {
        mKeyguardBottomArea.dozeTimeTick();
        mKeyguardStatusView.dozeTimeTick();
        if (mInterpolatedDarkAmount > 0) {
            positionClockAndNotifications();
        }
    }

    public void setStatusAccessibilityImportance(int mode) {
        mKeyguardStatusView.setImportantForAccessibility(mode);
    }

    /**
     * TODO: this should be removed.
     * It's not correct to pass this view forward because other classes will end up adding
     * children to it. Theme will be out of sync.
     *
     * @return bottom area view
     */
    public KeyguardBottomAreaView getKeyguardBottomAreaView() {
        return mKeyguardBottomArea;
    }

    public void setUserSetupComplete(boolean userSetupComplete) {
        mUserSetupComplete = userSetupComplete;
        mKeyguardBottomArea.setUserSetupComplete(userSetupComplete);
    }

    public void applyExpandAnimationParams(ExpandAnimationParameters params) {
        mExpandOffset = params != null ? params.getTopChange() : 0;
        updateQsExpansion();
        if (params != null) {
            boolean hideIcons = params.getProgress(
                    ActivityLaunchAnimator.ANIMATION_DELAY_ICON_FADE_IN, 100) == 0.0f;
            if (hideIcons != mHideIconsDuringNotificationLaunch) {
                mHideIconsDuringNotificationLaunch = hideIcons;
                if (!hideIcons) {
                    mCommandQueue.recomputeDisableFlags(mDisplayId, true /* animate */);
                }
            }
        }
    }

    public void addTrackingHeadsUpListener(Consumer<ExpandableNotificationRow> listener) {
        mTrackingHeadsUpListeners.add(listener);
    }

    public void removeTrackingHeadsUpListener(Consumer<ExpandableNotificationRow> listener) {
        mTrackingHeadsUpListeners.remove(listener);
    }

    public void addVerticalTranslationListener(Runnable verticalTranslationListener) {
        mVerticalTranslationListener.add(verticalTranslationListener);
    }

    public void removeVerticalTranslationListener(Runnable verticalTranslationListener) {
        mVerticalTranslationListener.remove(verticalTranslationListener);
    }

    public void setHeadsUpAppearanceController(
            HeadsUpAppearanceController headsUpAppearanceController) {
        mHeadsUpAppearanceController = headsUpAppearanceController;
    }

    /**
     * Starts the animation before we dismiss Keyguard, i.e. an disappearing animation on the
     * security view of the bouncer.
     */
    public void onBouncerPreHideAnimation() {
        setKeyguardStatusViewVisibility(mBarState, true /* keyguardFadingAway */,
                false /* goingToFullShade */);
    }

    /**
     * Do not let the user drag the shade up and down for the current touch session.
     * This is necessary to avoid shade expansion while/after the bouncer is dismissed.
     */
    public void blockExpansionForCurrentTouch() {
        mBlockingExpansionForCurrentTouch = mTracking;
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        super.dump(fd, pw, args);
        pw.println("    gestureExclusionRect: " + calculateGestureExclusionRect());
        if (mKeyguardStatusBar != null) {
            mKeyguardStatusBar.dump(fd, pw, args);
        }
        if (mKeyguardStatusView != null) {
            mKeyguardStatusView.dump(fd, pw, args);
        }
    }

    public boolean hasActiveClearableNotifications() {
        return mNotificationStackScroller.hasActiveClearableNotifications(ROWS_ALL);
    }

    @Override
    public void onZenChanged(int zen) {
        updateShowEmptyShadeView();
    }

    private void updateShowEmptyShadeView() {
        boolean showEmptyShadeView =
                mBarState != StatusBarState.KEYGUARD &&
                        mEntryManager.getNotificationData().getActiveNotifications().size() == 0;
        showEmptyShadeView(showEmptyShadeView);
    }

    public RemoteInputController.Delegate createRemoteInputDelegate() {
        return mNotificationStackScroller.createDelegate();
    }

    public void updateNotificationViews() {
        mNotificationStackScroller.updateSectionBoundaries();
        mNotificationStackScroller.updateSpeedBumpIndex();
        mNotificationStackScroller.updateFooter();
        updateShowEmptyShadeView();
        mNotificationStackScroller.updateIconAreaViews();
    }

    public void onUpdateRowStates() {
        mNotificationStackScroller.onUpdateRowStates();
    }

    public boolean hasPulsingNotifications() {
        return mNotificationStackScroller.hasPulsingNotifications();
    }

    public boolean isFullyDark() {
        return mNotificationStackScroller.isFullyDark();
    }

    public ActivatableNotificationView getActivatedChild() {
        return mNotificationStackScroller.getActivatedChild();
    }

    public void setActivatedChild(ActivatableNotificationView o) {
        mNotificationStackScroller.setActivatedChild(o);
    }

    public void runAfterAnimationFinished(Runnable r) {
        mNotificationStackScroller.runAfterAnimationFinished(r);
    }

    public void setScrollingEnabled(boolean b) {
        mNotificationStackScroller.setScrollingEnabled(b);
    }

    public void initDependencies(StatusBar statusBar, NotificationGroupManager groupManager,
            NotificationShelf notificationShelf,
            HeadsUpManagerPhone headsUpManager,
            NotificationIconAreaController notificationIconAreaController,
            ScrimController scrimController) {
        setStatusBar(statusBar);
        setGroupManager(mGroupManager);
        mNotificationStackScroller.setNotificationPanel(this);
        mNotificationStackScroller.setIconAreaController(notificationIconAreaController);
        mNotificationStackScroller.setStatusBar(statusBar);
        mNotificationStackScroller.setGroupManager(groupManager);
        mNotificationStackScroller.setHeadsUpManager(headsUpManager);
        mNotificationStackScroller.setShelf(notificationShelf);
        mNotificationStackScroller.setScrimController(scrimController);
        updateShowEmptyShadeView();
    }

    public void showTransientIndication(int id) {
        mKeyguardIndicationController.showTransientIndication(id);
    }

    @Override
    public void onDynamicPrivacyChanged() {
        mAnimateNextPositionUpdate = true;
    }

    /**
     * Panel and QS expansion callbacks.
     */
    public interface PanelExpansionListener {
        /**
         * Invoked whenever the notification panel expansion changes, at every animation frame.
         * This is the main expansion that happens when the user is swiping up to dismiss the
         * lock screen.
         *
         * @param expansion 0 when collapsed, 1 when expanded.
         * @param tracking {@code true} when the user is actively dragging the panel.
         */
        void onPanelExpansionChanged(float expansion, boolean tracking);

        /**
         * Invoked whenever the QS expansion changes, at every animation frame.
         * @param expansion 0 when collapsed, 1 when expanded.
         */
        void onQsExpansionChanged(float expansion);
>>>>>>> 709513c2460f (SystemUI: add quick settings pull down with one finger)
    }
}
