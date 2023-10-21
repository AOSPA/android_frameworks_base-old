/*
 * Copyright (C) 2020 The Android Open Source Project
 * Copyright (C) 2023 Paranoid Android
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

package com.android.server.biometrics.sensors.face.sense;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.content.Context;
import android.hardware.biometrics.BiometricFaceConstants;
import android.hardware.biometrics.common.ICancellationSignal;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Slog;
import android.view.Surface;

import com.android.internal.R;
import com.android.server.biometrics.Utils;
import com.android.server.biometrics.log.BiometricContext;
import com.android.server.biometrics.log.BiometricLogger;
import com.android.server.biometrics.sensors.BiometricNotificationUtils;
import com.android.server.biometrics.sensors.BiometricUtils;
import com.android.server.biometrics.sensors.ClientMonitorCallback;
import com.android.server.biometrics.sensors.ClientMonitorCallbackConverter;
import com.android.server.biometrics.sensors.ClientMonitorCompositeCallback;
import com.android.server.biometrics.sensors.EnrollClient;
import com.android.server.biometrics.sensors.face.FaceUtils;

import java.util.ArrayList;
import java.util.function.Supplier;

import vendor.aospa.biometrics.face.ISenseService;

/**
 * Face-specific enroll client for the {@link ISenseService} AIDL HAL interface.
 */
public class FaceEnrollClient extends EnrollClient<ISenseService> {

    private static final String TAG = "FaceEnrollClient";

    @NonNull private final int[] mEnrollIgnoreList;
    @NonNull private final int[] mEnrollIgnoreListVendor;
    @NonNull private final int[] mDisabledFeatures;
    @Nullable private ICancellationSignal mCancellationSignal;
    private final int mMaxTemplatesPerUser;

    FaceEnrollClient(@NonNull Context context, @NonNull Supplier<ISenseService> lazyDaemon,
            @NonNull IBinder token, @NonNull ClientMonitorCallbackConverter listener, int userId,
            @NonNull byte[] hardwareAuthToken, @NonNull String opPackageName, long requestId,
            @NonNull BiometricUtils<Face> utils, @NonNull int[] disabledFeatures, int timeoutSec,
            @Nullable Surface previewSurface, int sensorId,
            @NonNull BiometricLogger logger, @NonNull BiometricContext biometricContext,
            int maxTemplatesPerUser, boolean debugConsent) {
        super(context, lazyDaemon, token, listener, userId, hardwareAuthToken, opPackageName, utils,
                timeoutSec, sensorId, false /* shouldVibrate */, logger, biometricContext);
        setRequestId(requestId);
        mEnrollIgnoreList = getContext().getResources()
                .getIntArray(R.array.config_face_acquire_enroll_ignorelist);
        mEnrollIgnoreListVendor = getContext().getResources()
                .getIntArray(R.array.config_face_acquire_vendor_enroll_ignorelist);
        mMaxTemplatesPerUser = maxTemplatesPerUser;
        mDisabledFeatures = disabledFeatures;
    }

    @Override
    public void start(@NonNull ClientMonitorCallback callback) {
        super.start(callback);

        BiometricNotificationUtils.cancelReEnrollNotification(getContext());
    }

    @NonNull
    @Override
    protected ClientMonitorCallback wrapCallbackForStart(@NonNull ClientMonitorCallback callback) {
        return new ClientMonitorCompositeCallback(
                getLogger().getAmbientLightProbe(true /* startWithClient */), callback);
    }

    @Override
    protected boolean hasReachedEnrollmentLimit() {
        return FaceUtils.getInstance(getSensorId()).getBiometricsForUser(getContext(),
                getTargetUserId()).size() >= mMaxTemplatesPerUser;
    }

    private boolean shouldSendAcquiredMessage(int acquireInfo, int vendorCode) {
        return acquireInfo == FaceManager.FACE_ACQUIRED_VENDOR
                ? !Utils.listContains(mEnrollIgnoreListVendor, vendorCode)
                : !Utils.listContains(mEnrollIgnoreList, acquireInfo);
    }

    @Override
    public void onAcquired(int acquireInfo, int vendorCode) {
        final boolean shouldSend = shouldSendAcquiredMessage(acquireInfo, vendorCode);
        onAcquiredInternal(acquireInfo, vendorCode, shouldSend);
    }

    /**
     * Called each time a new frame is received during face enrollment.
     *
     * @param frame Information about the current frame.
     */
    public void onEnrollmentFrame(@NonNull FaceEnrollFrame frame) {
        // Log acquisition but don't send it to the client yet, since that's handled below.
        final int acquireInfo = frame.getData().getAcquiredInfo();
        final int vendorCode = frame.getData().getVendorCode();
        onAcquiredInternal(acquireInfo, vendorCode, false /* shouldSend */);

        final boolean shouldSend = shouldSendAcquiredMessage(acquireInfo, vendorCode);
        if (shouldSend && getListener() != null) {
            try {
                getListener().onEnrollmentFrame(frame);
            } catch (RemoteException e) {
                Slog.w(TAG, "Failed to send enrollment frame", e);
                mCallback.onClientFinished(this, false /* success */);
            }
        }
    }

    @Override
    protected void startHalOperation() {
        try {
            final ArrayList<Byte> token = new ArrayList<>();
            for (byte b : mHardwareAuthToken) {
                token.add(Byte.valueOf(b));
            }

            final ArrayList<Integer> disabledFeatures = new ArrayList<>();
            for (int disabledFeature : mDisabledFeatures) {
                disabledFeatures.add(disabledFeature);
            }

            mCancellationSignal = doEnroll(token, disabledFeatures);
        } catch (RemoteException | IllegalArgumentException e) {
            Slog.e(TAG, "Exception when requesting enroll", e);
            onError(BiometricFaceConstants.FACE_ERROR_UNABLE_TO_PROCESS, 0 /* vendorCode */);
            mCallback.onClientFinished(this, false /* success */);
        }
    }

    private ICancellationSignal doEnroll(ArrayList<Byte> token, ArrayList<Integer> disabledFeatures) throws RemoteException {
        final ISenseService session = getFreshDaemon();

        return session.enroll(SenseUtils.toByteArray(token), mTimeoutSec, SenseUtils.toIntArray(disabledFeatures));
    }

    @Override
    protected void stopHalOperation() {
        unsubscribeBiometricContext();

        if (mCancellationSignal != null) {
            try {
                mCancellationSignal.cancel();
            } catch (RemoteException e) {
                Slog.e(TAG, "Remote exception when requesting cancel", e);
                onError(BiometricFaceConstants.FACE_ERROR_HW_UNAVAILABLE, 0 /* vendorCode */);
                mCallback.onClientFinished(this, false /* success */);
            }
        }
    }
}
