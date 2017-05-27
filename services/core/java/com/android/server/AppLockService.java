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

package com.android.server;

import android.app.IAppLockService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Process;
import android.util.AtomicFile;
import android.util.Slog;
import android.util.Xml;

import com.android.internal.util.FastXmlSerializer;
import com.android.internal.util.XmlUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlSerializer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class AppLockService extends IAppLockService.Stub {

    private static final String TAG = AppLockService.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String FOLDER_NAME = "/system";
    private static final String FILE_NAME = "locked-apps.xml";

    private static final String PKG = "pkg";

    private AppLockUI mAppLockUI;

    private final AtomicFile mFile;
    private final Handler mHandler;
    private final Object mLock = new Object();
    private final Set<String> mAppsList = new HashSet<>();

    private final Runnable mWriteRunner = new Runnable() {
        @Override
        public void run() {
            writeState();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_PACKAGE_REMOVED.equals(intent.getAction())) {
                final Uri data = intent.getData();
                if (data == null) {
                    Slog.e(TAG, "Cannot handle package broadcast with null data");
                    return;
                }

                final String packageName = data.getSchemeSpecificPart();
                removeAppFromList(packageName);
            }
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                getAppLockUI(context).clearApplicationList();
            }
        }
    };

    public AppLockService(Context context) {
        HandlerThread handlerThread = new HandlerThread(TAG, Process.THREAD_PRIORITY_BACKGROUND);
        handlerThread.start();
        mHandler = new Handler(handlerThread.getLooper());

        mFile = new AtomicFile(new File(Environment.getDataDirectory() + FOLDER_NAME, FILE_NAME));

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        filter.addDataScheme("package");
        context.registerReceiver(mReceiver, filter);

        readState();
    }

    private AppLockUI getAppLockUI(Context context) {
        if (mAppLockUI == null) {
            mAppLockUI = AppLockUI.getInstance(context);
        }

        return mAppLockUI;
    }

    @Override
    public void addAppToList(String packageName) {
        if (!mAppsList.contains(packageName)) {
            mAppsList.add(packageName);

            mHandler.post(mWriteRunner);
        }
    }

    @Override
    public void removeAppFromList(String packageName) {
        if (mAppsList.contains(packageName)) {
            mAppsList.remove(packageName);

            mHandler.post(mWriteRunner);
        }
    }

    @Override
    public boolean isAppLocked(String packageName) {
        return mAppsList.contains(packageName);
    }

    private void readState() {
        synchronized (mLock) {
            FileInputStream stream;
            try {
                stream = mFile.openRead();
            } catch (FileNotFoundException e) {
                mHandler.post(mWriteRunner);
                if (DEBUG) {
                    Slog.i(TAG, "creating applock file: " + mFile.getBaseFile());
                }
                return;
            }
            boolean success = false;
            try {
                XmlPullParser parser = Xml.newPullParser();
                parser.setInput(stream, null);
                int type;
                while ((type = parser.next()) != XmlPullParser.START_TAG
                        && type != XmlPullParser.END_DOCUMENT) {
                    continue;
                }

                if (type != XmlPullParser.START_TAG) {
                    throw new IllegalStateException("no start tag found");
                }
                int outerDepth = parser.getDepth();
                while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                        && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
                    if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                        continue;
                    }

                    String tagName = parser.getName();
                    if (DEBUG) {
                        Slog.i(TAG, "tagName is: " + tagName);
                    }
                    if (tagName.equals("package")) {
                        String pkgName = parser.getAttributeValue(null, "package");
                        if (DEBUG) {
                            Slog.i(TAG, "pkgName is: " + pkgName);
                        }
                        if (!mAppsList.contains(pkgName)) {
                            mAppsList.add(pkgName);
                        }
                    } else {
                        Slog.e(TAG, "Unknown element under <packagename>: "
                                + parser.getName());
                        XmlUtils.skipCurrentTag(parser);
                    }
                }
                success = true;
            } catch (Exception e) {
                Slog.e(TAG, "Failed parsing " + e);
            } finally {
                if (!success) {
                    Slog.e(TAG, "AppLock Parser bailing out");
                    mAppsList.clear();
                }
                try {
                    if (DEBUG) {
                        Slog.i(TAG, "AppLock Parser success");
                    }
                    stream.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void writeState() {
        synchronized (mLock) {
            FileOutputStream stream;
            try {
                stream = mFile.startWrite();
            } catch (IOException e) {
                Slog.e(TAG, "Failed to write state: " + e.toString());
                return;
            }

            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, "utf-8");
                out.startDocument(null, true);
                out.startTag(null, "package");
                for (String pkgName : mAppsList) {
                    if (DEBUG) {
                        Slog.i(TAG, "writeState(): pkgName= " + pkgName);
                    }
                    if (pkgName != null) {
                        out.startTag(null, "package");
                        out.attribute(null, "package", pkgName);
                        out.endTag(null, "package");
                    }
                }
                out.endTag(null, "package");
                out.endDocument();
                mFile.finishWrite(stream);
            } catch (IOException e) {
                Slog.e(TAG, "Failed to write state, restoring from backup." + e.toString());
                mFile.failWrite(stream);
            }
        }
    }
}
