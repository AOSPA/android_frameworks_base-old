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

import java.util.ArrayList;
import java.util.List;

/**
 * A service to select and use custom themes.
 * The service is responsible for enabling and disabling the custom theme.
 *
 * @author Anas Karbila
 * @hide
 */
public class ThemeService extends IThemeService.Stub {

    private static final String TAG = ThemeService.class.getSimpleName();

    private final List<IThemeCallback> mCallbacks = new ArrayList<>();

    private Handler mHandler;

    private ThemeObserver mObserver;

    private Context mContext;

    private boolean mSystemReady;

    public ThemeService(Context context) {
        mContext = context;
        mHandler = new Handler();
        mObserver = new ThemeObserver(mHandler);
        mObserver.register();
    }

    @Override
    public void addCallback(IThemeCallback callback) {
        synchronized (mCallbacks) {
            if (!mCallbacks.contains(callback)) {
                mCallbacks.add(callback);
            }
            dispatchCallbackAdded();
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

    // called by system server
    public void systemReady() {
        mSystemReady = true;
    }

    private boolean isThemeApplied() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.THEME_ENABLED, 1 /* default */) == 1;
    }

    private int getAccentColor() {
        return Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.THEME_ACCENT_COLOR, 1);
    }

    private void dispatchCallbackAdded() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            IThemeCallback callback = mCallbacks.get(i);
            try {
                if (callback != null) {
                    callback.onCallbackAdded(isThemeApplied(), getAccentColor());
                }
            } catch (RemoteException ex) {
                // Callback is dead
            }
        }
    }

    private void dispatchThemeSettingChanged() {
        for (int i = 0; i < mCallbacks.size(); i++) {
            IThemeCallback callback = mCallbacks.get(i);
            try {
                if (callback != null) {
                    callback.onThemeChanged(isThemeApplied(), getAccentColor());
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
            dispatchThemeSettingChanged();
        }

        public void register() {
            if (!mRegistered) {
                mContext.getContentResolver().registerContentObserver(
                        Settings.Secure.getUriFor(Settings.Secure.THEME_ENABLED), true, this);
                mContext.getContentResolver().registerContentObserver(
                        Settings.Secure.getUriFor(Settings.Secure.THEME_ACCENT_COLOR), true, this);
                mRegistered = true;
                dispatchCallbackAdded();
            }
        }

        public void unregister() {
            if (mRegistered) {
                mContext.getContentResolver().unregisterContentObserver(this);
                mRegistered = false;
            }
        }

    }
}
