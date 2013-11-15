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

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;
import android.os.AppChangedBinder;
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
import java.util.HashMap;
import java.util.Map;

/**
 * Hybrid Service reads writes and proxying of HybridProps.
 * The database is a xml store stored in the data partion.
 * TODO: Check for safe mode in system server
 * @hide
 */
public class HybridService extends IHybridService.Stub {

    private static final String TAG = "Hybrid Service";
    private static final boolean DEBUG = true;

    private static final String FOLDER_NAME = "/system";
    private static final String FILE_NAME = "hybrid-props.xml";
    private static final int INITIAL_SIZE = 10;

    private static final String HYBRIDPROP = "hp";
    private static final String OUTERTAG = "hp";
    private static final String PKG = "pkg";
    private static final String PKG_ID = "n";
    private static final String XML_TAG = "hybrid-props";

    private static final String ACTIVE = "ac";
    private static final String DPI = "dp";
    private static final String LAYOUT = "lt";
    private static final String NAVBARCOLOR = "nb";
    private static final String NAVBARBUTTONCOLOR = "nc";
    private static final String NAVBARGLOWCOLOR = "ng";
    private static final String STATUSBARCOLOR = "sb";
    private static final String STATUSBARICONCOLOR = "si";

	private boolean mSystemReady;

    private final AtomicFile mFile;
    private final Handler mHandler;

    // Use a hashmap over a Arraymap to make lookups faster
    HashMap<String,HybridProp> mHybridProps;

    final Runnable mWriteRunner = new Runnable() {
        public void run() {
            synchronized (HybridService.this) {
                AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>() {
                    @Override protected Void doInBackground(Void... params) {
                        writeState();
                        return null;
                    }
                };
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, (Void[])null);
            }
        }
    };

    /**
     * Intializes and reads the database
     * @hide
     */
    public HybridService() {
        mHybridProps = new HashMap<String,HybridProp> (INITIAL_SIZE);
        File FILE_PATH = new File(Environment.getDataDirectory() + FOLDER_NAME, FILE_NAME);
        mFile = new AtomicFile(FILE_PATH);
        mHandler = new Handler();

        readState();
    }

    /**
     * Gets a HybridProp from the database.
     * @param packageName the package of the request HybridProp
     * @return the requested HybridProp
     * @hide
     */
    public HybridProp getHybridProp(String packageName) {
        if( mSystemReady) {
            return mHybridProps.get(packageName);
        } 
        return null;
    }

    /**
     * Intializes and reads the database.
     * @param packageName package of the HybridProp being saved.
     * @parm prop the HybridProp being added to the database
     * @hide
     */
    public void setHybridProp (String packageName, HybridProp prop) {
        if (DEBUG) Log.d(TAG, "Set " +  prop.toString());
        mHybridProps.put(packageName, prop);
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
                    if (DEBUG) Log.d(TAG, "ParserReader HybridProp " + pkgName + " is not active");
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
                String navBarColor = parser.getAttributeValue(null, NAVBARCOLOR);
                if (navBarColor != null) {
                    hp.navBarColor = Integer.parseInt(navBarColor);
                }
                String navBarButtonColor = parser.getAttributeValue(null, NAVBARBUTTONCOLOR);
                if (navBarButtonColor != null) {
                    hp.navBarButtonColor = Integer.parseInt(navBarButtonColor);
                }
                String navBarGlowColor = parser.getAttributeValue(null, NAVBARGLOWCOLOR);
                if (navBarGlowColor != null) {
                    hp.navBarGlowColor = Integer.parseInt(navBarGlowColor);
                }
                String statusBarColor = parser.getAttributeValue(null, STATUSBARCOLOR);
                if (statusBarColor != null) {
                    hp.statusBarColor = Integer.parseInt(statusBarColor);
                }
                String statusBarIconColor = parser.getAttributeValue(null, STATUSBARICONCOLOR);
                if (statusBarIconColor != null) {
                    hp.statusBarIconColor = Integer.parseInt(statusBarIconColor);
                }
                mHybridProps.put(pkgName,hp);
            } else {
                Log.e(TAG, "Unknown element under <hp>: "
                        + parser.getName());
                XmlUtils.skipCurrentTag(parser);
            }
        }
    }

    private void readState() {
        synchronized (mFile) {
            synchronized (this) {
                FileInputStream stream;
                try {
                    stream = mFile.openRead();
                } catch (FileNotFoundException e) {
                    Log.d(TAG, "Creating HybridProps file: " + mFile.getBaseFile());
                    mHandler.post(mWriteRunner);
                    return;
                }
                boolean success = false;
                try {
                    XmlPullParser parser = Xml.newPullParser();
                    parser.setInput(stream, null);
                    int type;
                    while ((type = parser.next()) != XmlPullParser.START_TAG
                            && type != XmlPullParser.END_DOCUMENT) {
                        ;
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
                    mSystemReady = true;
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
                        mHybridProps.clear();
                    }
                    try {
                        stream.close();
                    } catch (IOException e) {
                    }
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
                        if(!hp.active) {
                            Log.d(TAG,"Parser Writer, " + pkgName + " not active");
                            return;
                        }
                        out.startTag(null, PKG);
                        out.attribute(null, PKG_ID, pkgName);
                        out.startTag(null, HYBRIDPROP);

                        out.attribute(null, ACTIVE, Boolean.toString(true));

                        final int dpi = hp.dpi;
                        if (dpi !=0) {
                            out.attribute(null, DPI, Integer.toString(dpi));
                        }
                        final int layout = hp.layout;
                        if (layout !=0) {
                            out.attribute(null, LAYOUT, Integer.toString(layout));
                        }
                        final int navBarColor = hp.navBarColor;
                        if (navBarColor !=0) {
                            out.attribute(null, NAVBARCOLOR, Integer.toString(navBarColor));
                        }
                        final int navBarButtonColor = hp.navBarButtonColor;
                        if (navBarButtonColor !=0) {
                            out.attribute(null, NAVBARBUTTONCOLOR, Integer.toString(navBarButtonColor));
                        }
                        final int navBarGlowColor = hp.navBarGlowColor;
                        if (navBarGlowColor !=0) {
                            out.attribute(null, NAVBARGLOWCOLOR, Integer.toString(navBarGlowColor));
                        }
                        final int statusBarColor = hp.statusBarColor;
                        if (statusBarColor !=0) {
                            out.attribute(null, STATUSBARCOLOR, Integer.toString(statusBarColor));
                        }
                        final int statusBarIconColor = hp.statusBarIconColor;
                        if (statusBarIconColor !=0) {
                            out.attribute(null, STATUSBARICONCOLOR, Integer.toString(statusBarIconColor));
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
