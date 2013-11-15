/*
 * Copyright (C) 2013 Paranoid Android
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

package android.os;

import android.os.AppChangedCallback;
import android.util.Log;

import android.app.ActivityThread.HybridCallback;

import java.util.WeakHashMap;

import android.content.res.Configuration;
/**
 * Manages the list of classes implmenting an AppChangedCallback.
 * @hide
 */
public class AppChangedBinder {

    static final String TAG = "AppChangedBinder";
    static final boolean DEBUG = true;

    // One more than required for systemUI
    static final int INITIAL_SIZE = 7;

	static HybridCallback mSystem = null;

    // uses weak refrnces to avoid leaks, list is auto manged
    static final WeakHashMap<String,AppChangedCallback> mCallbacks =
        new WeakHashMap<String,AppChangedCallback>(INITIAL_SIZE);

    /**
     * Register to be notified of the application context being changed.
     * @param callback object implmenting an ActiveAppCallBack
     * @hide
     */
    public static void register(AppChangedCallback callback) {
        if(DEBUG) Log.d(TAG,"Registered "+ callback.getClass().getName());
        mCallbacks.put(callback.getClass().getName(),callback);
    }

    /**
     * Register to be notified of the application context being changed.
     * @param id a unique string identifier
     * @param callback object implmenting an ActiveAppCallBack
     * @hide
     */
    public static void register(String id, AppChangedCallback callback) {
        if(DEBUG) Log.d(TAG,id);
        mCallbacks.put(id,callback);
    }

    /**
     * Register to be notified of the application context being changed.
     * @param id a unique string identifier
     * @param callback object implmenting an ActiveAppCallBack
     * @hide
     */
    public static void registerSystem(HybridCallback callback) {
        if(DEBUG) Log.d(TAG,"added system");
        mSystem = callback;
    }

	public static void updateConfig(Configuration config) {
		if (mSystem !=null) {
			mSystem.updateConfig(config);
		}
	}

    /**
     * Notifies all the callbacks that the application context has changed.
     * TODO: gaurd to be only called by HybridManger.
     * @hide
     */
    public static void notifyOfAppChange() {
        if (DEBUG) Log.d(TAG,"Notifing applications of context change ");
        for (AppChangedCallback cb : mCallbacks.values()) {
            if (DEBUG) Log.d(TAG,"Notifed "+ cb.getClass().getName());
            cb.appChanged();
        }
    }
}
