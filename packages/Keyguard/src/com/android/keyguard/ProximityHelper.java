package com.android.keyguard;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * Created by carlo on 16-10-17.
 */

public class ProximityHelper  implements SensorEventListener {

    private static final String TAG = ProximityHelper.class.getSimpleName();
    private static final boolean DEBUG = true;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called but we don't have a
     * valid event value yet.
     */
    private static final int PROXIMITY_UNKNOWN = 0;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is covered.
     */
    protected static final int PROXIMITY_NEAR = 1;

    /**
     * Proximity sensor has been registered, onSensorChanged() has been called and we have a
     * valid event value which determined proximity sensor is not covered.
     */
    private static final int PROXIMITY_FAR = 2;

    private static final int PROXIMITY_TIMEOUT = 2000;

    private final ArrayList<WeakReference<ProximityListener>> mListeners = new ArrayList<>(1);

    private Handler mHandler;
    private SensorManager mSensorManager;
    private Sensor mSensor;

    private boolean mRegistered;
    private int mProximityResult = PROXIMITY_UNKNOWN;
    private float mMaxRange;

    public ProximityHelper(Context context) {
        ensureHandler();
        mSensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
        mMaxRange = mSensor.getMaximumRange();
    }

    public boolean init() {
        if (mSensor != null) {
            register();
            return true;
        }
        return false;
    }

//    public void onDeviceInteractiveChanged(boolean interactive) {
//        if (interactive) {
//            unregister();
//        }
//    }

    public void addListener(ProximityListener l) {
        synchronized (mListeners) {
            cleanUpListenersLocked(l);
            mListeners.add(new WeakReference<>(l));
        }
    }

    public void removeListener(ProximityListener l) {
        synchronized (mListeners) {
            cleanUpListenersLocked(l);
        }
    }

    private synchronized void ensureHandler() {
        if (mHandler == null) {
            HandlerThread thread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
            thread.start();
            mHandler = new Handler(thread.getLooper());
        }
    }

    private void dispatchListeners(int result) {
        synchronized (mListeners) {
            final int N = mListeners.size();
            boolean cleanup = false;
            for (int i = 0; i < N; i++) {
                ProximityListener l = mListeners.get(i).get();
                if (l != null) {
                    l.onProximityResult(result);
                } else {
                    cleanup = true;
                }
            }
            if (cleanup) {
                cleanUpListenersLocked(null);
            }
        }
    }

    private void cleanUpListenersLocked(ProximityListener listener) {
        for (int i = mListeners.size() - 1; i >= 0; i--) {
            ProximityListener found = mListeners.get(i).get();
            if (found == null || found == listener) {
                mListeners.remove(i);
            }
        }
    }

    private void dispatchWithResult(int result) {
        if (DEBUG) Log.d(TAG, "dispatchWithResult(), result = " + result);
        mHandler.postDelayed(mTimeout, PROXIMITY_TIMEOUT);
        dispatchListeners(result);
    }

    public void register() {
        if (mSensor == null) {
            if (DEBUG) Log.e(TAG, "No proximity sensor found");
            return;
        }

        if (!mRegistered) {
            mHandler.removeCallbacks(mTimeout);
            mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_FASTEST);
            mRegistered = true;
        } else {
            if (DEBUG) Log.d(TAG, "Proximity sensor already registered");
        }
    }

    public void unregister() {
        if (mRegistered) {
            if (DEBUG) Log.d(TAG, "Un-registering proximity sensor");
            mSensorManager.unregisterListener(this);
            mRegistered = false;
        } else {
            if (DEBUG) Log.d(TAG, "No proximity sensor registered");
        }
    }

    private Runnable mTimeout = new Runnable() {
        @Override
        public void run() {
            unregister();
        }
    };

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event == null || event.values == null || event.values.length == 0) {
            if (DEBUG) Log.d(TAG, "Event has no values!");
            dispatchWithResult(PROXIMITY_UNKNOWN);
        } else {
            if (DEBUG) Log.d(TAG, "Event: value=" + event.values[0] + " max=" + mMaxRange);
            final boolean isNear = event.values[0] < mMaxRange;
            mProximityResult = isNear ? PROXIMITY_NEAR : PROXIMITY_FAR;
            dispatchWithResult(mProximityResult);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // no-op
    }

    public interface ProximityListener {
        void onProximityResult(int result);
    }

}
