package com.android.server.pocket;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.pocket.IPocketServiceCallback;
import android.util.Log;
import android.util.Slog;

import com.android.server.SystemService;

import java.util.ArrayList;

/** @hide */
public class PocketService extends SystemService implements IBinder.DeathRecipient {

    private static final String TAG = PocketService.class.getSimpleName();
    private static final boolean DEBUG = true;

    // TODO> implement companion light sensor logic.
    private static final boolean ENABLE_LIGHT_SENSOR = false;

    /**
     * We don't have a valid proximity sensor event yet.
     * @author Carlo Savignano
     */
    private static final int PROXIMITY_UNKNOWN = 0;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is covered.
     * @author Carlo Savignano
     */
    private static final int PROXIMITY_NEAR = 1;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is not covered.
     * @author Carlo Savignano
     */
    private static final int PROXIMITY_FAR = 2;

    // TODO> javadoc
    private static final int LIGHT_UNKNOWN = 0;

    // TODO> javadoc
    private static final int LIGHT_ABSENT = 1;

    // TODO> javadoc
    private static final int LIGHT_LOW = 2;

    // TODO> javadoc
    private static final int LIGHT_NORMAL = 3;

    // TODO> javadoc
    private static final int LIGHT_HIGH = 4;

//    private final ArrayList<PocketServiceMonitor> mMonitors = new ArrayList<>();
    private final ArrayList<IPocketServiceCallback> mCallbacks= new ArrayList<>();

    private Context mContext;
    private boolean mSystemReady;
    private boolean mDeviceInteractive;
    private PocketHandler mHandler;
    private SensorManager mSensorManager;

    // proximity
    private int mProximityState = PROXIMITY_UNKNOWN;
    private int mLastProximityState = PROXIMITY_UNKNOWN;
    private float mProximityMaxRange;
    private boolean mProximityRegistered;
    private Sensor mProximitySensor;

    // light
    private boolean mLightRegistered;
    private Sensor mLightSensor;

    /** @hide */
    public PocketService(Context context) {
        super(context);
        mContext = context;
        mHandler = new PocketHandler();
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mProximityMaxRange = mProximitySensor.getMaximumRange();
        if (ENABLE_LIGHT_SENSOR) {
            mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        }
    }

    /** @hide */
    public void systemReady(boolean systemReady) {
        mSystemReady = systemReady;
    }


//    private class PocketServiceMonitor {
//
//        private final IPocketServiceCallback mCallback;
//
//        public PocketServiceMonitor(IPocketServiceCallback callback) {
//            mCallback = callback;
//        }
//
//        public IPocketServiceCallback getCallback() {
//            return mCallback;
//        }
//
//        public void sendPocketState() {
//            if (mCallback != null) {
//                try {
//                    mCallback.onPocketStateChanged(isDeviceInPocket());
//                } catch (DeadObjectException e) {
//                    Slog.w(TAG, "Death object while invoking onLockoutReset: ", e);
//                } catch (RemoteException e) {
//                    Slog.w(TAG, "Failed to invoke onLockoutReset: ", e);
//                }
//            }
//        }
//    }

    private class PocketHandler extends Handler {

        static final int MSG_DISPATCH_CALLBACKS = 0;
        static final int MSG_ADD_CALLBACK = 1;
        static final int MSG_REMOVE_CALLBACK = 2;

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DISPATCH_CALLBACKS:
                    dispatchCallbacks();
                    break;
                case MSG_ADD_CALLBACK:
                    addCallback((IPocketServiceCallback) msg.obj);
                    break;
                case MSG_REMOVE_CALLBACK:
                    removeCallback((IPocketServiceCallback) msg.obj);
                    break;
                default:
                    Slog.w(TAG, "Unknown message:" + msg.what);
            }
        }
    }

    @Override
    public void onBootPhase(int phase) {
        switch(phase) {
            case PHASE_SYSTEM_SERVICES_READY:
                break;
            case PHASE_BOOT_COMPLETED:
                break;
            default:
                break;
        }
    }

    @Override
    public void onStart() {
        publishBinderService(Context.POCKET_SERVICE, new PocketServiceWrapper());
    }

    @Override
    public void binderDied() {
        // TODO> handle death, unregister everything before taking last breathe.
    }

    private final class PocketServiceWrapper extends IPocketService.Stub {

        @Override // Binder call
        public void addCallback(final IPocketServiceCallback callback) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_ADD_CALLBACK;
            msg.obj = callback;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public void removeCallback(final IPocketServiceCallback callback) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_REMOVE_CALLBACK;
            msg.obj = callback;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public void onDeviceInteractiveChanged(boolean interactive) {
            mDeviceInteractive = interactive;
            if (mDeviceInteractive) {
                unregisterSensorListeners();
            } else {
                registerSensorListeners();
            }
        }

        @Override // Binder call
        public boolean isDeviceInPocket() {
            if (!mSystemReady) {
                return false;
            }
            return PocketService.this.isDeviceInPocket();
        }

    }

    private final SensorEventListener mProximityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLastProximityState = mProximityState;

            if (DEBUG) {
                final String sensorEventToString = sensorEvent != null ? sensorEvent.toString() : "NULL";
                Log.d(TAG, "PROXIMITY_SENSOR: onSensorChanged(), sensorEvent=" + sensorEventToString);
                printParams();
            }

            try {
                if (sensorEvent == null) {
                    if (DEBUG) Log.d(TAG, "Event is null!");
                    mProximityState = PROXIMITY_UNKNOWN;
                } else if (sensorEvent.values == null || sensorEvent.values.length == 0) {
                    if (DEBUG) Log.d(TAG, "Event has no values! event.values null ? " + (sensorEvent.values == null));
                    mProximityState = PROXIMITY_UNKNOWN;
                } else {
                    final float value = sensorEvent.values[0];
                    final boolean isNear = sensorEvent.values[0] < mProximityMaxRange;
                    if (DEBUG) Log.d(TAG, "Event: value=" + value + ", maxRange=" + mProximityMaxRange + ", isNear=" + isNear);
                    mProximityState = isNear ? PROXIMITY_NEAR : PROXIMITY_FAR;
                }
            } catch (NullPointerException e) {
                Log.e(TAG, "Event: something went wrong, exception caught, e = " + e);
                mProximityState = PROXIMITY_UNKNOWN;
            }

            if (mLastProximityState != mProximityState) {
                mHandler.removeMessages(PocketHandler.MSG_DISPATCH_CALLBACKS);
                mHandler.sendEmptyMessage(PocketHandler.MSG_DISPATCH_CALLBACKS);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private final SensorEventListener mLightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            // TODO> cache light state to dispatch when proximity changes: if was PROXIMITY_NEAR
            // and now is PROXIMITY_FAR check if light is absolutely absent as in device might be
            // still in pocket but gap between container and pocket sensor increased and triggered
            // PROXIMITY_FAR (which could be also taking device out of pocket, and if there is any
            // pending screen on operation such as call or ambient display it will be evaluate as well).
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private boolean isDeviceInPocket() {
        return mProximityState == PROXIMITY_NEAR;
    }

    private void registerSensorListeners() {
        startListeningForProximity();
        if (ENABLE_LIGHT_SENSOR) {
            startListeningForLight();
        }
    }

    private void unregisterSensorListeners() {
        stopListeningForProximity();
        if (ENABLE_LIGHT_SENSOR) {
            stopListeningForLight();
        }
    }

    private void startListeningForProximity() {
        if (DEBUG) {
            Log.d(TAG, "startListeningForProximity()");
            printParams();
        }

        if (!mProximityRegistered) {
            mSensorManager.registerListener(mProximityListener, mProximitySensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mProximityRegistered = true;
        }
    }

    private void stopListeningForProximity() {
        if (DEBUG) {
            Log.d(TAG, "startListeningForProximity()");
            printParams();
        }

        if (mProximityRegistered) {
            mLastProximityState = mProximityState = PROXIMITY_UNKNOWN;
            mSensorManager.unregisterListener(mProximityListener);
            mProximityRegistered = false;
        }
    }

    private void startListeningForLight() {
        if (DEBUG) {
            Log.d(TAG, "startListeningForLight()");
            printParams();
        }

        if (!mLightRegistered) {
            mSensorManager.registerListener(mLightListener, mLightSensor,
                    SensorManager.SENSOR_DELAY_NORMAL);
            mLightRegistered = true;
        }
    }

    private void stopListeningForLight() {
        if (DEBUG) {
            Log.d(TAG, "stopListeningForLight()");
            printParams();
        }

        if (mLightRegistered) {
            // TODO> Reset light params when implemented.
            mSensorManager.unregisterListener(mLightListener);
            mLightRegistered = false;
        }
    }

    private void addCallback(IPocketServiceCallback callback) {
        synchronized (mCallbacks) {
            if (!mCallbacks.contains(callback)) {
                mCallbacks.add(callback);
            }
        }
    }

    private void removeCallback(IPocketServiceCallback callback) {
        synchronized (mCallbacks) {
            if (mCallbacks.contains(callback)) {
                mCallbacks.remove(callback);
            }
        }
    }

    private void dispatchCallbacks() {
        synchronized (mCallbacks) {
            final int N = mCallbacks.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                IPocketServiceCallback callback= mCallbacks.get(i);
                try {
                    if (callback != null) {
                        callback.onPocketStateChanged(isDeviceInPocket());
                    } else {
                        cleanup = true;
                    }
                } catch (RemoteException e) {
                    cleanup = true;
                }
            }
            if (cleanup) {
                cleanUpCallbacksLocked(null);
            }
        }
    }

    private void cleanUpCallbacksLocked(IPocketServiceCallback callback) {
        synchronized (mCallbacks) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                IPocketServiceCallback found = mCallbacks.get(i);
                if (found == null || found == callback) {
                    mCallbacks.remove(i);
                }
            }
        }
    }

    private void printParams() {
        try {
            Log.d(TAG, "mDeviceInteractive=" + mDeviceInteractive
                    + ", mProximityState=" + mProximityState
                    + ", mProximityRegistered=" + mProximityRegistered
                    + ", mLightRegistered=" + mLightRegistered
                    + ", mProximitySensor == null ? " + (mProximitySensor == null)
                    + ", mLightSensor == null ? " + (mLightSensor == null)
                    + ", mCallbacks size =" + mCallbacks.size());
        } catch (NullPointerException e) {
            Log.e(TAG, "Printing params failed due to exception, e =" + e);
        }
    }

}
