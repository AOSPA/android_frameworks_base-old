/**
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

package android.app;

import android.annotation.SystemService;
import android.content.Context;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.ServiceManager.ServiceNotFoundException;

/**
 * @author Anas Karbila
 * @author StyloG
 * @hide
 */
@SystemService(Context.SECURE_APPS_SERVICE)
public class SecureAppsManager {

    private static final String TAG = "SecureAppsManager";

    private ISecureAppsManagerService mService;

    public SecureAppsManager(ISecureAppsManagerService service) {
        mService = service;
    }

    public void addAppToList(String packageName) {
        try {
            mService.addAppToList(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeAppFromList(String packageName) {
        try {
            mService.removeAppFromList(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public boolean isAppSecured(String packageName) {
        try {
            return mService.isAppSecured(packageName);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public int getSecuredAppsCount() {
        try {
            return mService.getSecuredAppsCount();
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void addSecureAppsCallback(ISecureAppsCallback cb) {
        try {
            mService.addSecureAppsCallback(cb);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }

    public void removeSecureAppsCallback(ISecureAppsCallback cb) {
        try {
            mService.removeSecureAppsCallback(cb);
        } catch (RemoteException e) {
            throw e.rethrowFromSystemServer();
        }
    }
}
