/*
 * Copyright (C) 2021 The Android Open Source Project
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

import static android.view.Display.DEFAULT_DISPLAY;

import static com.android.systemui.flags.Flags.ONE_WAY_HAPTICS_API_MIGRATION;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

import android.app.ActivityManager;
import android.app.StatusBarManager;
import android.os.PowerManager;
import android.os.UserHandle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.testing.AndroidTestingRunner;
import android.view.HapticFeedbackConstants;
import android.view.WindowInsets;

import androidx.test.filters.SmallTest;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.testing.FakeMetricsLogger;
import com.android.internal.statusbar.LetterboxDetails;
import com.android.internal.view.AppearanceRegion;
import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.systemui.SysuiTestCase;
import com.android.systemui.assist.AssistManager;
import com.android.systemui.flags.FakeFeatureFlags;
import com.android.systemui.keyguard.WakefulnessLifecycle;
import com.android.systemui.plugins.ActivityStarter;
import com.android.systemui.qs.QSHost;
import com.android.systemui.settings.UserTracker;
import com.android.systemui.shade.CameraLauncher;
import com.android.systemui.shade.QuickSettingsController;
import com.android.systemui.shade.ShadeController;
import com.android.systemui.shade.ShadeViewController;
import com.android.systemui.statusbar.CommandQueue;
import com.android.systemui.statusbar.VibratorHelper;
import com.android.systemui.statusbar.disableflags.DisableFlagsLogger;
import com.android.systemui.statusbar.notification.stack.NotificationStackScrollLayoutController;
import com.android.systemui.statusbar.policy.DeviceProvisionedController;
import com.android.systemui.statusbar.policy.FlashlightController;
import com.android.systemui.statusbar.policy.KeyguardStateController;
import com.android.systemui.statusbar.policy.RemoteInputQuickSettingsDisabler;

import dagger.Lazy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.Optional;

@SmallTest
@RunWith(AndroidTestingRunner.class)
public class CentralSurfacesCommandQueueCallbacksTest extends SysuiTestCase {

    @Mock private CentralSurfaces mCentralSurfaces;
    @Mock private ShadeController mShadeController;
    @Mock private CommandQueue mCommandQueue;
    @Mock private QuickSettingsController mQuickSettingsController;
    @Mock private ShadeViewController mShadeViewController;
    @Mock private RemoteInputQuickSettingsDisabler mRemoteInputQuickSettingsDisabler;
    private final MetricsLogger mMetricsLogger = new FakeMetricsLogger();
    @Mock private KeyguardUpdateMonitor mKeyguardUpdateMonitor;
    @Mock private KeyguardStateController mKeyguardStateController;
    @Mock private HeadsUpManagerPhone mHeadsUpManager;
    @Mock private WakefulnessLifecycle mWakefulnessLifecycle;
    @Mock private DeviceProvisionedController mDeviceProvisionedController;
    @Mock private StatusBarKeyguardViewManager mStatusBarKeyguardViewManager;
    @Mock private AssistManager mAssistManager;
    @Mock private DozeServiceHost mDozeServiceHost;
    @Mock private NotificationStackScrollLayoutController mNotificationStackScrollLayoutController;
    @Mock private PowerManager mPowerManager;
    @Mock private VibratorHelper mVibratorHelper;
    @Mock private Vibrator mVibrator;
    @Mock private StatusBarHideIconsForBouncerManager mStatusBarHideIconsForBouncerManager;
    @Mock private SystemBarAttributesListener mSystemBarAttributesListener;
    @Mock private Lazy<CameraLauncher> mCameraLauncherLazy;
    @Mock private UserTracker mUserTracker;
    @Mock private QSHost mQSHost;
    @Mock private ActivityStarter mActivityStarter;
    @Mock private FlashlightController mFlashlightController;
    private final FakeFeatureFlags mFeatureFlags = new FakeFeatureFlags();

    CentralSurfacesCommandQueueCallbacks mSbcqCallbacks;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);

        mSbcqCallbacks = new CentralSurfacesCommandQueueCallbacks(
                mCentralSurfaces,
                mQuickSettingsController,
                mContext,
                mContext.getResources(),
                mShadeController,
                mCommandQueue,
                mShadeViewController,
                mRemoteInputQuickSettingsDisabler,
                mMetricsLogger,
                mKeyguardUpdateMonitor,
                mKeyguardStateController,
                mHeadsUpManager,
                mWakefulnessLifecycle,
                mDeviceProvisionedController,
                mStatusBarKeyguardViewManager,
                mAssistManager,
                mDozeServiceHost,
                mNotificationStackScrollLayoutController,
                mStatusBarHideIconsForBouncerManager,
                mPowerManager,
                mVibratorHelper,
                Optional.of(mVibrator),
                new DisableFlagsLogger(),
                DEFAULT_DISPLAY,
                mSystemBarAttributesListener,
                mCameraLauncherLazy,
                mUserTracker,
                mQSHost,
                mActivityStarter,
                mFlashlightController,
                mFeatureFlags);

        when(mUserTracker.getUserHandle()).thenReturn(
                UserHandle.of(ActivityManager.getCurrentUser()));
        when(mDeviceProvisionedController.isCurrentUserSetup()).thenReturn(true);
        when(mRemoteInputQuickSettingsDisabler.adjustDisableFlags(anyInt()))
                .thenAnswer((Answer<Integer>) invocation -> invocation.getArgument(0));
    }

    @Test
    public void testDisableNotificationShade() {
        // Start with nothing disabled
        mSbcqCallbacks.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_NONE,
                StatusBarManager.DISABLE2_NONE, false);

        when(mCommandQueue.panelsEnabled()).thenReturn(false);
        // WHEN the new disable flags have the shade disabled
        mSbcqCallbacks.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_NONE,
                StatusBarManager.DISABLE2_NOTIFICATION_SHADE, false);

        // THEN the shade is collapsed
        verify(mShadeController).animateCollapseShade();
    }

    @Test
    public void testEnableNotificationShade() {
        // Start with the shade disabled
        mSbcqCallbacks.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_NONE,
                StatusBarManager.DISABLE2_NOTIFICATION_SHADE, false);
        reset(mShadeController);

        when(mCommandQueue.panelsEnabled()).thenReturn(true);
        // WHEN the new disable flags have the shade enabled
        mSbcqCallbacks.disable(DEFAULT_DISPLAY, StatusBarManager.DISABLE_NONE,
                StatusBarManager.DISABLE2_NONE, false);

        // THEN the shade is not collapsed
        verify(mShadeController, never()).animateCollapseShade();
    }

    @Test
    public void testSuppressAmbientDisplay_suppress() {
        mSbcqCallbacks.suppressAmbientDisplay(true);
        verify(mDozeServiceHost).setAlwaysOnSuppressed(true);
    }

    @Test
    public void testSuppressAmbientDisplay_unsuppress() {
        mSbcqCallbacks.suppressAmbientDisplay(false);
        verify(mDozeServiceHost).setAlwaysOnSuppressed(false);
    }

    @Test
    public void onSystemBarAttributesChanged_forwardsToSysBarAttrsListener() {
        int displayId = DEFAULT_DISPLAY;
        int appearance = 123;
        AppearanceRegion[] appearanceRegions = new AppearanceRegion[]{};
        boolean navbarColorManagedByIme = true;
        int behavior = 456;
        int requestedVisibleTypes = WindowInsets.Type.systemBars();
        String packageName = "test package name";
        LetterboxDetails[] letterboxDetails = new LetterboxDetails[]{};

        mSbcqCallbacks.onSystemBarAttributesChanged(
                displayId,
                appearance,
                appearanceRegions,
                navbarColorManagedByIme,
                behavior,
                requestedVisibleTypes,
                packageName,
                letterboxDetails);

        verify(mSystemBarAttributesListener).onSystemBarAttributesChanged(
                displayId,
                appearance,
                appearanceRegions,
                navbarColorManagedByIme,
                behavior,
                requestedVisibleTypes,
                packageName,
                letterboxDetails
        );
    }

    @Test
    public void onSystemBarAttributesChanged_differentDisplayId_doesNotForwardToAttrsListener() {
        int appearance = 123;
        AppearanceRegion[] appearanceRegions = new AppearanceRegion[]{};
        boolean navbarColorManagedByIme = true;
        int behavior = 456;
        int requestedVisibleTypes = WindowInsets.Type.systemBars();
        String packageName = "test package name";
        LetterboxDetails[] letterboxDetails = new LetterboxDetails[]{};

        mSbcqCallbacks.onSystemBarAttributesChanged(
                DEFAULT_DISPLAY + 1,
                appearance,
                appearanceRegions,
                navbarColorManagedByIme,
                behavior,
                requestedVisibleTypes,
                packageName,
                letterboxDetails);

        verifyZeroInteractions(mSystemBarAttributesListener);
    }

    @Test
    public void vibrateOnNavigationKeyDown_oneWayHapticsDisabled_usesVibrate() {
        mFeatureFlags.set(ONE_WAY_HAPTICS_API_MIGRATION, false);

        mSbcqCallbacks.vibrateOnNavigationKeyDown();

        verify(mVibratorHelper).vibrate(VibrationEffect.EFFECT_TICK);
    }

    @Test
    public void vibrateOnNavigationKeyDown_oneWayHapticsEnabled_usesPerformHapticFeedback() {
        mFeatureFlags.set(ONE_WAY_HAPTICS_API_MIGRATION, true);

        mSbcqCallbacks.vibrateOnNavigationKeyDown();

        verify(mShadeViewController).performHapticFeedback(
                HapticFeedbackConstants.GESTURE_START
        );
    }
}
