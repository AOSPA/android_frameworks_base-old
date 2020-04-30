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

package com.android.server.wm;

import static com.android.server.wm.ActivityTaskManagerDebugConfig.DEBUG_APPLOCK;
import static com.android.server.wm.ActivityTaskManagerDebugConfig.POSTFIX_APPLOCK;

import android.app.admin.DevicePolicyManager;
import android.app.ActivityManager;
import android.app.IAppLockService;
import android.app.IAppLockCallback;
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
import com.android.server.DisplayThread;
import com.android.server.LocalServices;
import com.android.server.SystemService;
//import com.android.server.wm.ActivityTaskManagerService;
//import com.android.server.wm.WindowState;

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

public class AppLockService extends SystemService {

    private static final String TAG = "AppLockService";
    private static final String TAG_APPLOCK = TAG + POSTFIX_APPLOCK;

    private static final String FILE_NAME = "locked-apps.xml";
    private static final String TAG_LOCKED_APPS = "locked-apps";
    private static final String TAG_PACKAGE = "package";
    private static final String TAG_EXTRA = "extra";
    private static final String ATTRIBUTE_NAME = "name";
    private final String[] GAMES = {"com"};

    protected final ArraySet<String> GAMES_SET = new ArraySet<> (Arrays.asList(GAMES));

    private AtomicBoolean mEnabled;

    private AppLockContainer mCurrent;
    private FingerprintManager mFingerprintManager;
    private PackageManager mPackageManager;
    private CancellationSignal mCancellationSignal;
    private boolean mFingerprintListening;
    private boolean mStartFingerprint;

    private int mUserHandle;
    private int mUserId;
    private UserManager mUserManager;
    private boolean mShowOnlyOnWake;
    private SettingsObserver mSettingsObserver;
    
    private final LockPatternUtils mLockPatternUtils;
    private ActivityTaskManagerService mAtmService;
    private Context mContext;

    private AtomicFile mFile;
    private final AppLockHandler mHandler;
    private final Handler mDisplayHandler;
    private final Object mLock = new Object();

    private final ArrayMap<String, AppLockContainer> mAppsList = new ArrayMap<>();
    private final ArraySet<String> mOpenedApplicationsIndex = new ArraySet<>();
    private final ArraySet<IAppLockCallback> mCallbacks= new ArraySet<>();

    private final AuthenticationCallback mFingerprintCallback =
            new AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Slog.v(TAG, "onAuthenticationError() pkg:" + mCurrent.mPackageName + " Id=" + errMsgId + " Name=" + errString  + " isCancelled:" + mCancellationSignal.isCanceled());
            if (errString.toString().equals("Fingerprint operation cancelled.")) {  
                mFingerprintListening = false;
                if (mStartFingerprint || !mCancellationSignal.isCanceled()) {
                    Slog.v(TAG, "restarting fingerprint");
                    startListeningForFingerprint();
                    mStartFingerprint = false;
                }
            }
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Slog.v(TAG, "onAuthenticationHelp");
            Slog.v(TAG, "Help: Id=" + helpMsgId + " Name=" + helpString);
        }

        @Override
        public void onAuthenticationFailed() {
            Slog.v(TAG, "onAuthenticationFailed");
            mFingerprintListening = false;
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            Slog.v(TAG, "onAuthenticationSucceeded");
            mCurrent.mAppLockUi.onUnlockSucceed();
            mFingerprintListening = false;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "Package removed intent received");
                final Uri data = intent.getData();
                if (data == null) {
                    if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK,
                            "Cannot handle package broadcast with null data");
                    return;
                }

                final String packageName = data.getSchemeSpecificPart();
                removeAppFromList(packageName);
            }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                clearOpenedAppsList();
                stopListeningForFingerprint();
            }
        }
    };

    public AppLockService(Context context) {
        super(context);

        mContext = context;
        mHandler = new AppLockHandler(BackgroundThread.getHandler().getLooper());
        mDisplayHandler = DisplayThread.getHandler();
        mUserId = ActivityManager.getCurrentUser();
        mUserManager = UserManager.get(context);
        mEnabled = new AtomicBoolean(!mUserManager.isManagedProfile(mUserId)
                && mUserManager.isUserUnlockingOrUnlocked(mUserId));
        mLockPatternUtils = new LockPatternUtils(context);

        IntentFilter packageFilter = new IntentFilter();
        packageFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packageFilter.addDataScheme("package");
        context.registerReceiver(mReceiver, packageFilter);

        IntentFilter screenOffFilter = new IntentFilter();
        screenOffFilter.addAction(Intent.ACTION_SCREEN_OFF);
        context.registerReceiver(mReceiver, screenOffFilter);

        mSettingsObserver = new SettingsObserver(mHandler);
        mSettingsObserver.observe();

        mHandler.sendEmptyMessage(AppLockHandler.MSG_READ_STATE);
    }

    @Override
    public void onStart() {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "Starting AppLockService");
        publishBinderService(Context.APPLOCK_SERVICE, new AppLockImpl());
        publishLocalService(AppLockService.class, this);
    }

    @Override
    public void onUnlockUser(int userHandle) {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onUnlockUser()");
        mUserHandle = userHandle;
        mHandler.sendEmptyMessage(AppLockHandler.MSG_INIT_APPS);
        mFingerprintManager = (FingerprintManager) mContext.
                getSystemService(Context.FINGERPRINT_SERVICE);
        mPackageManager = mContext.getPackageManager();
    }

    @Override
    public void onSwitchUser(int userHandle) {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onSwitchUser()");
        mUserHandle = userHandle;
        mHandler.sendEmptyMessage(AppLockHandler.MSG_INIT_APPS);
    }

    @Override
    public void onStopUser(int userHandle) {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onStopUser()");
        mEnabled.set(false);
    }

    private void initLockedApps() {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "initLockedApps(" + mUserHandle + ")");
        if (mUserManager.isManagedProfile(mUserHandle)) {
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "Disabled");
            mEnabled.set(false);
        } else {
            mUserId = mUserHandle;
            mFile = new AtomicFile(getFile());
            AppLockUi.setLockPatternUtils(mLockPatternUtils);
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "Enabled");
            mEnabled.set(true);
            readState();
            clearOpenedAppsList();
        }
    }

    private File getFile() {
        File file = new File(Environment.getDataSystemCeDirectory(mUserId), FILE_NAME);
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "getFile(): " + file.getAbsolutePath());
        return file;
    }

    private void readState() {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "readState()");
        if (!mEnabled.get()) {
            return;
        }
        try (FileInputStream in = mFile.openRead()) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, null);
            parseXml(parser);
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "Read locked-apps.xml successfully");
        } catch (FileNotFoundException e) {
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "locked-apps.xml not found");
            Slog.i(TAG, "locked-apps.xml not found");
        } catch (XmlPullParserException | IOException e) {
            throw new IllegalStateException("Failed to parse locked-apps.xml: " + mFile, e);
        }
    }

    private void parseXml(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        int type;
        int depth;
        int innerDepth = parser.getDepth() + 1;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (depth > innerDepth || type != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals(TAG_LOCKED_APPS)) {
                parsePackages(parser);
                return;
            }
        }
        Slog.w(TAG, "Missing <" + TAG_LOCKED_APPS + "> in locked-apps.xml");
    }

    private void parsePackages(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        mAppsList.clear();
        int type;
        int depth;
        int innerDepth = parser.getDepth() + 1;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && ((depth = parser.getDepth()) >= innerDepth || type != XmlPullParser.END_TAG)) {
            if (depth > innerDepth || type != XmlPullParser.START_TAG) {
                continue;
            }
            if (parser.getName().equals(TAG_PACKAGE)) {
                String pkgName = parser.getAttributeValue(null, ATTRIBUTE_NAME);
                AppLockContainer cont = new AppLockContainer(new AppLockUi(getContext(), pkgName,
                        mUserId, mAtmService), pkgName);
                mAppsList.put(pkgName, cont);
                if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "parsePackages(): pkgName=" + pkgName);
            }
        }
    }

    private void writeState() {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "writeState()");
        if (!mEnabled.get()) {
            return;
        }
        ArraySet<String> packages = snapshotPackages();
        FileOutputStream out = null;
        try {
            out = mFile.startWrite();
            XmlSerializer serializer = Xml.newSerializer();
            serializer.setOutput(out, StandardCharsets.UTF_8.name());
            serializer.setFeature(
                    "http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startDocument(null, true);
            serializeLockedApps(serializer, packages);
            serializer.endDocument();
            mFile.finishWrite(out);
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "Wrote locked-apps.xml successfully");
        } catch (IllegalArgumentException | IllegalStateException | IOException e) {
            Slog.wtf(TAG, "Failed to write locked-apps.xml, restoring backup", e);
            if (out != null) {
                mFile.failWrite(out);
            }
        } finally {
            IoUtils.closeQuietly(out);
        }
    }

    private void serializeLockedApps(XmlSerializer serializer,
            ArraySet<String> packages) throws IOException {
        serializer.startTag(null, TAG_LOCKED_APPS);
        for (int i = 0, size = packages.size(); i < size; ++i) {
            String pkgName = packages.valueAt(i);
            //ArraySet<String> extras = packages.valueAt(i);
            serializer.startTag(null, TAG_PACKAGE);
            serializer.attribute(null, ATTRIBUTE_NAME, pkgName);
            //serializeLockedAppExtras(serializer, extras);
            serializer.endTag(null, TAG_PACKAGE);
        }
        serializer.endTag(null, TAG_LOCKED_APPS);
    }

    private ArraySet<String> snapshotPackages() {
        ArraySet<String> packages = new ArraySet<>();
        for (int i = 0, size = CollectionUtils.size(mAppsList); i < size; ++i) {
            String pkgName = mAppsList.keyAt(i);
            packages.add(pkgName);
        }
        return packages;
    }

    private void addAppToList(String packageName) {
        if (!mEnabled.get()) {
            return;
        }
        if (DEBUG_APPLOCK) Slog.v(TAG, "addAppToList packageName:" + packageName);
        if (!mAppsList.containsKey(packageName)) {
            AppLockContainer cont = new AppLockContainer(new AppLockUi(getContext(), packageName,
                    mUserId, mAtmService), packageName);
            mAppsList.put(packageName, cont);
            mHandler.sendEmptyMessage(AppLockHandler.MSG_WRITE_STATE);
        }
    }

    private void removeAppFromList(String packageName) {
        if (!mEnabled.get()) {
            return;
        }
        if (mAppsList.containsKey(packageName)) {
            mAppsList.remove(packageName);
            mHandler.sendEmptyMessage(AppLockHandler.MSG_WRITE_STATE);
        }
    }

    public boolean isAppLocked(String packageName) {
        if (!mEnabled.get()) {
            return false;
        }
        return mAppsList.containsKey(packageName);
    }

    private AppLockContainer getAppLockContainer(String packageName) {
        if (!mEnabled.get()) {
            return null;
        }
        return mAppsList.get(packageName);
    }

    private void clearOpenedAppsList() {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "clearOpenedAppsList()");
        for (String p : mOpenedApplicationsIndex) {
            dispatchCallbacks(p, false);
        }
        mOpenedApplicationsIndex.clear();
    }

    public boolean isGame(String packageName) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null) {
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "isGame() pkg:" + packageName);
            return cont.mIsGame;
        }
        return false;
    }

    public boolean isAppOpen(String packageName) {
        return mOpenedApplicationsIndex.contains(packageName);
    }

    void removeOpenedApp(String packageName) {
        if (isAppOpen(packageName)) {
            Slog.v(TAG_APPLOCK, "removeOpenedApp(" + packageName + ")");
            mOpenedApplicationsIndex.remove(packageName);
            dispatchCallbacks(packageName, false);
        }
    }

    void addOpenedApp(String packageName) {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "addOpenedApp(" + packageName + ")");
        mOpenedApplicationsIndex.add(packageName);
        AppLockContainer cont = getAppLockContainer(packageName);
        cont.unFreezeApp();
        if (isGame(packageName)) {
            Slog.v(TAG_APPLOCK, "startActivityAfterUnlock(" + packageName + ")");
            cont.startActivityAfterUnlock();
        }
        dispatchCallbacks(packageName, true);
    }

    public void updateAppLockConfig(String packageName, Configuration newConfig) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null && isSecure()) {
            cont.mLayoutChanged = cont.mAppLockUi.updateConfig(newConfig);
        }
    }

    public void launchBeforeActivity(String packageName) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null) {
            mCurrent = cont;
            mDisplayHandler.postAtFrontOfQueue(() -> {
                if (isSecure()) {
                    Slog.v(TAG_APPLOCK, "launchBeforeActivity(" + packageName + ")");
                    cont.mAppLockUi.launchBeforeActivity();
                    if (mFingerprintListening) {
                        stopListeningForFingerprint();
                        mStartFingerprint = true;
                    } else {
                        startListeningForFingerprint();
                    }
                }
            });
        }
    }

    public void setAppIntent(String packageName, Intent intent) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null) {
            cont.intent = intent;
        }
    }

    public void setActivityTaskManagerService(ActivityTaskManagerService s) {
        mAtmService = s;
    }

    public void onAppLaunching(String packageName) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null && isSecure()) {
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onAppLaunching()");
            if (!isAppOpen(packageName)) {
                mCurrent = cont;
                cont.freezeApp();
                cont.mAppLockUi.onAppLaunching();
            }
            if (mHandler.hasMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName)) {
                if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onAppLaunching() removeMessage: MSG_REMOVE_OPENED_APP");
                mHandler.removeMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName);
            }
        }
    }

    public void showAppLockIfNeeded(String packageName, WindowState w) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null && isSecure()) {
            if (!isAppOpen(packageName)) {
                cont.addActivityRecord(w.mAppToken.mActivityRecord);
                cont.freezeApp();
                cont.mAppLockUi.show(w);
            } else {
                cont.mAppLockUi.updateAppWindow(w);
            }
            if (mHandler.hasMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName)) {
                if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "showAppLockIfNeeded() removeMessage: MSG_REMOVE_OPENED_APP");
                mHandler.removeMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName);
            }
        }
    }

    public void onWindowsDrawn(String packageName) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null && isSecure() && !isAppOpen(packageName)) {
            mCurrent = cont;
            cont.freezeApp();
            mDisplayHandler.post(() -> {
                if (mFingerprintListening) {
                    stopListeningForFingerprint();
                    mStartFingerprint = true;
                } else {
                    startListeningForFingerprint();
                }
            });
        }
    }

    public void onAppWindowRemoved(String packageName, WindowState w) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null) {
            if (DEBUG_APPLOCK) Slog.d(TAG_APPLOCK, "onAppWindowRemoved() " + w);
            cont.mAppLockUi.setAppWindowGone(w);
            cont.removeActivityRecord(w.mAppToken.mActivityRecord);
            mDisplayHandler.post(() -> {
                boolean visible = cont.mAppLockUi.isAppWindowsVisible();
                if (!visible) {
                    if (mCurrent == cont && !cont.mLayoutChanged) {
                        stopListeningForFingerprint();
                        mStartFingerprint = false;
                    }
                    cont.mLayoutChanged = false;
                    if (isAppOpen(packageName)) {
                        if (mHandler.hasMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName)) {
                            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onAppWindowRemoved() removeMessage: MSG_REMOVE_OPENED_APP");
                            mHandler.removeMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName);
                        }
                        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onAppWindowRemoved() sendMessage: MSG_REMOVE_OPENED_APP");
                        final Message msg = mHandler.obtainMessage(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName);
                        mHandler.sendMessageDelayed(msg, 10000);
                    }
                }
            });
        }
    }

    public void updateAppVisibility(String packageName) {
        AppLockContainer cont = getAppLockContainer(packageName);
        if (cont != null) {
            if (mHandler.hasMessages(AppLockHandler.MSG_UPDATE_VISIBILITY, cont)) {
                mHandler.removeMessages(AppLockHandler.MSG_UPDATE_VISIBILITY, cont);
            }
            final Message msg = mHandler.obtainMessage(AppLockHandler.MSG_UPDATE_VISIBILITY, cont);
            mHandler.sendMessage(msg);
        }
    }

    private void startListeningForFingerprint() {
        if (mFingerprintListening) return;
        if (DEBUG_APPLOCK) Slog.v(TAG, "startListeningForFingerprint(): packageName=" + mCurrent.mPackageName);
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        mFingerprintListening = true;
        if (mCancellationSignal == null || mCancellationSignal.isCanceled()) {
            mCancellationSignal = new CancellationSignal();
        }
        mFingerprintManager.authenticate(null, mCancellationSignal, 0,
                mFingerprintCallback, mDisplayHandler);
    }

    public void stopListeningForFingerprint() {
        if (!mFingerprintListening) return;
        if (DEBUG_APPLOCK) Slog.v(TAG, "stopListeningForFingerprint(): packageName=" + mCurrent.mPackageName + " isCanceled:" + mCancellationSignal.isCanceled());
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
        }
    }

    private boolean isFingerprintAuthAvailable() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                && mFingerprintManager.hasEnrolledFingerprints(mUserId);
    }

    private boolean isSecure() {
        int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId);
        switch (storedQuality) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                return true;
            default:
                return false;
        }
    }

    public void notifyTimerTickForAll() {
        mAppsList.forEach((k, v) -> {
            AppLockContainer cont = (AppLockContainer) v;
            if (cont != null && cont.mAppLockUi.isShowing()) {
                cont.mAppLockUi.notifyTimerTick();
            }
        });
    }

    public void notifyTimerStartForAll() {
        mAppsList.forEach((k, v) -> {
            AppLockContainer cont = (AppLockContainer) v;
            if (cont != null && cont.mAppLockUi.isShowing()) {
                cont.mAppLockUi.notifyTimerStart();
            }
        });
    }

    public void notifyTimerStopForAll() {
        mAppsList.forEach((k, v) -> {
            AppLockContainer cont = (AppLockContainer) v;
            if (cont != null && cont.mAppLockUi.isShowing()) {
                cont.mAppLockUi.notifyTimerStop();
            }
        });
    }

    private int getLockedAppsCount() {
        if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "Number of locked apps: " + mAppsList.size());
        return mAppsList.size();
    }

    private void dispatchCallbacks(String packageName, boolean opened) {
        mHandler.post(() -> {
            synchronized (mCallbacks) {
                final int N = mCallbacks.size();
                boolean cleanup = false;
                for (int i = 0; i < N; i++) {
                    final IAppLockCallback callback = mCallbacks.valueAt(i);
                    try {
                        if (callback != null) {
                            callback.onAppStateChanged(packageName, opened);
                        } else {
                            cleanup = true;
                        }
                    } catch (RemoteException e) {
                        cleanup = true;
                    }
                }
                if (cleanup) {
                    cleanUpCallbacksLocked(null);
                }
            }
        });
    }

    private void cleanUpCallbacksLocked(IAppLockCallback callback) {
        mHandler.post(() -> {
            synchronized (mCallbacks) {
                for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                    IAppLockCallback found = mCallbacks.valueAt(i);
                    if (found == null || found == callback) {
                        mCallbacks.remove(i);
                    }
                }
            }
        });
    }

    private void addAppLockCallback(IAppLockCallback callback) {
        mHandler.post(() -> {
            synchronized(mCallbacks) {
                if (!mCallbacks.contains(callback)) {
                    mCallbacks.add(callback);
                }
            }
        });
    }

    private void removeAppLockCallback(IAppLockCallback callback) {
        mHandler.post(() -> {
            synchronized(mCallbacks) {
                if (mCallbacks.contains(callback)) {
                    mCallbacks.remove(callback);
                }
            }
        });
    }

    private class SettingsObserver extends ContentObserver {

        SettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.APP_LOCK_SHOW_ONLY_ON_WAKE), false, this,
                UserHandle.USER_ALL);
            mShowOnlyOnWake = Settings.System.getIntForUser(mContext
                .getContentResolver(),
                Settings.System.APP_LOCK_SHOW_ONLY_ON_WAKE, 0,
                UserHandle.USER_CURRENT) != 0;
        }

        @Override
        public void onChange(boolean selfChange) {
            mShowOnlyOnWake = Settings.System.getIntForUser(mContext
                    .getContentResolver(),
                    Settings.System.APP_LOCK_SHOW_ONLY_ON_WAKE, 0,
                    UserHandle.USER_CURRENT) != 0;
            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "onChange: " + mShowOnlyOnWake);
        }
    }

    private class AppLockImpl extends IAppLockService.Stub {
        @Override
        public void addAppToList(String packageName) {
            AppLockService.this.addAppToList(packageName);
        }

        @Override
        public void addAppExtraToList(String packageName, String extraName) {
            //AppLockService.this.addAppExtraToList(packageName, extraName);
        }

        @Override
        public void removeAppFromList(String packageName) {
            AppLockService.this.removeAppFromList(packageName);
        }

        @Override
        public void removeAppExtraFromList(String packageName, String extraName) {
            //AppLockService.this.removeAppExtraFromList(packageName, extraName);
        }

        @Override
        public boolean isAppLocked(String packageName) {
            return AppLockService.this.isAppLocked(packageName);
        }

        @Override
        public boolean hasAppExtra(String packageName, String extraName) {
            return false;//AppLockService.this.hasAppExtra(packageName, extraName);
        }

        @Override
        public int getLockedAppsCount() {
            return AppLockService.this.getLockedAppsCount();
        }

        @Override
        public void addAppLockCallback(IAppLockCallback callback) {
            AppLockService.this.addAppLockCallback(callback);
        }

        @Override
        public void removeAppLockCallback(IAppLockCallback callback) {
            AppLockService.this.removeAppLockCallback(callback);
        }
    };

    private class AppLockHandler extends Handler {

        public static final int MSG_INIT_APPS = 0;
        public static final int MSG_READ_STATE = 1;
        public static final int MSG_WRITE_STATE = 2;
        public static final int MSG_REMOVE_OPENED_APP = 3;
        public static final int MSG_UPDATE_VISIBILITY = 4;

        public AppLockHandler(Looper looper) {
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
                case MSG_WRITE_STATE:
                    writeState();
                    break;
                case MSG_REMOVE_OPENED_APP:
                    if (!mShowOnlyOnWake) {
                        removeOpenedApp((String) msg.obj);
                    }
                    break;
                case MSG_UPDATE_VISIBILITY:
                    AppLockContainer cont = (AppLockContainer) msg.obj;
                    boolean visible = cont.mAppLockUi.isAppWindowsVisible();
                    //if (DEBUG_APPLOCK) Slog.d(TAG_APPLOCK, "updateAppVisibility() pkg:" + cont.mPackageName + " is Visible:" + visible);
                    if (!visible) {
                        cont.mAppLockUi.setAppWindowGone(null);
                        //if (DEBUG_APPLOCK) Slog.d(TAG_APPLOCK, "updateAppVisibility() pkg:" + cont.mPackageName + " is not visible");
                        if (mCurrent == cont && !cont.mLayoutChanged) {
                            stopListeningForFingerprint();
                            mStartFingerprint = false;
                        }
                        cont.mLayoutChanged = false;
                        if (isAppOpen(cont.mPackageName)) {
                            if (mHandler.hasMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName)) {
                                if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "updateAppVisibility() removeMessage: MSG_REMOVE_OPENED_APP");
                                mHandler.removeMessages(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName);
                            }
                            if (DEBUG_APPLOCK) Slog.v(TAG_APPLOCK, "updateAppVisibility() sendMessage: MSG_REMOVE_OPENED_APP");
                            final Message msgRemove = mHandler.obtainMessage(AppLockHandler.MSG_REMOVE_OPENED_APP, cont.mPackageName);
                            mHandler.sendMessageDelayed(msgRemove, 10000);
                        }
                    }
                    break;
                default:
                    Slog.w(TAG, "Unknown message:" + msg.what);
            }
        }
    }

    private class AppLockContainer {
        protected final String mPackageName;
        protected final AppLockUi mAppLockUi;
        protected boolean mLayoutChanged;
        protected final boolean mIsGame;
        protected Intent intent;
        protected final ArraySet<ActivityRecord> activityRecord = new ArraySet<>();

        public AppLockContainer (AppLockUi ui, String pkg) {
            mPackageName = pkg;
            mAppLockUi = ui;
            mLayoutChanged = false;
            mIsGame = mAppLockUi.isGame() || GAMES_SET.contains(mPackageName);
            if (mIsGame) {
                mAppLockUi.setIsGame();
            }
        }

        public void startActivityAfterUnlock () {
            Slog.d(TAG, "startActivityAfterUnlock() intent:" + intent);
            if (intent != null) mContext.startActivity(intent);
        }

        public void addActivityRecord (ActivityRecord r) {
            Slog.d(TAG, "addActivityRecord() activityRecord:" + r);
            activityRecord.add(r);
        }

        public void removeActivityRecord (ActivityRecord r) {
            Slog.d(TAG, "removeActivityRecord() activityRecord:" + r);
            activityRecord.remove(r);
        }

        public void freezeApp () {
            for (ActivityRecord r : activityRecord) {
                r.freezeApp();
            }
        }

        public void unFreezeApp () {
            for (ActivityRecord r : activityRecord) {
                r.unFreezeApp();
            }
        }
    }
}
