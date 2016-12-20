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
package android.app;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * A class that updates the current theme to pixel theme
 * <p>
 * Use {@link android.content.Context#getSystemService(java.lang.String)}
 * with argument {@link android.content.Context#THEME_SERVICE} to get
 * an instance of this class.
 *
 * Usage: call void updateTheme with argument true if you want to enable the theme
 * @author Anas Karbila
 */
public class ThemeManager {

    public static final String TAG = "ThemeManager";
    public static final boolean DEBUG = false;

    private Context mContext;
    private IThemeService mService;

    public ThemeManager(Context context, IThemeService service) {
        mContext = context;
        mService = service;
        if (mService == null) {
            Log.e(TAG, "ThemeService was null");
        }
    }

    public void addCallback(IThemeCallback callback) {
        if (mService != null) {
            try {
                mService.addCallback(callback);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to dispatch callback");
                if (callback != null) {
                    try {
                        callback.onThemeChanged(false);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to call onThemeChanged");
                    }
                }
            }
        }
    }

    public void removeCallback(IThemeCallback callback) {
        if (mService != null) {
            try {
                mService.removeCallback(callback);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to remove callback");
                if (callback != null) {
                    try {
                        callback.onThemeChanged(false);
                    } catch (RemoteException e) {
                        Log.e(TAG, "Failed to call onThemeChanged");
                    }
                }
            }
        }
    }
}
