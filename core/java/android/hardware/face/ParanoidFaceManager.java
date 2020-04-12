package android.hardware.face;

import android.annotation.SystemService;
import android.content.Context;
import android.hardware.biometrics.CryptoObject;
import android.hardware.face.Face;
import android.hardware.face.FaceManager;
import android.hardware.face.FaceManager.AuthenticationCallback;
import android.hardware.face.FaceManager.AuthenticationResult;
import android.hardware.face.IFaceService;
import android.hardware.face.IParanoidFaceService;
import android.hardware.face.IParanoidFaceServiceReceiver;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Message;
import android.os.RemoteException;
import android.os.Trace;
import android.util.Log;

/**
 * A class that coordinates access to the face authentication hardware.
 * @hide
 */
@SystemService(Context.PARANOID_FACE_SERVICE)
public class ParanoidFaceManager extends FaceManager {

    private static final String TAG = "ParanoidFaceManager";

    public int LOCKOUT_PENALTY_TIME_INTERVAL = 30000;

    private static final int MSG_ACQUIRED = 101;
    private static final int MSG_AUTHENTICATION_SUCCEEDED = 102;
    private static final int MSG_AUTHENTICATION_FAILED = 103;
    private static final int MSG_LOCKOUT = 201;
    private static final int MSG_LOCKOUT_RESET = 202;

    private Context mContext;
    private IParanoidFaceService mService;

    private AuthenticationCallback mAuthenticationCallback;
    private CryptoObject mCryptoObject;
    public int mFaceAuthFailCount = 0;
    public Handler mHandler;
    public boolean mIsLockOut = false;

    private FaceManager.LockoutResetCallback mLockoutResetCallback;
    private IParanoidFaceServiceReceiver mServiceReceiver = new IParanoidFaceServiceReceiver.Stub() {
        @Override
        public void onAcquired(int i) {
            mHandler.obtainMessage(MSG_ACQUIRED, i, 0).sendToTarget();
        }

        @Override
        public void onAuthenticationSucceeded(long j, int i) {
            mHandler.obtainMessage(MSG_AUTHENTICATION_SUCCEEDED, i, 0).sendToTarget();
        }

        @Override
        public void onAuthenticationFailed(int i) {
            if (!mIsLockOut && i > 0) {
                mFaceAuthFailCount = mFaceAuthFailCount + 1;
                Log.d(TAG, "mFaceAuthFailCount: #" + mFaceAuthFailCount);
                if (mFaceAuthFailCount >= 5) {
                    Message obtainMessage = mHandler.obtainMessage(MSG_LOCKOUT);
                    obtainMessage.arg1 = LOCKOUT_PENALTY_TIME_INTERVAL;
                    mHandler.sendMessage(obtainMessage);
                }
            }
            mHandler.obtainMessage(MSG_AUTHENTICATION_FAILED).sendToTarget();
        }
    };

    public boolean hasEnrolledTemplates() {
        return true;
    }

    public boolean hasEnrolledTemplates(int i) {
        return true;
    }

    /**
     * @hide
     */
    public ParanoidFaceManager(Context context, IFaceService service, IParanoidFaceService paService) {
        super(context, service);
        mContext = context;
        mService = paService;
        if (mService == null) {
            Log.d(TAG, "ParanoidFaceService was null");
        }
        mHandler = new ParanoidFaceHandler(context);
    }

    public void bind(boolean shouldBind) {
        if (!shouldBind) {
            Log.d(TAG, "UnBinding paranoid face service");
            try {
                if (mService != null) {
                    mService.unbindFaceService();
                }
            } catch (RemoteException e) {
                throw e.rethrowFromSystemServer();
            }
            return;
        }
            
        Log.d(TAG, "Binding paranoid face service");
        try {
            if (mService != null) {
                mService.bindFaceService(mServiceReceiver);
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void onStrongAuthenticateSuccess() {
        Log.d(TAG, "onStrongAuthenticateSuccess");
        setLockout(0, true);
    }

    /**
     * @hide
     */
    @Override
    public void authenticate(CryptoObject cryptoObject, CancellationSignal signal, int flags, AuthenticationCallback cb, Handler handler, int userId) {
        Log.d(TAG, "Trigger face authenticate");
        if (cb != null) {
            if (signal != null) {
                if (signal.isCanceled()) {
                    Log.d(TAG, "Authentication already canceled");
                    return;
                }
                signal.setOnCancelListener(new OnAuthenticationCancelListener(cryptoObject));
            }
            if (mIsLockOut) {
                cb.onAuthenticationError(7, FaceManager.getErrorString(mContext, 7, 0));
                return;
            }
            bind(true);
            if (mService != null) {
                mAuthenticationCallback = cb;
                mCryptoObject = cryptoObject;
                try {
                    if (mService != null) {
                        mService.setCurrentUserId(userId);
                        mService.startCameraAndDetectFace();
                    }
                } catch (RemoteException e) {
                    throw e.rethrowFromSystemServer();
                }
                return;
            }
            return;
        }
        throw new IllegalArgumentException("Must supply an authentication callback");
    }

    private class OnAuthenticationCancelListener implements CancellationSignal.OnCancelListener {
        private CryptoObject mCrypto;

        OnAuthenticationCancelListener(CryptoObject cryptoObject) {
            mCrypto = cryptoObject;
        }

        public void onCancel() {
            stopAuthentication();
        }
    }

    public void stopAuthentication() {
        try {
            if (mService != null) {
                mService.stopCameraAndDetectFace();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    private class ParanoidFaceHandler extends Handler {
        private ParanoidFaceHandler(Context context) {
            super(context.getMainLooper());
        }

        @Override
        public void handleMessage(Message message) {
            Trace.beginSection("FaceManager#handleMessage: " + Integer.toString(message.what));
            if (message.what == MSG_LOCKOUT) {
                Log.d(TAG, "MSG_LOCKOUT");
                setLockout(message.arg1, false);
            } else if (message.what != MSG_LOCKOUT_RESET) {
                switch (message.what) {
                    case MSG_ACQUIRED:
                        Log.d(TAG, "MSG_ACQUIRED");
                        handleAcquiredResult(message.arg1);
                        break;
                    case MSG_AUTHENTICATION_SUCCEEDED:
                        Log.d(TAG, "MSG_AUTHENTICATION_SUCCEEDED");
                        try {
                            if (mService != null) {
                                mService.setStartStopCamera();
                            }
                        } catch (RemoteException e) {
                            throw e.rethrowFromSystemServer();
                        }
                        handleAuthSucceeded(message.arg1);
                        bind(false);
                        break;
                    case MSG_AUTHENTICATION_FAILED:
                        Log.d(TAG, "MSG_AUTHENTICATION_FAILED");
                        handleAuthFailed();
                        break;
                    default:
                        Log.d(TAG, "Unknown message: " + message.what);
                        break;
                }
            } else {
                Log.d(TAG, "MSG_UNLOCKOUT");
                setLockout(0, true);
            }
            Trace.endSection();
        }
    }

    /**
     * @hide
     */
    @Override
    public void addLockoutResetCallback(FaceManager.LockoutResetCallback lockoutResetCallback) {
        if (mService != null) {
            mLockoutResetCallback = lockoutResetCallback;
        } else {
            Log.d(TAG, "Cannot add lockout reset callback: Service not connected!");
        }
    }

    public boolean isLockout() {
        return mIsLockOut;
    }

    public void setLockout(int messageArg, boolean reset) {
        if (reset) {
            mIsLockOut = false;
            mHandler.removeMessages(MSG_LOCKOUT_RESET);
            if (mLockoutResetCallback != null) {
                mLockoutResetCallback.onLockoutReset();
            }
            Log.d(TAG, "Resettng lockout for face");
            return;
        }
            
        Log.d(TAG, "Enabling lockout for face");
        mIsLockOut = true;
        mFaceAuthFailCount = 0;
        if (mAuthenticationCallback != null) {
            mAuthenticationCallback.onAuthenticationError(7, FaceManager.getErrorString(mContext, 7, 0));
        }
        mHandler.removeMessages(MSG_LOCKOUT_RESET);
        mHandler.sendEmptyMessageDelayed(MSG_LOCKOUT_RESET, (long) messageArg);
    }

    public void handleAcquiredResult(int acquireInfo) {
        Log.d(TAG, "Sending acquired result " + acquireInfo);
        if (mAuthenticationCallback != null) {
            mAuthenticationCallback.onAuthenticationAcquired(acquireInfo);
        }
    }

    public void handleAuthSucceeded(int userId) {
        if (mAuthenticationCallback != null) {
            mAuthenticationCallback.onAuthenticationSucceeded(new AuthenticationResult(mCryptoObject, (Face) null, userId));
        }
        stopAuthentication();
    }

    public void handleAuthFailed() {
        if (mAuthenticationCallback != null) {
            mAuthenticationCallback.onAuthenticationFailed();
        }
        stopAuthentication();
    }

    public void setStartStopCamera() {
        try {
            if (mService != null) {
                mService.setStartStopCamera();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean getCameraStopCompleted() {
        try {
            if (mService != null) {
                return mService.getCameraStopCompleted();
            }
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
        return false;
    }
}
