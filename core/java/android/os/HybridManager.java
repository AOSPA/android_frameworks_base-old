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

package android.os;

import android.content.Context;
import android.graphics.Point;
import android.os.IHybridService;
import android.os.RemoteException;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

/**
 * Hybrid Manager, provides framework access to Hybrid engine properties.
 * Uses a binder to retive Hybridprops from Hybrid Service in system server.  
 * @hide
 */
public class HybridManager {

    static final String TAG = "Hybrid Manager";

    public static final boolean DEBUG = true;
    static final boolean DEBUG_PAC = DEBUG && true;

    float mDisplayFactor;

    Context mContext;
    HybridProp mCurrentProp;
    HybridProp mDefaultProp;
    IHybridService mService;

    //TODO: remove
    boolean mTest = true;

    private void test() {
        HybridProp[] props = new HybridProp[5];
        props[0] = new HybridProp("com.android.settings");
        props[1] = new HybridProp("com.android.calculator2");
        props[2] = new HybridProp("com.android.deskclock");
        props[3] = new HybridProp("com.paranoid.paranoidota");
        for(int i = 0; i <4; i++ ) {
            props[i].active = true;
            props[i].statusBarColor = -256;
            props[i].dpi = 280;
            try {
                mService.setHybridProp(props[i].packageName, props[i]);
            } catch (RemoteException ex) {
                Log.d("shes dead", ex.toString());
            }
        }
    }

    /**
     * Intilizes the binder connetion and default properties.
     * @param context the applications context
     * @param service the binder implmention
     * @hide
     */
    public HybridManager(Context context, IHybridService service) {
        mContext = context;
        mService = service;
        mDefaultProp = new HybridProp("android");
    }

    public boolean getActive() {
        return mCurrentProp.active;
    }

    public void setActive(boolean active) {
        mCurrentProp.active = active;
		saveProp();
    }

    public synchronized HybridProp setPackageName(String packageName) {
        HybridProp tmpProp;
        try {
            //TODO: remove
            if (mTest) {
                Log.d(TAG, packageName + " is being set");
                test();
                mTest=false;
            }  

           if (DEBUG) Log.d(TAG, packageName + " is being set");
           if ((tmpProp = mService.getHybridProp(packageName)) != null) {
                if (DEBUG) Log.d(TAG, tmpProp.packageName + " found in HybridProp map");
                tmpProp.active = true;
                mCurrentProp = tmpProp;
                AppChangedBinder.notifyOfAppChange();
                return mCurrentProp;
           }
           return mDefaultProp;

        } catch (RemoteException ex) {
            return mDefaultProp;
        }
    }

    public String getPackageName(){
        return mCurrentProp.packageName;
    }

    public HybridProp getCurentProp() {
        return mCurrentProp;
    }

    /*
    *@hide
    */
    public int getStatusBarColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC status bar color:" + mCurrentProp.statusBarColor );
        return mCurrentProp.statusBarColor ;
    }

    /*
    *@hide
    */
    public void setStatusBarColor(int color) {
        mCurrentProp.statusBarColor = color;
        setActive(true);
    }

    /*
    *@hide
    */
    public int getNavBarColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC nav bar color:" + mCurrentProp.navBarColor );
        return mCurrentProp.navBarColor;
    }

    public void setNavBarColor(int color) {
        mCurrentProp.navBarColor = color;
        setActive(true);
    }

    /*
    *@hide
    */
    public int getNavBarButtonColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC nav bar button color:" + mCurrentProp.navBarButtonColor );
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
        try {
            mService.setHybridProp(mCurrentProp.packageName, mCurrentProp);
        } catch (RemoteException ex) {
 
        }
    }

    /**
     * Calcuates the size of one pixel for this display
     * @hide
     * @return float the dpi scalling factor
     */
    public float getDisplayFactor() {
        if (mDisplayFactor != 0) {
            return mDisplayFactor;
        } else {
            final Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
            final Point size = new Point();
            display.getSize(size);
            mDisplayFactor = (float) Math.max(size.x, size.y) / (float) Math.min(size.x, size.y);
            return mDisplayFactor;
        }
    }
}
