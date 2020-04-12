/*
 * Copyright (C) 2020 Paranoid Android
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
 * limitations under the License
 */

package com.android.server.biometrics.face;

import android.content.Context;
import android.hardware.face.IParanoidFaceServiceReceiver;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.android.server.SystemService;

public class ParanoidFaceServiceBase extends SystemService {

    protected static final String TAG = "ParanoidFaceServiceBase";

    protected static final int MSG_START_CAMERA_DETECT_FACE = 1;
    protected static final int MSG_FACEUNLOCK_TIME_UP = 2;
    protected static final int MSG_STOP_CAMERA_DETECT_FACE = 3;
    protected static final int MSG_STOP_FACEUNLOCK_SERVICE = 4;
    protected static final int MSG_START_FACEUNLOCK_SERVICE = 5;

    protected static SenseWrapper mSenseWrapper = null;
    protected static long sStartTime;
    protected final Object mAuthLock = new Object();
    protected Looper mAuthLooper;
    protected Context mContext;
    protected IParanoidFaceServiceReceiver mFaceServiceReceiver;

    protected static boolean sIsAssumeWarningAsAttack = false;
    protected int mCurrentUserId = -10000;

    protected final Object mFuLock = new Object();
    protected Looper mFuLooper;

    protected boolean mIsAuthenticated = false;
    protected boolean mIsCameraStopCompleted = true;
    protected int sCompareFailCount = 0;

    interface FaceServiceListener {
        void onFaceServiceConnected();
        void onFaceServiceDisconnected();
    }

    protected Handler mFaceUnlockHandler = new Handler(getFaceUnlockHandler()) {
        @Override
        public void handleMessage(Message message) {
            int i = message.what;
            if (message.what == MSG_START_CAMERA_DETECT_FACE) {
                Log.d(TAG, "handleMessage MSG_START_CAMERA_DETECT_FACE");
                boolean handleStartCamera = handleStartCamera();
                Log.d(TAG, "MSG_START_CAMERA_DETECT_FACE: handleStartCamera started: " + handleStartCamera);
                if (!handleStartCamera) {
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            boolean handleStartCamera = handleStartCamera();
                            Log.d(TAG, "MSG_START_CAMERA_DETECT_FACE: handleStartCamera (retry) started: " + handleStartCamera);
                            if (handleStartCamera) {
                                mIsAuthenticated = false;
                                setTimeoutFail(5000);
                                return;
                            }
                            Log.d(TAG, "MSG_START_CAMERA_DETECT_FACE: handleStartCamera (retry) still fail, call timeOutFail Now");
                            setTimeoutFail(0);
                        }
                    }, 200);
                    return;
                }
                mIsAuthenticated = false;
                setTimeoutFail(5000);
            } else if (message.what == MSG_FACEUNLOCK_TIME_UP) {
                Log.d(TAG, "handleMessage MSG_FACEUNLOCK_TIME_UP");
                try {
                    mFaceServiceReceiver.onAuthenticationFailed(sCompareFailCount);
                } catch (RemoteException e) {
                    Log.d(TAG, "Fail to attemp onAuthenticationFailed ", e);
                }
                resetCompareFailCount();
            } else if (message.what == MSG_STOP_CAMERA_DETECT_FACE) {
                Log.d(TAG, "handleMessage MSG_STOP_CAMERA_DETECT_FACE");
                mSenseWrapper.endAuthenticate();
                handleStopCamera();
                mIsCameraStopCompleted = true;
            } else if (message.what == MSG_STOP_FACEUNLOCK_SERVICE) {
                Log.d(TAG, "handleMessage MSG_STOP_FACEUNLOCK_SERVICE");
                if (mSenseWrapper != null) {
                    mSenseWrapper.unbindFaceUnlockService();
                }
            } else if (message.what == MSG_START_FACEUNLOCK_SERVICE) {
                Log.d(TAG, "handleMessage MSG_START_FACEUNLOCK_SERVICE");
                if (mSenseWrapper != null) {
                    mSenseWrapper.bindFaceUnlockService(new FaceServiceListener() {
                        @Override
                        public void onFaceServiceConnected() {
                            // no op
                        }

                        public void onFaceServiceDisconnected() {
                            Log.d(TAG, "onFaceServiceDisconnected");
                            stopCameraAndDetectFaceInternal();
                        }
                    });
                }
            }
        }
    };

    public void checkCameraAvailableAndStart() {
        throw null;
    }

    public boolean handleStartCamera() {
        throw null;
    }

    public void handleStopCamera() {
        throw null;
    }

    public boolean isSystemUIHoldCamera() {
        throw null;
    }

    public ParanoidFaceServiceBase(Context context) {
        super(context);
        mSenseWrapper = SenseWrapper.getInstance(context);
        mContext = context;
    }

    @Override
    public void onStart() {
        // Handled in derived service
    }

    public Looper getAuthHandler() {
        if (mAuthLooper == null) {
            synchronized (mAuthLock) {
                if (mAuthLooper == null) {
                    HandlerThread authThread = new HandlerThread("FaceUnlockAuthThread");
                    authThread.start();
                    mAuthLooper = authThread.getLooper();
                }
            }
        }
        return mAuthLooper;
    }

    public void setTimeoutFail(int i) {
        if (!mFaceUnlockHandler.hasMessages(MSG_FACEUNLOCK_TIME_UP)) {
            Log.d(TAG, "setTimeoutFail: " + i);
            mFaceUnlockHandler.sendEmptyMessageDelayed(MSG_FACEUNLOCK_TIME_UP, (long) i);
        }
    }

    public void resetCompareFailCount() {
        Log.d(TAG, "Reset CompareFailCount");
        sCompareFailCount = 0;
    }

    public Looper getFaceUnlockHandler() {
        if (mFuLooper == null) {
            synchronized (mFuLock) {
                if (mFuLooper == null) {
                    HandlerThread handlerThread = new HandlerThread("FaceUnlockThread");
                    handlerThread.start();
                    mFuLooper = handlerThread.getLooper();
                }
            }
        }
        return mFuLooper;
    }

    protected void setCurrentUserIdInternal(int userId) {
        mCurrentUserId = userId;
    }

    protected void startCameraAndDetectFaceInternal() {
        if (isSystemUIHoldCamera()) {
            Log.d(TAG, "Camera component is already exist, return from startCameraAndDetectFace");
            return;
        }
        mFaceUnlockHandler.removeMessages(MSG_FACEUNLOCK_TIME_UP);
        mFaceUnlockHandler.removeMessages(MSG_STOP_CAMERA_DETECT_FACE);
        mFaceUnlockHandler.removeMessages(MSG_START_CAMERA_DETECT_FACE);
        checkCameraAvailableAndStart();
    }

    protected void stopCameraAndDetectFaceInternal() {
        mFaceUnlockHandler.removeMessages(MSG_FACEUNLOCK_TIME_UP);
        mFaceUnlockHandler.removeMessages(MSG_START_CAMERA_DETECT_FACE);
        mFaceUnlockHandler.removeMessages(MSG_STOP_CAMERA_DETECT_FACE);
        mFaceUnlockHandler.sendEmptyMessage(MSG_STOP_CAMERA_DETECT_FACE);
    }

    protected void bindFaceServiceInternal() {
        Log.d(TAG, "bindFaceUnlockService");
        mFaceUnlockHandler.removeMessages(MSG_START_FACEUNLOCK_SERVICE);
        mFaceUnlockHandler.removeMessages(MSG_STOP_FACEUNLOCK_SERVICE);
        mFaceUnlockHandler.sendEmptyMessage(MSG_START_FACEUNLOCK_SERVICE);
    }

    protected void unbindFaceServiceInternal() {
        Log.d(TAG, "unbindFaceUnlockService");
        mFaceUnlockHandler.removeMessages(MSG_STOP_FACEUNLOCK_SERVICE);
        mFaceUnlockHandler.sendEmptyMessageDelayed(MSG_STOP_FACEUNLOCK_SERVICE, 3000);
    }

    protected void setStartStopCameraInternal() {
        mIsCameraStopCompleted = false;
    }

    protected boolean getCameraStopCompletedInternal() {
        return mIsCameraStopCompleted;
    }
}
