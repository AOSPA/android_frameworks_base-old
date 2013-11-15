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
import android.os.HybirdProp;
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

/*
 *@hide
 */
public class HybridService extends IHybridService.Stub {

    static final boolean DEBUG = true;
    static final String FILE_NAME = "hybrid-props.xml";
    static final String TAG = "Hybrid Service";

    final Context mContext;

	boolean mSystemReady;
    AtomicFile mFile;
    Handler mHandler;

    // Use a hashmap over a Arraymap to make lookups faster
    HashMap<String,HybridProp> 	mHybridProps = new HashMap<String,HybridProp>(10);

    //Set to keygaurd as it is the first package loaded
    String mPackageName = "com.android.keyguard";
    HybridProp mCurrentProp = new HybridProp(mPackageName);

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

    /*
    *@hide
    */
    public HybridService(Context context) {
        mContext = context;
    }

    public HybirdProp getPackageNameForResult() {
        mPackageName = name;
        HybridProp tmpProp = mHybridProps.get(mPackageName);
        if (tmpProp != null) {
            Log.d(TAG, mPackageName + " found in HybridProp map");
            mCurrentProp = tmpProp;
            setStatusBarColor(-16711681);
            setNavBarColor(-256);
            setDpi(200);
            setActive(true);
        }
    }

    private void test() {
        HybridProp[] props = new HybridProp[5];
        props[0] = new HybridProp("pl.soildexplorer");
        props[1] = new HybridProp("com.android.calculator2");
        props[2] = new HybridProp("com.android.deskclock");
        props[3] = new HybridProp("com.paranoid.paranoidota");
        for(int i = 0; i <5; i++ ) {
	        mHybridProps.put(props[i].packageName,props[i]);
        }
    }

    private HybridProp build(HybridProp hp) {
        return hp;
    }

    public void systemReady() {
        mFile = new AtomicFile(new File(Environment.getDataDirectory() + "/system", FILE_NAME));
        mHandler = new Handler();
        readState();
    }

    public boolean getActive() {
        return mCurrentProp.active;
    }

    public void setActive(boolean active) {
        mCurrentProp.active = active;
		saveProp();
    }

    public void setPackageName(String name) {
        Log.d(TAG, name + " is being set");
        if(mSystemReady) {
            mPackageName = name;
            HybridProp tmpProp = mHybridProps.get(mPackageName);
            if (tmpProp != null) {
                Log.d(TAG, mPackageName + " found in HybridProp map");
                mCurrentProp = tmpProp;
                setStatusBarColor(-16711681);
                setNavBarColor(-256);
                setDpi(200);
                setActive(true);
            }
        AppChangedBinder.notifyAppChanged();
        }
    }

    public String getPackageName(){
        return mPackageName;
    }

    /*
    *@hide
    */
    public int getStatusBarColor() {
        return mCurrentProp.statusBarColor ;
    }

    /*
    *@hide
    */
    public void setStatusBarColor(int color) {
        mCurrentProp.statusBarColor = color;
    }

    /*
    *@hide
    */
    public int getNavBarColor() {
        return mCurrentProp.navBarColor;
    }

    public void setNavBarColor(int color) {
        mCurrentProp.navBarColor = color;
    }

    /*
    *@hide
    */
    public int getNavBarButtonColor() {
        return mCurrentProp.navBarButtonColor;
    }

    public void setNavBarButonColor(int color) {
        mCurrentProp.navBarButtonColor = color;
        setActive(true);
    }

    public int getDpi() {
        return mCurrentProp.dpi;
    }

    public void setDpi(int dpi) {
        mCurrentProp.dpi = dpi;
        setActive(true);
    }

    public void setLayout(int layout) {
        mCurrentProp.layout = layout;
        setActive(true);
    }

    public int getLayout() {
        return mCurrentProp.layout;
    }

    private void saveProp() {
        mHybridProps.put(mPackageName,mCurrentProp);
    }

    private HashMap<String,HybridProp> getPackages() {
        final ArrayList<PackageInfo> pkgs= (ArrayList) mContext.getPackageManager().getInstalledPackages(0);
        final HashMap<String,HybridProp> hybridProps = new HashMap<String,HybridProp>(pkgs.size());

        for(PackageInfo info: pkgs) {
            final String pkgName = info.packageName;
            hybridProps.put(pkgName,new HybridProp(pkgName));
        }

        return hybridProps;
    }

    void readHybridProp(XmlPullParser parser) throws NumberFormatException,
            XmlPullParserException, IOException {
        final String pkgName = parser.getAttributeValue(null, "n");
        int outerDepth = parser.getDepth();
        int type;
        while ((type = parser.next()) != XmlPullParser.END_DOCUMENT
                && (type != XmlPullParser.END_TAG || parser.getDepth() > outerDepth)) {
            if (type == XmlPullParser.END_TAG || type == XmlPullParser.TEXT) {
                continue;
            }

            String tagName = parser.getName();
            if (tagName.equals("hp")) {
                HybridProp hp = new HybridProp(pkgName);
                String active = parser.getAttributeValue(null, "nb");
                if (active != null) {
                    hp.active = Boolean.parseBoolean(active);
                    if (DEBUG) Log.d(TAG,"ParserReader, HybridProp " + pkgName + " not active");
                    if (!hp.active) return;
                }
                String dpi = parser.getAttributeValue(null, "di");
                if (dpi != null) {
                    hp.dpi = Integer.parseInt(dpi);
                }
                String layout = parser.getAttributeValue(null, "lt");
                if (layout != null) {
                    hp.layout = Integer.parseInt(layout);
                }
                String navBarColor = parser.getAttributeValue(null, "nb");
                if (navBarColor != null) {
                    hp.navBarColor = Integer.parseInt(navBarColor);
                }
                String navBarButtonColor = parser.getAttributeValue(null, "nc");
                if (navBarButtonColor != null) {
                    hp.navBarButtonColor = Integer.parseInt(navBarButtonColor);
                }
                String statusBarColor = parser.getAttributeValue(null, "sb");
                if (statusBarColor != null) {
                    hp.statusBarColor = Integer.parseInt(statusBarColor);
                }
                String statusBarIconColor = parser.getAttributeValue(null, "sc");
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
                    Log.e(TAG, "No Hybrid Props found in " + mFile.getBaseFile() + "; starting empty");
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
                        if (tagName.equals("pkg")) {
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
                        mHybridProps.clear();
                    }
                    try {
                        stream.close();
		                test();
                        mSystemReady = true;
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
                out.startTag(null, "hybrid-props");

                if (mHybridProps != null) {
                    for (Map.Entry<String, HybridProp> entry : mHybridProps.entrySet()) {
                        final String pkgName = entry.getKey();
                        HybridProp hp = entry.getValue();
                        if(!hp.active) {
                            Log.d(TAG,"Parser Writer, " + pkgName + " not active");
                            return;
                        }
                        out.startTag(null, "pkg");
                        out.attribute(null, "n", pkgName);
                        out.startTag(null, "hp");
                        out.attribute(null, "ac", Boolean.toString(true));

                        final int dpi = hp.dpi;
                        if (dpi !=0) {
                            out.attribute(null, "di", Integer.toString(dpi));
                        }
                        final int layout = hp.layout;
                        if (layout !=0) {
                            out.attribute(null, "lt", Integer.toString(layout));
                        }
                        final int navBarColor = hp.navBarColor;
                        if (navBarColor !=0) {
                            out.attribute(null, "nb", Integer.toString(navBarColor));
                        }
                        final int navBarButtonColor = hp.navBarButtonColor;
                        if (navBarButtonColor !=0) {
                            out.attribute(null, "nb", Integer.toString(navBarButtonColor));
                        }
                        final int statusBarColor = hp.statusBarColor;
                        if (statusBarColor !=0) {
                            out.attribute(null, "nb", Integer.toString(statusBarColor));
                        }
                        final int statusBarIconColor = hp.statusBarIconColor;
                        if (statusBarIconColor !=0) {
                            out.attribute(null, "nb", Integer.toString(statusBarIconColor));
                        }
                        out.endTag(null, "hp");
                        out.endTag(null, "pkg");
                    }
                }
                out.endTag(null, "hybrid-props");
                out.endDocument();
                mFile.finishWrite(stream);
            } catch (IOException e) {
                Log.e(TAG, "Failed to write state, restoring backup." + e.toString());
                mFile.failWrite(stream);
            }
        }
    }

    @Override
    protected void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        synchronized (this) {
            int i =0;
            pw.println("Current Hybrid Service state:");
            pw.println("Number of props = " + mHybridProps.size());
            final HybridProp prop =  mHybridProps.get(mPackageName);
            if (prop != null) {
	            pw.println("Current Hybrid prop is :" + prop.toString());
            }
            for (HybridProp hp : mHybridProps.values()) {
                pw.println("Hybrid prop:" + i + " " + hp.toString());
                i++;
            }
        }
    }

}
