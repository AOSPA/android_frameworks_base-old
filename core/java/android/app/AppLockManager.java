/**
 * Copyright (C) 2017 The ParanoidAndroid Project
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

/**
 * @author Anas Karbila
 * @hide
 */
public class AppLockManager {

    private static final String TAG = AppLockManager.class.getSimpleName();

    private Context mContext;
    private IAppLockService mService;

    public AppLockManager(Context context, IAppLockService service) {
        mContext = context;
        mService = service;
    }

    public void addAppToList(String packageName) {
        if (mService != null) {
            try {
                mService.addAppToList(packageName);
           } catch (RemoteException e) {
           }
        }
    }

    public void removeAppFromList(String packageName) {
        if (mService != null) {
            try {
                mService.removeAppFromList(packageName);
            } catch (RemoteException e) {
            }
        }
    }

    public boolean isAppLocked(String packageName) {
        if (mService != null) {
            try {
                return mService.isAppLocked(packageName);
            } catch (RemoteException e) {
            }
        }
        return false;
    }
}
