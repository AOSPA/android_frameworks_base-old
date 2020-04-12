package com.android.systemui.aospa;

import android.content.Context;
import android.hardware.face.ParanoidFaceManager;
import android.hardware.face.IFaceService;
import android.util.Log;

import com.android.keyguard.KeyguardUpdateMonitor;
import com.android.keyguard.KeyguardUpdateMonitorCallback;

public class ParanoidFaceManagerFactory extends ParanoidFaceManager {

    private static final String TAG = "ParanoidFaceManager:Factory";

    private Context mContext;

    private final KeyguardUpdateMonitorCallback mKeyguardUpdateMonitorCallback = new KeyguardUpdateMonitorCallback() {
        @Override
        public void onKeyguardVisibilityChanged(boolean showing) {
            Log.d(TAG, "onKeyguardVisibilityChanged: " + showing);
            if (!showing) {
                bind(false);
            }
        }
    };

    public ParanoidFaceManagerFactory(Context context, IFaceService service) {
        super(context, service, null);
        mContext = context;
    }

    public KeyguardUpdateMonitorCallback getMonitorCallback() {
        return mKeyguardUpdateMonitorCallback;
    }

    @Override
    public boolean isHardwareDetected() {
        return KeyguardUpdateMonitor.getInstance(mContext).isPaFaceUnlockServiceEnabled();
    }
}
