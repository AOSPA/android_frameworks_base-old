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
package com.android.server;

import android.app.IThemeCallback;
import android.app.IThemeService;
import android.app.ThemeManager;
import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.Settings;
import android.util.Log;

import com.android.server.SystemService;

import java.util.ArrayList;

/**
 * A service to select and use custom themes.
 * The service is responsible for enabling and disabling the custom theme.

 * TODO: Add more themes
 *
 * @author Anas Karbila
 * @hide
 */
public class ThemeService extends IThemeService.Stub {

    private static final String TAG = ThemeService.class.getSimpleName();
    private static final boolean DEBUG = true;

    private final ArrayList<IThemeCallback> mCallbacks= new ArrayList<>();

    private ThemeObserver mObserver;

    private Context mContext;

    public ThemeService(Context context) {
        mContext = context;
        final Handler mHandler = new Handler();
        mObserver = new ThemeObserver(mHandler);
        mObserver.register();
    }

    @Override
    public void addCallback(IThemeCallback callback) {
        synchronized (mCallbacks) {
            if (!mCallbacks.contains(callback)) {
                mCallbacks.add(callback);
            }
            dispatchThemeUpdated();
        }
    }

    @Override
    public void removeCallback(IThemeCallback callback) {
        synchronized (mCallbacks) {
            if (mCallbacks.contains(callback)) {
                mCallbacks.remove(callback);
            }
        }
    }

    private boolean isThemeApplied() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.THEME_ENABLED, 1 /* default */) == 1;
    }

    private void dispatchThemeUpdated() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            IThemeCallback callback = mCallbacks.get(i);
            try {
                if (callback != null) {
                    callback.onThemeChanged(isThemeApplied());
                }
            } catch (RemoteException ex) {
                // Callback is dead
            }
        }
    }

    private class ThemeObserver extends ContentObserver {
        private boolean mRegistered;

        public ThemeObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            Log.d(TAG, "onChange called");
            dispatchThemeUpdated();
        }

        public void register() {
            if (!mRegistered) {
                Log.d(TAG, "Registering themeobserver");
                mContext.getContentResolver().registerContentObserver(
                        Settings.Secure.getUriFor(Settings.Secure.THEME_ENABLED), true, this);
                mRegistered = true;
                dispatchThemeUpdated();
            }
        }

        public void unregister() {
            if (mRegistered) {
                Log.d(TAG, "unregistering theemeobserver");
                mContext.getContentResolver().unregisterContentObserver(this);
                mRegistered = false;
            }
        }

    }
}
