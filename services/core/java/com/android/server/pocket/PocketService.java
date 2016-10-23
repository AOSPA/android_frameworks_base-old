/**
 * Copyright (C) 2016 The ParanoidAndroid Project
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
package com.android.server.pocket;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.DeadObjectException;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemClock;
import android.pocket.IPocketService;
import android.pocket.IPocketCallback;
import android.pocket.PocketManager;
import android.util.Log;
import android.util.Slog;

import com.android.server.SystemService;

import java.util.ArrayList;

/**
 * A service to manage multiple clients that want to listen for pocket state.
 * The service is responsible for maintaining a list of clients and dispatching all
 * pocket -related information.
 *
 * @author Carlo Savignano
 * @hide
 */
public class PocketService extends SystemService implements IBinder.DeathRecipient {

    private static final String TAG = PocketService.class.getSimpleName();
    private static final boolean DEBUG = true;
    private static final boolean DEBUG_SPEW = DEBUG;

    // TODO> implement companion light sensor logic.
    private static final boolean ENABLE_LIGHT_SENSOR = DEBUG;

    /**
     * Wheater we don't have yet a valid proximity sensor event or pocket service not running.
     */
    private static final int PROXIMITY_UNKNOWN = 0;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is covered.
     */
    private static final int PROXIMITY_POSITIVE = 1;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is not covered.
     */
    private static final int PROXIMITY_NEGATIVE = 2;

    /**
     * Wheater we don't have yet a valid light sensor event or pocket service not running.
     */
    private static final int LIGHT_UNKNOWN = 0;

    /**
     * Livght sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined light is absent or poor.
     */
    private static final int LIGHT_POOR = 1;

    /**
     * Livght sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined light is visible.
     */
    private static final int LIGHT_VISIBLE = 2;

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
    private int mLightState = LIGHT_UNKNOWN;
    private int mLastLightState = LIGHT_UNKNOWN;
    private float mLightMaxRange;
    private boolean mLightRegistered;
    private Sensor mLightSensor;

    public PocketService(Context context) {
        super(context);
        mContext = context;
        HandlerThread handlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        mHandler = new PocketHandler(handlerThread.getLooper());
        mSensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mProximityMaxRange = mProximitySensor.getMaximumRange();
        mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        mLightMaxRange = mLightSensor.getMaximumRange();
    }

    private class PocketHandler extends Handler {

        public static final int MSG_SYSTEM_READY = 0;
        public static final int MSG_SYSTEM_BOOTED = 1;
        public static final int MSG_DISPATCH_CALLBACKS = 2;
        public static final int MSG_ADD_CALLBACK = 3;
        public static final int MSG_REMOVE_CALLBACK = 4;
        public static final int MSG_INTERACTIVE_CHANGED = 5;
        public static final int MSG_SENSOR_EVENT_PROXIMITY = 6;
        public static final int MSG_SENSOR_EVENT_LIGHT = 7;

        public PocketHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_SYSTEM_READY:
                    handleSystemReady();
                    break;
                case MSG_SYSTEM_BOOTED:
                    handleSystemBooted();
                    break;
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
                case MSG_SENSOR_EVENT_PROXIMITY:
                    handleProximitySensorEvent((SensorEvent) msg.obj);
                    break;
                case MSG_SENSOR_EVENT_LIGHT:
                    handleLightSensorEvent((SensorEvent) msg.obj);
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
                mHandler.sendEmptyMessage(PocketHandler.MSG_SYSTEM_READY);
                break;
            case PHASE_BOOT_COMPLETED:
                mHandler.sendEmptyMessage(PocketHandler.MSG_SYSTEM_BOOTED);
                break;
            default:
                Slog.w(TAG, "Un-handled boot phase:" + phase);
                break;
        }
    }

    @Override
    public void onStart() {
        publishBinderService(Context.POCKET_SERVICE, new PocketServiceWrapper());
    }

    @Override
    public void binderDied() {
        synchronized (mCallbacks) {
            mProximityState = PROXIMITY_UNKNOWN;
            int callbacksSize = mCallbacks.size();
            for (int i = callbacksSize - 1; i >= 0; i--) {
                if (mCallbacks.get(i) != null) {
                    try {
                        mCallbacks.get(i).onStateChanged(false, PocketManager.REASON_RESET);
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
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_SENSOR_EVENT_PROXIMITY;
            msg.obj = sensorEvent;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private final SensorEventListener mLightListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent sensorEvent) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_SENSOR_EVENT_LIGHT;
            msg.obj = sensorEvent;
            mHandler.sendMessage(msg);
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int i) { }
    };

    private boolean isDeviceInPocket() {
        if (mProximityState == PROXIMITY_POSITIVE) {
            return mLightState == LIGHT_POOR;
        }
        return mProximityState != PROXIMITY_POSITIVE;
    }

    private boolean wasDeviceInPocket() {
        if (mLastProximityState == PROXIMITY_POSITIVE) {
            return mLastLightState == LIGHT_POOR;
        }
        return mLastProximityState != PROXIMITY_POSITIVE;
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
                    SensorManager.SENSOR_DELAY_NORMAL, mHandler);
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
                    SensorManager.SENSOR_DELAY_NORMAL, mHandler);
            mLightRegistered = true;
        }
    }

    private void stopListeningForLight() {
        if (DEBUG) {
            Log.d(TAG, "stopListeningForLight()");
            printParams();
        }

        if (mLightRegistered) {
            mLightState = mLastLightState = LIGHT_UNKNOWN;
            mSensorManager.unregisterListener(mLightListener);
            mLightRegistered = false;
        }
    }

    /***** Pocket background thread work. *****/

    private void handleSystemReady() {
        if (DEBUG) {
            Log.d(TAG, "onBootPhase(): PHASE_SYSTEM_SERVICES_READY");
            printParams();
        }
        mSystemReady = true;
        if (mPending) {
            final Message msg = new Message();
            msg.what = PocketHandler.MSG_INTERACTIVE_CHANGED;
            msg.arg1 = mInteractive ? 1 : 0;
            mHandler.sendMessage(msg);
            mPending = false;
        }
    }

    private void handleSystemBooted() {
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
    }

    private void handleDispatchCallbacks() {
        synchronized (mCallbacks) {
            final int N = mCallbacks.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                final IPocketCallback callback = mCallbacks.get(i);
                try {
                    if (callback != null) {
                        callback.onStateChanged(isDeviceInPocket(), PocketManager.REASON_SENSOR);
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

    private void handleInteractiveChanged(boolean interactive) {
        // always update interactive state.
        mInteractive = interactive;

        if (mPending) {
            // working on it.
            return;
        } else if (!mPending && (!mSystemBooted || !mSystemReady)) {
            // postpone till system is ready AND booted.
            mPending = true;
            return;
        }

        if (mInteractive) {
            if (isDeviceInPocket()) {
                // leave it on, will unregister when pocket state changes while device state
                // is interactive and/or device state switches back to not interactive.
                return;
            }
            unregisterSensorListeners();
        } else {
            registerSensorListeners();
        }
    }

    private void handleLightSensorEvent(SensorEvent sensorEvent) {
        mLastLightState = mLightState;

        if (DEBUG) {
            final String sensorEventToString = sensorEvent != null ? sensorEvent.toString() : "NULL";
            Log.d(TAG, "LIGHT_SENSOR: onSensorChanged(), sensorEvent =" + sensorEventToString);
            printParams();
        }

        try {
            if (sensorEvent == null) {
                if (DEBUG) Log.d(TAG, "Event is null!");
                mLightState = LIGHT_UNKNOWN;
            } else if (sensorEvent.values == null || sensorEvent.values.length == 0) {
                if (DEBUG) Log.d(TAG, "Event has no values! event.values null ? " + (sensorEvent.values == null));
                mLightState = LIGHT_UNKNOWN;
            } else {
                final long time = SystemClock.uptimeMillis();
                final float value = sensorEvent.values[0];
                final boolean isPoor = value >= 0.0f /* absent */ && value <= 0.1f /* maximum light observed in pocket */;
                if (DEBUG) Log.d(TAG, "Event: time= " + time + ", value=" + value
                        + ", maxRange=" + mLightMaxRange + ", isPoor=" + isPoor);
                mLightState = isPoor ? LIGHT_POOR : LIGHT_VISIBLE;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Event: something went wrong, exception caught, e = " + e);
            mLightState = LIGHT_UNKNOWN;
        }

        dispatchCallbacksIfNeeded();
    }

    private void handleProximitySensorEvent(SensorEvent sensorEvent) {
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
                if (DEBUG) Log.d(TAG, "Event: value=" + value
                        + ", maxRange=" + mProximityMaxRange + ", isNear=" + isNear);
                mProximityState = isNear ? PROXIMITY_POSITIVE : PROXIMITY_NEGATIVE;
            }
        } catch (NullPointerException e) {
            Log.e(TAG, "Event: something went wrong, exception caught, e = " + e);
            mProximityState = PROXIMITY_UNKNOWN;
        }

        dispatchCallbacksIfNeeded();
    }

    private void dispatchCallbacksIfNeeded() {
        final boolean isDeviceInPocket = isDeviceInPocket();
        final boolean wasDeviceInPocket = wasDeviceInPocket();
        if (isDeviceInPocket != wasDeviceInPocket) {
            if (mInteractive && wasDeviceInPocket) {
                unregisterSensorListeners();
            }
            mHandler.removeMessages(PocketHandler.MSG_DISPATCH_CALLBACKS);
            mHandler.sendEmptyMessage(PocketHandler.MSG_DISPATCH_CALLBACKS);
        }
    }

    /***** SPEW *****/

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
