package com.android.systemui.aospa.facesense;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import vendor.pa.biometrics.face.V1_0.IFaceUnlockService;

public class FaceServiceWrapper {

    private static FaceServiceWrapper sInstance;
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            synchronized (mLock) {
                Log.d("FaceServiceWrapper", "mConnection onServiceConnected");
                mService = IFaceUnlockService.Stub.asInterface(iBinder);
                if (mListener != null) {
                    mListener.onFaceServiceConnected();
                }
            }
        }

        public void onServiceDisconnected(ComponentName componentName) {
            synchronized (mLock) {
                Log.d("FaceServiceWrapper", "mConnection onServiceDisconnected");
                mService = null;
                unbindFaceUnlockService();
            }
        }
    };

    private Context mContext;
    public FaceServiceListener mListener;
    public IFaceUnlockService mService;
    public final Object mLock = new Object();

    private boolean mIsBind = false;

    private FaceServiceWrapper(Context context) {
        if (context != null) {
            mContext = context;
        }
    }

    public static FaceServiceWrapper getInstance(Context context) {
        if (sInstance == null && context != null) {
            sInstance = new FaceServiceWrapper(context);
        }
        return sInstance;
    }

    public void bindFaceUnlockService(FaceServiceListener listener) {
        synchronized (mLock) {
            mListener = listener;
            Log.d("FaceServiceWrapper", "bindFaceUnlockService: " + mIsBind + ", service: " + mService);
            if (!mIsBind) {
                Intent intent = new Intent();
                intent.setAction("service.remote");
                intent.setPackage("com.paranoid.facesense");
                if (mConnection != null) {
                    if (mContext.bindService(intent, mConnection, 1)) {
                        mIsBind = true;
                        Log.d("FaceServiceWrapper", "FaceService bound");
                    }
                }
                Log.d("FaceServiceWrapper", "FAILED TO BIND TO FService!");
            }
        }
    }

    public void unbindFaceUnlockService() {
        synchronized (mLock) {
            Log.d("FaceServiceWrapper", "unbindFaceUnlockService");
            if (!(!mIsBind || mContext == null || mConnection == null)) {
                mIsBind = false;
                mContext.unbindService(mConnection);
                if (mListener != null) {
                    mListener.onFaceServiceDisconnected();
                }
                Log.d("FaceServiceWrapper", "FaceService unbound");
            }
            mService = null;
        }
    }

    public int authenticate(byte[] token, int i, int i2) {
        int authenticate;
        synchronized (mLock) {
            authenticate = -1001;
            try {
                if (!mIsBind) {
                    Log.d("FaceServiceWrapper", "authenticate: still waiting for connection");
                } else if (mService != null) {
                    authenticate = mService.authenticate(token, i, i2);
                } else {
                    Log.d("FaceServiceWrapper", "authenticate: service is not connected, cannot authenticate with face.");
                }
            } catch (RemoteException e) {
                Log.d("FaceServiceWrapper", "RemoteException", e);
            }
        }
        return authenticate;
    }

    public void endAuthenticate() {
        synchronized (mLock) {
            if (mIsBind) {
                if (mService != null) {
                    try {
                        mService.endAuthenticate();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
