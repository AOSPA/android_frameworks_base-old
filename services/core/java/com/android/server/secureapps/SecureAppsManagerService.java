
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
package com.android.server.secureapps;

import static com.android.server.wm.ActivityTaskManagerDebugConfig.DEBUG_APPLOCK;
import static com.android.server.wm.ActivityTaskManagerDebugConfig.POSTFIX_APPLOCK;

import android.app.admin.DevicePolicyManager;
import android.app.ActivityManager;
import android.app.ISecureAppsService;
import android.app.ISecureAppsManagerService;
import android.app.ISecureAppsCallback;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.database.ContentObserver;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.net.Uri;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.DeadObjectException;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.util.ArrayMap;
import android.util.ArraySet;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;
import com.android.internal.os.BackgroundThread;
import com.android.internal.util.CollectionUtils;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.wm.ActivityTaskManagerService;
import com.android.server.wm.WindowState;
import libcore.io.IoUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class SecureAppsManagerService extends SystemService {

    private static final String TAG = "SecureAppsManagerService";

    private AtomicBoolean mEnabled;
    private int mUserHandle;
    private int mUserId;
    private UserManager mUserManager;

    private Context mContext;
    private final SecureAppsHandler mHandler;
    private final Object mLock = new Object();

    SparseArray<ISenseService> mServices = new SparseArray<>();
    public boolean mBound = false;

    public SecureAppsManagerService(Context context) {
        super(context);
        mContext = context;
        mHandler = new SecureAppsHandler(BackgroundThread.getHandler().getLooper());
        mUserId = ActivityManager.getCurrentUser();
        mUserManager = UserManager.get(context);
        mEnabled = new AtomicBoolean(!mUserManager.isManagedProfile(mUserId)
                && mUserManager.isUserUnlockingOrUnlocked(mUserId));
        mHandler.sendEmptyMessage(SecureAppsHandler.MSG_READ_STATE);
    }
    @Override
    public void onStart() {
        publishBinderService(Context.SECURE_APPS_SERVICE, new SecureAppsImpl());
        publishLocalService(SecureAppsManagerService.class, this);
    }

    @Override
    public void onUnlockUser(int userHandle) {
        mUserHandle = userHandle;
        mHandler.sendEmptyMessage(SecureAppsHandler.MSG_INIT_APPS);
    }

    @Override
    public void onSwitchUser(int userHandle) {
        mUserHandle = userHandle;
        mHandler.sendEmptyMessage(SecureAppsHandler.MSG_INIT_APPS);
    }

    @Override
    public void onStopUser(int userHandle) {
        mEnabled.set(false);
    }

    private void initLockedApps() {
        if (mUserManager.isManagedProfile(mUserHandle)) {
            mEnabled.set(false);
        } else {
            mUserId = mUserHandle;
            mEnabled.set(true);
            ISecureAppsService service = getService();
            if (service != null) {
                try {
                    service.initSecureApps(mUserId);
                } catch (Exception e) {
                    Slog.e(TAG, "initLockedApps failed", e);
                }
                return;
            }
            bind(mUserId);
            Slog.w(TAG, "initLockedApps(): secure apps service not started!");
            readState();
        }
    }

    private void readState() {
        if (!mEnabled.get()) {
            return;
        }

        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.readState(mEnabled);
            } catch (Exception e) {
                Slog.e(TAG, "readState failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "readState(): secure apps service not started!");
    }

    private void setAppLaunching(String packageName) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.setAppLaunching(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "setAppLaunching failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "setAppLaunching(): secure apps service not started!");
    }

    private void addAppToList(String packageName) {
        if (!mEnabled.get()) {
            return;
        }

        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.addAppToList(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "addAppToList failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "addAppToList(): secure apps service not started!");
    }

    private void removeAppFromList(String packageName) {
        if (!mEnabled.get()) {
            return;
        }

        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.removeAppFromList(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "removeAppFromList failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "removeAppFromList(): secure apps service not started!");
    }

    public void launchBeforeActivity(String packageName) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.launchBeforeActivity(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "launchBeforeActivity failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "launchBeforeActivity(): secure apps service not started!");
    }

    public void setAppIntent(String packageName, Intent intent) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.setAppIntent(packageName, intent);
            } catch (Exception e) {
                Slog.e(TAG, "setAppIntent failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "setAppIntent(): secure apps service not started!");
    }

    private boolean isAppSecured(String packageName) {
        if (!mEnabled.get()) {
            return false;
        }

        boolean isAppSecured = false;
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                isAppSecured = service.isAppSecured(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "isAppSecured failed", e);
            }
            return isAppSecured;
        }
        bind(mUserId);
        Slog.w(TAG, "isAppSecured(): secure apps service not started!");
        return isAppSecured;
    }

    private int getSecuredAppsCount() {
        int count = 0;
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                count = service.getSecuredAppsCount();
            } catch (Exception e) {
                Slog.e(TAG, "getSecuredAppsCount failed", e);
            }
            return count;
        }
        bind(mUserId);
        Slog.w(TAG, "getSecuredAppsCount(): secure apps service not started!");
        return count;
    }

    private void addSecureAppsCallback(ISecureAppsCallback callback) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.addSecureAppsCallback(callback);
            } catch (Exception e) {
                Slog.e(TAG, "addSecureAppsCallback failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "addSecureAppsCallback(): secure apps service not started!");
    }

    private void removeSecureAppsCallback(ISecureAppsCallback callback) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.removeSecureAppsCallback(callback);
            } catch (Exception e) {
                Slog.e(TAG, "removeSecureAppsCallback failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "removeSecureAppsCallback(): secure apps service not started!");
    }

    public void onWindowsDrawn(String packageName) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.onWindowsDrawn(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "onWindowsDrawn failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "onWindowsDrawn(): secure apps service not started!");
    }

    public void onAppWindowRemoved(String packageName, WindowState w) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.onAppWindowRemoved(packageName, w);
            } catch (Exception e) {
                Slog.e(TAG, "onAppWindowRemoved failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "onAppWindowRemoved(): secure apps service not started!");
    }

    public void updateAppVisibility(String packageName) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.updateAppVisibility(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "updateAppVisibility failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "updateAppVisibility(): secure apps service not started!");
    }

    public void promptIfNeeded(String packageName, WindowState w) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.promptIfNeeded(packageName, w);
            } catch (Exception e) {
                Slog.e(TAG, "promptIfNeeded failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "promptIfNeeded(): secure apps service not started!");
    }

    public boolean isGame(String packageName) {
        boolean isGame = false;
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                isGame = service.isGame(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "isGame failed", e);
            }
            return isGame;
        }
        bind(mUserId);
        Slog.w(TAG, "isGame(): secure apps service not started!");
        return isGame;
    }

    public boolean isAppOpen(String packageName) {
        boolean isAppOpen = false;
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                isAppOpen = service.isAppOpen(packageName);
            } catch (Exception e) {
                Slog.e(TAG, "isAppOpen failed", e);
            }
            return isAppOpen;
        }
        bind(mUserId);
        Slog.w(TAG, "isAppOpen(): secure apps service not started!");
        return isAppOpen;
    }

    public void updateAppLockConfig(String packageName, Configuration newConfig) {
        ISecureAppsService service = getService();
        if (service != null) {
            try {
                service.updateSecureAppsConfig(packageName, newConfig);
            } catch (Exception e) {
                Slog.e(TAG, "updateAppLockConfig failed", e);
            }
            return;
        }
        bind(mUserId);
        Slog.w(TAG, "updateAppLockConfig(): secure apps service not started!");
    }

    private boolean bind(int userId) {
        Slog.d(TAG, "bind");
        if (mBound) {
            Slog.d(TAG, "Sense service is binding");
            return true;
        } else {
            if (mUserId != UserHandle.USER_NULL && getService() == null) {
                if (createService()) {
                    return true;
                }
                return false;
            }
            return false;
        }
    }

    private boolean createService() {
        try {
            Intent intent = getServiceIntent();
            if (intent == null) {
                Slog.d(TAG, "Sense service not found");
                return false;
            }
            boolean result = mContext.bindServiceAsUser(intent, new SecureAppsServiceConnection(mUserId), 65, UserHandle.of(mUserId));
            if (result) {
                mBound = true;
            }
            return result;
        } catch (Exception e) {
            Slog.e(TAG, "bind failed", e);
        }
        return false;
    }

    public ISecureAppsService getService() {
        return mServices.get(mUserId);
    }

    private Intent getServiceIntent() {
        Intent intent = new Intent("secure_apps:remote");
        intent.setComponent(ComponentName.unflattenFromString(
                "com.android.systemui/com.android.systemui.secureapps.SecureAppsService"));
        return intent;
    }

    private class SecureAppsHandler extends Handler {
        public static final int MSG_INIT_APPS = 0;
        public static final int MSG_READ_STATE = 1;

        public SecureAppsHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_INIT_APPS:
                    initLockedApps();
                    break;
                case MSG_READ_STATE:
                    readState();
                    break;
                default:
                    Slog.w(TAG, "Unknown message:" + msg.what);
            }
        }
    }

    private class SecureAppsImpl extends ISecureAppsManagerService.Stub {
        @Override
        public void addAppToList(String packageName) {
            SecureAppsManagerService.this.addAppToList(packageName);
        }

        @Override
        public void removeAppFromList(String packageName) {
            SecureAppsManagerService.this.removeAppFromList(packageName);
        }

        @Override
        public boolean isAppSecured(String packageName) {
            return SecureAppsManagerService.this.isAppSecured(packageName);
        }

        @Override
        public int getSecuredAppsCount() {
            return SecureAppsManagerService.this.getSecuredAppsCount();
        }

        @Override
        public void addSecureAppsCallback(ISecureAppsCallback callback) {
            SecureAppsManagerService.this.addSecureAppsCallback(callback);
        }

        @Override
        public void removeSecureAppsCallback(ISecureAppsCallback callback) {
            SecureAppsManagerService.this.removeSecureAppsCallback(callback);
        }
    };

    private class SecureAppsServiceConnection implements ServiceConnection {
        int mUserId;

        public SecureAppsServiceConnection(int userId) {
            mUserId = userId;
        }

        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            Slog.d(TAG, "Sense service connected");
            ISecureAppsService service = ISecureAppsService.Stub.asInterface(service);
            if (service != null) {
                synchronized (mServices) {
                    try {
                        service.asBinder().linkToDeath(new IBinder.DeathRecipient() {
                            @Override
                            public void binderDied() {
                                Slog.e(TAG, "Sense service binder died");
                                mServices.remove(mUserId);
                                if (mUserId == mCurrentUserId) {
                                    bind(mUserId);
                                }
                            }
                        }, 0);
                        mServices.put(mUserId, service);
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    mBound = false;
                }
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            Slog.d(TAG, "Sense service disconnected");
            mServices.remove(mUserId);
            mBound = false;
            if (mUserId == mCurrentUserId) {
                bind(mUserId);
            }
        }
    }
}
