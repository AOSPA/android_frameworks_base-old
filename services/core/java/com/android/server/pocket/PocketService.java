package com.android.server.pocket;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.pocket.IPocketService;
import android.pocket.IPocketCallback;
import android.util.Log;
import android.util.Slog;

import com.android.server.SystemService;

import java.util.ArrayList;

/** @hide */
public class PocketService extends SystemService implements IBinder.DeathRecipient {

    private static final String TAG = PocketService.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_SPEW = true;

    // TODO> implement companion light sensor logic.
    private static final boolean ENABLE_LIGHT_SENSOR = false;

    /**
     * Wheater we don't have yet a valid proximity sensor event or pocket service not running.
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

    private final ArrayList<IPocketCallback> mCallbacks= new ArrayList<>();

    private Context mContext;
    private boolean mSystemReady;
    private boolean mSystemBooted;
    private boolean mInteractive;
    private boolean mPending;
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
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    /** @hide */
    public void systemReady(boolean systemReady) {
        mSystemReady = systemReady;
    }

    /**
     * Pocket callback wrapper.
     *  @deprecated
     */
    @Deprecated
    private class PocketServiceMonitor {

        private final IPocketCallback mCallback;

        public PocketServiceMonitor(IPocketCallback callback) {
            mCallback = callback;
        }

        public IPocketCallback getCallback() {
            return mCallback;
        }

        public void sendPocketState() {
            if (mCallback != null) {
                try {
                    mCallback.onStateChanged(isDeviceInPocket());
                } catch (DeadObjectException e) {
                    Slog.w(TAG, "Death object while invoking sendPocketState: ", e);
                } catch (RemoteException e) {
                    Slog.w(TAG, "Failed to invoke sendPocketState: ", e);
                }
            }
        }
    }

    private class PocketHandler extends Handler {

        static final int MSG_DISPATCH_CALLBACKS = 0;
        static final int MSG_ADD_CALLBACK = 1;
        static final int MSG_REMOVE_CALLBACK = 2;
        static final int MSG_INTERACTIVE_CHANGED = 3;

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_DISPATCH_CALLBACKS:
                    handleDispatchCallbacks();
                    break;
                case MSG_ADD_CALLBACK:
                    handleAddCallback((IPocketCallback) msg.obj);
                    break;
                case MSG_REMOVE_CALLBACK:
                    handleRemoveCallback((IPocketCallback) msg.obj);
                    break;
                case MSG_INTERACTIVE_CHANGED:
                    handleInteractiveChanged(msg.arg1 != 0);
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
                if (DEBUG) {
                    Log.d(TAG, "onBootPhase(): PHASE_SYSTEM_SERVICES_READY");
                    printParams();
                }
                break;
            case PHASE_BOOT_COMPLETED:
                if (DEBUG) {
                    Log.d(TAG, "onBootPhase(): PHASE_BOOT_COMPLETED");
                    printParams();
                }
                mSystemBooted = true;
                if (mPending) {
                    final Message msg = new Message();
                    msg.what = PocketHandler.MSG_INTERACTIVE_CHANGED;
                    msg.arg1 = mInteractive ? 1 : 0;
                    mHandler.sendMessage(msg);
                    mPending = false;
                }
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
        synchronized (mCallbacks) {
            mProximityState = PROXIMITY_UNKNOWN;
            int callbacksSize = mCallbacks.size();
            for (int i = callbacksSize - 1; i >= 0; i--) {
                if (mCallbacks.get(i) != null) {
                    try {
                        mCallbacks.get(i).onStateChanged(false);
                    } catch (DeadObjectException e) {
                        Slog.w(TAG, "Death object while invoking sendPocketState: ", e);
                    } catch (RemoteException e) {
                        Slog.w(TAG, "Failed to invoke sendPocketState: ", e);
                    }
                }
            }
            mCallbacks.clear();
        }
        unregisterSensorListeners();
    }

    private final class PocketServiceWrapper extends IPocketService.Stub {

        @Override // Binder call
        public void addCallback(final IPocketCallback callback) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_ADD_CALLBACK;
            msg.obj = callback;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public void removeCallback(final IPocketCallback callback) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_REMOVE_CALLBACK;
            msg.obj = callback;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public void onInteractiveChanged(boolean interactive) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_INTERACTIVE_CHANGED;
            msg.arg1 = interactive ? 1 : 0;
            mHandler.sendMessage(msg);
        }

        @Override // Binder call
        public boolean isDeviceInPocket() {
            final long ident = Binder.clearCallingIdentity();
            try {
                if (!mSystemReady || !mSystemBooted) {
                    return false;
                }
                return PocketService.this.isDeviceInPocket();
            } finally {
                Binder.restoreCallingIdentity(ident);
            }
        }

    }

    private final SensorEventListener mProximityListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            mLastProximityState = mProximityState;

            if (DEBUG) {
                final String sensorEventToString = sensorEvent != null ? sensorEvent.toString() : "NULL";
                Log.d(TAG, "PROXIMITY_SENSOR: onSensorChanged(), sensorEvent =" + sensorEventToString);
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
        startListeningForLight();
    }

    private void unregisterSensorListeners() {
        stopListeningForProximity();
        stopListeningForLight();
    }

    private void startListeningForProximity() {
        if (DEBUG) {
            Log.d(TAG, "startListeningForProximity()");
            printParams();
        }

        if (mProximitySensor == null) {
            Log.d(TAG, "Cannot detect proximity sensor, sensor is NULL");
            return;
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

        if (!ENABLE_LIGHT_SENSOR) {
            return;
        }

        if (mLightSensor == null) {
            Log.d(TAG, "Cannot detect light sensor, sensor is NULL");
            return;
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

    private void handleInteractiveChanged(boolean interactive) {
        if (!mPending && !mSystemBooted) {
            mPending = true;
            return;
        }

        mInteractive = interactive;
        if (mInteractive) {
            unregisterSensorListeners();
        } else {
            registerSensorListeners();
        }
    }

    private void handleAddCallback(IPocketCallback callback) {
        synchronized (mCallbacks) {
            if (!mCallbacks.contains(callback)) {
                mCallbacks.add(callback);
            }
        }
    }

    private void handleRemoveCallback(IPocketCallback callback) {
        synchronized (mCallbacks) {
            if (mCallbacks.contains(callback)) {
                mCallbacks.remove(callback);
            }
        }
    }

    private void handleDispatchCallbacks() {
        synchronized (mCallbacks) {
            final int N = mCallbacks.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                final IPocketCallback callback = mCallbacks.get(i);
                try {
                    if (callback != null) {
                        callback.onStateChanged(isDeviceInPocket());
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

    private void cleanUpCallbacksLocked(IPocketCallback callback) {
        synchronized (mCallbacks) {
            for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                IPocketCallback found = mCallbacks.get(i);
                if (found == null || found == callback) {
                    mCallbacks.remove(i);
                }
            }
        }
    }

    private void printParams() {
        if (DEBUG_SPEW) {
            try {
                Log.d(TAG, "mInteractive=" + mInteractive
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

}
