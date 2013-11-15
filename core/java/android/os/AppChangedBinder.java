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

import java.util.WeakHashMap;

/*
 *@hide
 */
public class AppChangedBinder {

    // One more than required
    private static final int INITIAL_SIZE = 8;

    private static final String TAG = "AppChangedBinder";
    private static final boolean DEBUG = true;

    private static final WeakHashMap<String,AppChangedCallback> mCallbacks =
        new WeakHashMap<String,AppChangedCallback>(INITIAL_SIZE);

    /*
    *@hide
    */
    public static void register(AppChangedCallback cb) {
        if(DEBUG) {
            Log.d(TAG,"Registered "+ cb.getClass().getName());
        }
        mCallbacks.put(cb.getClass().getName(),cb);
    }

    /*
    *@hide
    */
    public static void register(String name, AppChangedCallback cb) {
        if(DEBUG) {
            Log.d(TAG,"Registered "+ name);
        }
        mCallbacks.put(name,cb);
    }

    /*
    *@hide
    */
    public static void notifyAppChanged() {
        Log.d(TAG, "omg its working");
        for (AppChangedCallback cb : mCallbacks.values()) {
            if(DEBUG) {
                Log.d(TAG,"Notifed "+ cb.getClass().getName());
            }
            cb.appChanged();
        }
    }


}
