/**
 * Copyright (C) 2017-2019 The ParanoidAndroid Project
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

import android.app.ISecureAppsCallback;
import android.context.Intent;
import android.content.res.Configuration;
import android.os.IBinder;

/** @hide */
interface ISecureAppsService {

    void initSecureApps(in int userId);

    void readState(in boolean enabled);

    void setAppLaunching(in String packageName);

    void addAppToList(in String packageName);

    void removeAppFromList(in String packageName);

    void launchBeforeActivity(in String packageName);

    void setAppIntent(in String packageName, Intent intent);

    boolean isAppSecured(in String packageName);

    int getSecuredAppsCount();

    void addSecureAppsCallback(ISecureAppsCallback callback);

    void removeSecureAppsCallback(ISecureAppsCallback callback);

    void onWindowsDrawn(in String packageName);

    void onAppWindowRemoved(in String packageName, IBinder token);

    void updateAppVisibility(in String packageName);

    void promptIfNeeded(in String packageName, IBinder token);

    boolean isGame(in String packageName);

    boolean isAppOpen(in String packageName);

    void updateSecureAppsConfig(in String packageName, Configuration newConfig);
}
