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
import android.os.IBinder;
import android.util.Log;

public class SecureAppsRemoteService extends Service {

    private static final String TAG = "SecureAppsRemoteService";

    /*private RemoteWrapper mService;

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "is bound");
        return mService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Setting up the remote wrapper");
        mService = new RemoteWrapper();
    }

    private class RemoteWrapper extends ISecureAppsService.Stub {
        @Override
        public void addAppToList(String packageName) {
            // no op
        }

        @Override
        public void addAppExtraToList(String packageName, String extraName) {
            // no op
        }

        @Override
        public void removeAppFromList(String packageName) {
            // no op
        }

        @Override
        public void removeAppExtraFromList(String packageName, String extraName) {
            // no op
        }

        @Override
        public boolean isAppLocked(String packageName) {
            return false;
        }

        @Override
        public boolean hasAppExtra(String packageName, String extraName) {
            return false;
        }

        @Override
        public int getLockedAppsCount() {
            return -1;
        }

        @Override
        public void addAppLockCallback(ISecureAppsCallback callback) {
            // no op
        }

        @Override
        public void removeAppLockCallback(ISecureAppsCallback callback) {
            // no op
        }
    };*/
}
