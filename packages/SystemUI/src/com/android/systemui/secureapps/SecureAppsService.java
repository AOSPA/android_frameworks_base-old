/*
 * Copyright (C) 2020 Paranoid Android
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

package com.android.systemui.secureapps;

import android.app.Service;
import android.app.ISecureAppsCallback;
import android.app.ISecureAppsService;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.IBinder;
import android.util.Log;

public class SecureAppsService extends Service {

    private static final String TAG = "SecureAppsService";

    private RemoteWrapper mService;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "is bound");
        return mService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Setting up remote");
        mService = new RemoteWrapper();
    }

    private class RemoteWrapper extends ISecureAppsService.Stub {
        @Override
        public void initSecureApps(int userId) {
            // no op
            Log.d(TAG, "initSecureApps");
        }

        @Override
        public void readState(boolean enabled) {
            // no op
        }

        @Override
        public void setAppLaunching(String packageName) {
            // no op
        }

        @Override
        public void addAppToList(String packageName) {
            // no op
        }

        @Override
        public void removeAppFromList(String packageName) {
            // no op
        }

        @Override
        public void launchBeforeActivity(String packageName) {
            // no op
        }

        @Override
        public void setAppIntent(String packageName, Intent intent) {
            // no op
        }

        @Override
        public boolean isAppSecured(String packageName) {
            return false;
        }

        @Override
        public int getSecuredAppsCount() {
            return -1;
        }

        @Override
        public void addSecureAppsCallback(ISecureAppsCallback callback) {
            // no op
        }

        @Override
        public void removeSecureAppsCallback(ISecureAppsCallback callback) {
            // no op
        }

        @Override
        public void onWindowsDrawn(String packageName) {
            // no op
        }

        @Override
        public void onAppWindowRemoved(String packageName, IBinder token) {
            // no op
        }

        @Override
        public void updateAppVisibility(String packageName) {
            // no op
        }

        @Override
        public void promptIfNeeded(String packageName, IBinder token) {
            // no op
        }

        @Override
        public boolean isGame(String packageName) {
            return false;
        }

        @Override
        public boolean isAppOpen(String packageName) {
            return false;
        }

        @Override
        public void updateSecureAppsConfig(String packageName, Configuration newConfig) {
            // no op
        }
    };
}
