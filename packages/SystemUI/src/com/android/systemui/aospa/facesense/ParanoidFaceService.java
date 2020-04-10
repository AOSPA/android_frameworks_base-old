package com.android.systemui.aospa.facesense;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import vendor.pa.biometrics.face.V1_0.IFaceServiceReceiver;

public class ParanoidFaceService {

    protected static final String TAG = "ParanoidFaceService";

    private static final int MSG_START_CAMERA_DETECT_FACE = 1;
    private static final int MSG_FACEUNLOCK_TIME_UP = 2;
    private static final int MSG_STOP_CAMERA_DETECT_FACE = 3;
    private static final int MSG_STOP_FACEUNLOCK_SERVICE = 4;
    private static final int MSG_START_FACEUNLOCK_SERVICE = 5;

    protected static FaceServiceWrapper sFaceServiceWrapper = null;
    protected static long sStartTime;
    protected final Object mAuthLock = new Object();
    protected Looper mAuthLooper;
    protected Context mContext;
    protected IFaceServiceReceiver mFaceServiceReceiver;

    protected static boolean sIsAssumeWarningAsAttack = false;
    protected int mCurrentUserId = -10000;

    protected final Object mFuLock = new Object();
    protected Looper mFuLooper;

    protected boolean mIsAuthenticated = false;
    protected boolean mIsCameraStopCompleted = true;
    protected int sCompareFailCount = 0;

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
                sFaceServiceWrapper.endAuthenticate();
                handleStopCamera();
                mIsCameraStopCompleted = true;
            } else if (message.what == MSG_STOP_FACEUNLOCK_SERVICE) {
                Log.d(TAG, "handleMessage MSG_STOP_FACEUNLOCK_SERVICE");
                if (sFaceServiceWrapper != null) {
                    sFaceServiceWrapper.unbindFaceUnlockService();
                }
            } else if (message.what == MSG_START_FACEUNLOCK_SERVICE) {
                Log.d(TAG, "handleMessage MSG_START_FACEUNLOCK_SERVICE");
                if (sFaceServiceWrapper != null) {
                    sFaceServiceWrapper.bindFaceUnlockService(new FaceServiceListener() {
                        @Override
                        public void onFaceServiceConnected() {
                            // no op
                        }

                        public void onFaceServiceDisconnected() {
                            Log.d(TAG, "onFaceServiceDisconnected");
                            stopCameraAndDetectFace();
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

    public ParanoidFaceService(Context context, IFaceServiceReceiver receiver) {
        sFaceServiceWrapper = FaceServiceWrapper.getInstance(context);
        mContext = context;
        mFaceServiceReceiver = receiver;
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

    public void setCurrentUserId(int userId) {
        mCurrentUserId = userId;
    }

    public void startCameraAndDetectFace() {
        if (isSystemUIHoldCamera()) {
            Log.d(TAG, "Camera component is already exist, return from startCameraAndDetectFace");
            return;
        }
        mFaceUnlockHandler.removeMessages(MSG_FACEUNLOCK_TIME_UP);
        mFaceUnlockHandler.removeMessages(MSG_STOP_CAMERA_DETECT_FACE);
        mFaceUnlockHandler.removeMessages(MSG_START_CAMERA_DETECT_FACE);
        checkCameraAvailableAndStart();
    }

    public void stopCameraAndDetectFace() {
        mFaceUnlockHandler.removeMessages(MSG_FACEUNLOCK_TIME_UP);
        mFaceUnlockHandler.removeMessages(MSG_START_CAMERA_DETECT_FACE);
        mFaceUnlockHandler.removeMessages(MSG_STOP_CAMERA_DETECT_FACE);
        mFaceUnlockHandler.sendEmptyMessage(MSG_STOP_CAMERA_DETECT_FACE);
    }

    public void bindFaceUnlockService() {
        Log.d(TAG, "bindFaceUnlockService");
        mFaceUnlockHandler.removeMessages(MSG_START_FACEUNLOCK_SERVICE);
        mFaceUnlockHandler.removeMessages(MSG_STOP_FACEUNLOCK_SERVICE);
        mFaceUnlockHandler.sendEmptyMessage(MSG_START_FACEUNLOCK_SERVICE);
    }

    public void unbindFaceUnlockService() {
        Log.d(TAG, "unbindFaceUnlockService");
        mFaceUnlockHandler.removeMessages(MSG_STOP_FACEUNLOCK_SERVICE);
        mFaceUnlockHandler.sendEmptyMessageDelayed(MSG_STOP_FACEUNLOCK_SERVICE, 3000);
    }

    public void resetCompareFailCount() {
        Log.d(TAG, "Reset CompareFailCount");
        sCompareFailCount = 0;
    }

    public void setStartStopCamera() {
        mIsCameraStopCompleted = false;
    }

    public boolean getCameraStopCompleted() {
        return mIsCameraStopCompleted;
    }
}
