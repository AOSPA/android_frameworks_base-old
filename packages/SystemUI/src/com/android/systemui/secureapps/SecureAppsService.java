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

import android.app.admin.DevicePolicyManager;
import android.app.ActivityManager;
import android.app.ISecureAppsService;
import android.app.ISecureAppsCallback;
import android.app.IApplicationThread;
import android.app.ProfilerInfo;
import android.app.Service;
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
import android.util.Log;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.os.BackgroundThread;
import com.android.internal.util.CollectionUtils;
import com.android.internal.widget.LockPatternUtils;

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

public class SecureAppsService extends Service {

    private static final String TAG = "SecureAppsService";

    private static final String FILE_NAME = "locked-apps.xml";
    private static final String TAG_LOCKED_APPS = "locked-apps";
    private static final String TAG_PACKAGE = "package";
    private static final String ATTRIBUTE_NAME = "name";
    private final String[] GAMES = {"com.tencent.ig"};
    protected final ArraySet<String> GAMES_SET = new ArraySet<> (Arrays.asList(GAMES));

    private final ArrayMap<String, SecureAppsInfo> mAppsList = new ArrayMap<>();
    private final ArraySet<ISecureAppsCallback> mCallbacks= new ArraySet<>();
    private final ArraySet<String> mOpenedApplicationsIndex = new ArraySet<>();

    private AtomicBoolean mEnabled;
    private AtomicFile mFile;
    private CancellationSignal mCancellationSignal;
    private Context mContext;
    private FingerprintManager mFingerprintManager;
    private PackageManager mPackageManager;
    private RemoteWrapper mService;
    private SecureAppsHandler mHandler;
    private SecureAppsInfo mSecureAppsInfo;

    private SettingsObserver mSettingsObserver;

    private boolean mFingerprintListening;
    private boolean mShowOnlyOnWake;
    private boolean mStartFingerprint;
    private int mUserId;

    private final AuthenticationCallback mFingerprintCallback =
            new AuthenticationCallback() {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
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
            mSecureAppsInfo.mAppLockUi.onUnlockSucceed();
            mFingerprintListening = false;
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())
                    && !intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) {
                final Uri data = intent.getData();
                if (data == null) {
                    Slog.v(TAG, "Cannot handle package broadcast with null data");
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

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "is bound");
        return mService;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Setting up remote");
        mContext = this;
        mService = new RemoteWrapper();
        mHandler = new SecureAppsHandler(BackgroundThread.getHandler().getLooper());
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        mContext.registerReceiver(mReceiver, filter);
        mSettingsObserver = new SettingsObserver(this, mHandler);
        mSettingsObserver.observe();
    }

    public void initSecureApps(int userId) {
        mUserId = userId;
        mHandler.sendEmptyMessage(SecureAppsHandler.MSG_INIT_APPS);
        mFingerprintManager = (FingerprintManager) mContext.
                getSystemService(Context.FINGERPRINT_SERVICE);
        mPackageManager = mContext.getPackageManager();
    }

    public void handleSecureApps() {
        mFile = new AtomicFile(getFile());
        readSecuredApps(mEnabled.get());
        clearOpenedAppsList();
    }

    private File getFile() {
        File file = new File(Environment.getDataSystemCeDirectory(mUserId), FILE_NAME);
        return file;
    }

    private void readSecuredApps(boolean isUserUnlocked) {
        mEnabled = new AtomicBoolean(isUserUnlocked);
        if (!mEnabled.get()) {
            return;
        }

        try (FileInputStream in = mFile.openRead()) {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(in, null);
            parseXml(parser);
        } catch (FileNotFoundException e) {
            Log.e(TAG, "locked-apps.xml not found");
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
        Log.w(TAG, "Missing <" + TAG_LOCKED_APPS + "> in locked-apps.xml");
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
                SecureAppsInfo info = new SecureAppsInfo(new AppLockUi(this, mContext, pkgName,
                        mUserId), pkgName);
                mAppsList.put(pkgName, info);
            }
        }
    }

    public void writeSecuredApps() {
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
            serializer.startTag(null, TAG_PACKAGE);
            serializer.attribute(null, ATTRIBUTE_NAME, pkgName);
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

    public void addAppToList(String packageName) {
        if (!mEnabled.get()) {
            return;
        }
        if (!mAppsList.containsKey(packageName)) {
            SecureAppsInfo info = new SecureAppsInfo(new AppLockUi(this, mContext, packageName,
                    mUserId), packageName);
            mAppsList.put(packageName, info);
            mHandler.sendEmptyMessage(SecureAppsHandler.MSG_WRITE_STATE);
        }
    }

    public void removeAppFromList(String packageName) {
        if (!mEnabled.get()) {
            return;
        }
        if (mAppsList.containsKey(packageName)) {
            mAppsList.remove(packageName);
            mHandler.sendEmptyMessage(SecureAppsHandler.MSG_WRITE_STATE);
        }
    }

    public boolean isAppSecured(String packageName) {
        if (!mEnabled.get()) {
            return false;
        }
        return mAppsList.containsKey(packageName);
    }

    public int getSecuredAppsCount() {
        return mAppsList.size();
    }

    public void setAppLaunching(String packageName) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null && isSecure()) {
            if (!isAppOpen(packageName)) {
                mSecureAppsInfo = info;
                info.mAppLockUi.onAppLaunching();
            }

            if (mHandler.hasMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName)) {
                mHandler.removeMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName);
            }
        }
    }

    public void launchBeforeActivity(String packageName) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null) {
            mSecureAppsInfo = info;
            mHandler.postAtFrontOfQueue(() -> {
                if (isSecure()) {
                    info.mAppLockUi.launchBeforeActivity();
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
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null) {
            info.intent = intent;
        }
    }

    private void startListeningForFingerprint() {
        if (mFingerprintListening) return;
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        mFingerprintListening = true;
        if (mCancellationSignal == null || mCancellationSignal.isCanceled()) {
            mCancellationSignal = new CancellationSignal();
        }
        mFingerprintManager.authenticate(null, mCancellationSignal, 0,
                mFingerprintCallback, mHandler);
    }

    public void stopListeningForFingerprint() {
        if (!mFingerprintListening) return;
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
        }
    }

    private boolean isFingerprintAuthAvailable() {
        return mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
                && mFingerprintManager.hasEnrolledFingerprints(mUserId);
    }

    public boolean isAppOpen(String packageName) {
        return mOpenedApplicationsIndex.contains(packageName);
    }

    public void addOpenedApp(String packageName) {
        dispatchCallbacks(packageName, true);
        mOpenedApplicationsIndex.add(packageName);
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null && isGame(packageName)) {
            Slog.v(TAG, "startActivityAfterUnlock(" + packageName + ")");
            info.startActivityAfterUnlock();
        }
    }

    public void removeOpenedApp(String packageName) {
        if (isAppOpen(packageName)) {
            mOpenedApplicationsIndex.remove(packageName);
            dispatchCallbacks(packageName, false);
        }
    }

    public void onWindowsDrawn(String packageName) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null && isSecure() && !isAppOpen(packageName)) {
            mSecureAppsInfo = info;
            mHandler.post(() -> {
                if (mFingerprintListening) {
                    stopListeningForFingerprint();
                    mStartFingerprint = true;
                } else {
                    startListeningForFingerprint();
                }
            });
        }
    }

    public void onAppWindowRemoved(String packageName, IBinder token) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null) {
            info.mAppLockUi.setAppWindowGone(token);
            mHandler.post(() -> {
                boolean visible = info.mAppLockUi.isAppWindowsVisible();
                if (!visible) {
                    if (mSecureAppsInfo == info && !info.mLayoutChanged) {
                        stopListeningForFingerprint();
                        mStartFingerprint = false;
                    }
                    info.mLayoutChanged = false;
                    if (isAppOpen(packageName)) {
                        if (mHandler.hasMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName)) {
                            mHandler.removeMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName);
                        }
                        final Message msg = mHandler.obtainMessage(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName);
                        mHandler.sendMessageDelayed(msg, 10000);
                    }
                }
            });
        }
    }

    public void promptIfNeeded(String packageName, IBinder token, boolean isAppWindowsVisible) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null && isSecure()) {
            if (!isAppOpen(packageName)) {
                info.mAppLockUi.show(token, isAppWindowsVisible);
            } else {
                info.mAppLockUi.updateAppWindow(token, isAppWindowsVisible);
            }
            if (mHandler.hasMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName)) {
                mHandler.removeMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName);
            }
        }
    }

    public void updateAppVisibility(String packageName) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null) {
            if (mHandler.hasMessages(SecureAppsHandler.MSG_UPDATE_VISIBILITY, info)) {
                mHandler.removeMessages(SecureAppsHandler.MSG_UPDATE_VISIBILITY, info);
            }
            final Message msg = mHandler.obtainMessage(SecureAppsHandler.MSG_UPDATE_VISIBILITY, info);
            mHandler.sendMessage(msg);
        }
    }

    private SecureAppsInfo getSecureAppsInfo(String packageName) {
        if (!mEnabled.get()) {
            return null;
        }
        return mAppsList.get(packageName);
    }

    private void clearOpenedAppsList() {
        for (String p : mOpenedApplicationsIndex) {
            dispatchCallbacks(p, false);
        }
        mOpenedApplicationsIndex.clear();
    }

    private boolean isSecure() {
        LockPatternUtils lp = new LockPatternUtils(this);
        int storedQuality = lp.getKeyguardStoredPasswordQuality(mUserId);
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

    public void updateSecureAppsConfig(String packageName, Configuration newConfig) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null && isSecure()) {
            info.mLayoutChanged = info.mAppLockUi.updateConfig(newConfig);
        }
    }

    public boolean isGame(String packageName) {
        SecureAppsInfo info = getSecureAppsInfo(packageName);
        if (info != null) {
            return info.mIsGame;
        }
        return false;
    }

    public void notifyTimerTickForAll() {
        mAppsList.forEach((k, v) -> {
            SecureAppsInfo info = (SecureAppsInfo) v;
            if (info != null && info.mAppLockUi.isShowing()) {
                info.mAppLockUi.notifyTimerTick();
            }
        });
    }

    public void notifyTimerStartForAll() {
        mAppsList.forEach((k, v) -> {
            SecureAppsInfo info = (SecureAppsInfo) v;
            if (info != null && info.mAppLockUi.isShowing()) {
                info.mAppLockUi.notifyTimerStart();
            }
        });
    }

    public void notifyTimerStopForAll() {
        mAppsList.forEach((k, v) -> {
            SecureAppsInfo info = (SecureAppsInfo) v;
            if (info != null && info.mAppLockUi.isShowing()) {
                info.mAppLockUi.notifyTimerStop();
            }
        });
    }

    private void dispatchCallbacks(String packageName, boolean opened) {
        mHandler.post(() -> {
            synchronized (mCallbacks) {
                final int N = mCallbacks.size();
                boolean cleanup = false;
                for (int i = 0; i < N; i++) {
                    final ISecureAppsCallback callback = mCallbacks.valueAt(i);
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

    private void cleanUpCallbacksLocked(ISecureAppsCallback callback) {
        mHandler.post(() -> {
            synchronized (mCallbacks) {
                for (int i = mCallbacks.size() - 1; i >= 0; i--) {
                    ISecureAppsCallback found = mCallbacks.valueAt(i);
                    if (found == null || found == callback) {
                        mCallbacks.remove(i);
                    }
                }
            }
        });
    }

    public void addSecureAppsCallback(ISecureAppsCallback callback) {
        mHandler.post(() -> {
            synchronized(mCallbacks) {
                if (!mCallbacks.contains(callback)) {
                    mCallbacks.add(callback);
                }
            }
        });
    }

    public void removeSecureAppsCallback(ISecureAppsCallback callback) {
        mHandler.post(() -> {
            synchronized(mCallbacks) {
                if (mCallbacks.contains(callback)) {
                    mCallbacks.remove(callback);
                }
            }
        });
    }

    private class RemoteWrapper extends ISecureAppsService.Stub {
        @Override
        public void initSecureApps(int userId) {
            SecureAppsService.this.initSecureApps(userId);
        }

        @Override
        public void readState(boolean enabled) {
            SecureAppsService.this.readSecuredApps(enabled);
        }

        @Override
        public void setAppLaunching(String packageName) {
            SecureAppsService.this.setAppLaunching(packageName);
        }

        @Override
        public void addAppToList(String packageName) {
            SecureAppsService.this.addAppToList(packageName);
        }

        @Override
        public void removeAppFromList(String packageName) {
            SecureAppsService.this.removeAppFromList(packageName);
        }

        @Override
        public void launchBeforeActivity(String packageName) {
            SecureAppsService.this.launchBeforeActivity(packageName);
        }

        @Override
        public void setAppIntent(String packageName, Intent intent) {
            SecureAppsService.this.setAppIntent(packageName, intent);
        }

        @Override
        public boolean isAppSecured(String packageName) {
            return SecureAppsService.this.isAppSecured(packageName);
        }

        @Override
        public int getSecuredAppsCount() {
            return SecureAppsService.this.getSecuredAppsCount();
        }

        @Override
        public void addSecureAppsCallback(ISecureAppsCallback callback) {
            SecureAppsService.this.addSecureAppsCallback(callback);
        }

        @Override
        public void removeSecureAppsCallback(ISecureAppsCallback callback) {
            SecureAppsService.this.removeSecureAppsCallback(callback);
        }

        @Override
        public void onWindowsDrawn(String packageName) {
            SecureAppsService.this.onWindowsDrawn(packageName);
        }

        @Override
        public void onAppWindowRemoved(String packageName, IBinder token) {
            SecureAppsService.this.onAppWindowRemoved(packageName, token);
        }

        @Override
        public void updateAppVisibility(String packageName) {
            SecureAppsService.this.updateAppVisibility(packageName);
        }

        @Override
        public void promptIfNeeded(String packageName, IBinder token, boolean isAppWindowsVisible) {
            SecureAppsService.this.promptIfNeeded(packageName, token, isAppWindowsVisible);
        }

        @Override
        public boolean isGame(String packageName) {
            return SecureAppsService.this.isGame(packageName);
        }

        @Override
        public boolean isAppOpen(String packageName) {
            return SecureAppsService.this.isAppOpen(packageName);
        }

        @Override
        public void updateSecureAppsConfig(String packageName, Configuration newConfig) {
            SecureAppsService.this.updateSecureAppsConfig(packageName, newConfig);
        }
    };

    private class SettingsObserver extends ContentObserver {

        private Context mContext;

        SettingsObserver(Context context, Handler handler) {
            super(handler);
            mContext = context;
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                Settings.System.SECURE_APPS_SHOW_ONLY_WAKE), false, this,
                UserHandle.USER_ALL);
            mShowOnlyOnWake = Settings.System.getIntForUser(mContext
                .getContentResolver(),
                Settings.System.SECURE_APPS_SHOW_ONLY_WAKE, 0,
                UserHandle.USER_CURRENT) != 0;
        }

        @Override
        public void onChange(boolean selfChange) {
            mShowOnlyOnWake = Settings.System.getIntForUser(mContext
                    .getContentResolver(),
                    Settings.System.SECURE_APPS_SHOW_ONLY_WAKE, 0,
                    UserHandle.USER_CURRENT) != 0;
        }
    }

    private class SecureAppsHandler extends Handler {
        public static final int MSG_INIT_APPS = 0;
        public static final int MSG_READ_STATE = 1;
        public static final int MSG_WRITE_STATE = 2;
        public static final int MSG_REMOVE_OPENED_APP = 3;
        public static final int MSG_UPDATE_VISIBILITY = 4;

        public SecureAppsHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case MSG_INIT_APPS:
                    handleSecureApps();
                    break;
                case MSG_READ_STATE:
                    readSecuredApps(mEnabled.get());
                    break;
                case MSG_WRITE_STATE:
                    writeSecuredApps();
                    break;
                case MSG_REMOVE_OPENED_APP:
                    if (!mShowOnlyOnWake) {
                        removeOpenedApp((String) msg.obj);
                    }
                    break;
                case MSG_UPDATE_VISIBILITY:
                    SecureAppsInfo info = (SecureAppsInfo) msg.obj;
                    boolean visible = info.mAppLockUi.isAppWindowsVisible();
                    if (!visible) {
                        info.mAppLockUi.setAppWindowGone(null);
                        if (mSecureAppsInfo == info && !info.mLayoutChanged) {
                            stopListeningForFingerprint();
                            mStartFingerprint = false;
                        }
                        info.mLayoutChanged = false;
                        if (isAppOpen(info.mPackageName)) {
                            if (mHandler.hasMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName)) {
                                mHandler.removeMessages(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName);
                            }
                            final Message msgRemove = mHandler.obtainMessage(SecureAppsHandler.MSG_REMOVE_OPENED_APP, info.mPackageName);
                            mHandler.sendMessageDelayed(msgRemove, 10000);
                        }
                    }
                    break;
                default:
                    Slog.w(TAG, "Unknown message:" + msg.what);
            }
        }
    }

    private class SecureAppsInfo {
        protected final String mPackageName;
        protected final AppLockUi mAppLockUi;
        protected boolean mLayoutChanged;
        protected final boolean mIsGame;
        protected Intent intent;

        public SecureAppsInfo(AppLockUi ui, String pkg) {
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
    }
}
