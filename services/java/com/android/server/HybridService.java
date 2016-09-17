/*
 * Copyright (C) 2013 ParanoidAndroid project
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

import android.os.AsyncTask;
import android.os.Environment;
import android.os.HybridProp;
import android.os.IHybridService;
import android.os.Handler;
import android.util.AtomicFile;
import android.util.Log;
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
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid Service reads writes and proxying of HybridProps.
 * The database is a xml store stored in the data partition.
 * TODO: Check for safe mode in system server
 *
 * @hide
 */
public class HybridService extends IHybridService.Stub {

    private static final String TAG = "HybridService";
    private static final boolean DEBUG = true;

    private static final String FOLDER_NAME = "/system";
    private static final String FILE_NAME = "hybrid-props.xml";
    private static final int INITIAL_SIZE = 10;

    private static final String HYBRIDPROP = "hp";
    private static final String PKG = "pkg";
    private static final String PKG_ID = "n";
    private static final String XML_TAG = "hybrid-props";

    private static final String ACTIVE = "ac";
    private static final String DPI = "dp";
    private static final String LAYOUT = "lt";

    private static final String PACKAGE_NAME_SYSTEM_UI = "com.android.systemui";
    private static final String PACKAGE_NAME_SETTINGS = "com.android.settings";
    private static final String PACKAGE_NAME_CALCULATOR2 = "com.android.calculator2";

    private boolean mSystemReady;

    private final AtomicFile mFile;
    private final Handler mHandler;

    // Use a hashmap over a Arraymap to make lookups faster
    HashMap<String, HybridProp> mHybridProps;

    final Runnable mWriteRunner = new Runnable() {
        public void run() {
            synchronized (HybridService.this) {
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... params) {
                        writeState();
                        return null;
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[]) null);
            }
        }
    };

    /**
     * Initializes and reads the database
     *
     * @hide
     */
    public HybridService() {
        mHybridProps = new HashMap<String, HybridProp>(INITIAL_SIZE);
        File FILE_PATH = new File(Environment.getDataDirectory() + FOLDER_NAME, FILE_NAME);
        mFile = new AtomicFile(FILE_PATH);
        mHandler = new Handler();

        readState();
    }

    /**
     * Gets a HybridProp from the database.
     *
     * @return the requested HybridProp
     * @param packageName the package of the request HybridProp
     * @hide
     */
    @Override
    public HybridProp getHybridProp(String packageName) {
        if (mSystemReady) {
            if (DEBUG) Log.d(TAG, "packageName: " + packageName);
            return mHybridProps.get(packageName);
        }
        return null;
    }

    /**
     * Entry point to add a hybridProp for a package.
     *
     * @param packageName package of the HybridProp being saved.
     * @param prop the HybridProp being added to the database
     * @hide
     */
    @Override
    public void setHybridProp(String packageName, HybridProp prop) {
        if (DEBUG) Log.d(TAG, "Set " + prop.toString());
        mHybridProps.put(packageName, prop);

        // delay long enough to only write once in a for loop
        mHandler.removeCallbacks(mWriteRunner);
        mHandler.postDelayed(mWriteRunner, 500);
    }

    @Override
    public List<HybridProp> getAllProps() {
        return new ArrayList<HybridProp>(mHybridProps.values());
    }

    public void injectDummyProps() {
        HybridProp systemui = new HybridProp(PACKAGE_NAME_SYSTEM_UI, true);
        systemui.dpi = HybridProp.PHABLET_DPI;
        systemui.layout = HybridProp.PHABLET_LAYOUT;
        mHybridProps.put(PACKAGE_NAME_SYSTEM_UI, systemui);

        HybridProp calculator2 = new HybridProp(PACKAGE_NAME_CALCULATOR2, true);
        calculator2.dpi = HybridProp.PHABLET_DPI;
        calculator2.layout = HybridProp.PHABLET_LAYOUT;
        mHybridProps.put(PACKAGE_NAME_CALCULATOR2, calculator2);

        HybridProp settings = new HybridProp(PACKAGE_NAME_SETTINGS, true);
        settings.dpi = HybridProp.PHABLET_DPI;
        settings.layout = HybridProp.PHABLET_LAYOUT;
        mHybridProps.put(PACKAGE_NAME_SETTINGS, settings);

        writeState(); // write to xml with new ones added
    }

    public void systemReady(boolean safeMode, boolean onlyCore) {
        mSystemReady = safeMode && onlyCore;
    }

    private void readHybridProp(XmlPullParser parser) throws NumberFormatException,
            XmlPullParserException, IOException {
        final String pkgName = parser.getAttributeValue(null, PKG_ID);
        int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            String tagName = parser.getName();
            if (tagName.equals(HYBRIDPROP)) {
                HybridProp hp = new HybridProp(pkgName);
                String active = parser.getAttributeValue(null, ACTIVE);
                if (active != null) {
                    hp.active = Boolean.parseBoolean(active);
                    if (DEBUG && !hp.active) Log.d(TAG, "ParserReader HybridProp " + pkgName + " is not active");
                    if (!hp.active) return;
                }
                String dpi = parser.getAttributeValue(null, DPI);
                if (dpi != null) {
                    hp.dpi = Integer.parseInt(dpi);
                }
                String layout = parser.getAttributeValue(null, LAYOUT);
                if (layout != null) {
                    hp.layout = Integer.parseInt(layout);
                }
                mHybridProps.put(pkgName, hp);
            } else {
                Log.e(TAG, "Unknown element under <hp>: "
                        + parser.getName());
                XmlUtils.skipCurrentTag(parser);
            }
        }
    }

    private boolean readState() {
        synchronized (mFile) {
            synchronized (this) {
                FileInputStream stream;
                try {
                    stream = mFile.openRead();
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "Creating HybridProps file: " + mFile.getBaseFile());
                    mHandler.post(mWriteRunner);
                    return false;
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
                        if (tagName.equals(PKG)) {
                            readHybridProp(parser);
                        } else {
                            Log.e(TAG, "Unknown element under <hybrid-props>: "
                                    + parser.getName());
                            XmlUtils.skipCurrentTag(parser);
                        }
                    }
                    success = true;
                } catch (IllegalStateException e) {
                    Log.e(TAG, "Failed parsing " + e);
                } catch (NullPointerException e) {
                    Log.e(TAG, "Failed parsing " + e);
                } catch (NumberFormatException e) {
                    Log.e(TAG, "Failed parsing " + e);
                } catch (XmlPullParserException e) {
                    Log.e(TAG, "Failed parsing " + e);
                } catch (IOException e) {
                    Log.e(TAG, "Failed parsing " + e);
                } catch (IndexOutOfBoundsException e) {
                    Log.e(TAG, "Failed parsing " + e);
                } finally {
                    if (!success) {
                        Log.e(TAG, "Hybrid Parser bailing out");
                        mHybridProps.clear();
                    }
                    try {
                        Log.d(TAG, "Hybrid Parser success");
                        stream.close();
                    } catch (IOException e) {
                    }
                    return success;
                }
            }
        }
    }

    private void writeState() {
        synchronized (mFile) {
            FileOutputStream stream;
            try {
                stream = mFile.startWrite();
            } catch (IOException e) {
                Log.d(TAG, "Failed to write state: " + e.toString());
                return;
            }

            try {
                XmlSerializer out = new FastXmlSerializer();
                out.setOutput(stream, "utf-8");
                out.startDocument(null, true);
                out.startTag(null, XML_TAG);
                if (mHybridProps != null) {
                    for (Map.Entry<String, HybridProp> entry : mHybridProps.entrySet()) {
                        final String pkgName = entry.getKey();
                        HybridProp hp = entry.getValue();
                        if (!hp.active) {
                            if (DEBUG) Log.d(TAG, "Parser Writer, " + pkgName + " not active");
                            break;
                        }
                        out.startTag(null, PKG);
                        out.attribute(null, PKG_ID, pkgName);
                        out.startTag(null, HYBRIDPROP);

                        out.attribute(null, ACTIVE, Boolean.toString(true));

                        final int dpi = hp.dpi;
                        if (dpi != 0) {
                            out.attribute(null, DPI, Integer.toString(dpi));
                        }
                        final int layout = hp.layout;
                        if (layout != 0) {
                            out.attribute(null, LAYOUT, Integer.toString(layout));
                        }
                        out.endTag(null, HYBRIDPROP);
                        out.endTag(null, PKG);
                    }
                }
                out.endTag(null, XML_TAG);
                out.endDocument();
                mFile.finishWrite(stream);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write state, restoring from backup." + e.toString());
                mFile.failWrite(stream);
            }
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this) {
            int i = 0;
            pw.println("Current Hybrid Service state:");
            pw.println("Number of HybridProps = " + mHybridProps.size());

            for (HybridProp hp : mHybridProps.values()) {
                pw.println("Hybrid prop:" + i + " " + hp.toString());
                i++;
            }
        }
    }

}
