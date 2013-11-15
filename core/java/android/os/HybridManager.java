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

    public static final String TAG = "Hybrid Manager";

    public static final boolean DEBUG = true;
    public static final boolean DEBUG_PAC = DEBUG && true;

    private float mDisplayFactor;

    private Context mContext;
    private HybridProp mCurrentProp;
    private HybridProp mDefaultProp;
    private IHybridService mService;

    //TODO: remove
    static boolean mTest = true;

    private void test() {
		if (mTest) {
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
		mTest = true;
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
        mDefaultProp = new HybridProp("default");
    }

    /**
     * Gets the HybridProp corispoinding to the reguested package.
     * @param packageName the package name of the prop to request
     * @return the requested HybridProp
     * @hide
     */
    public synchronized HybridProp setPackageName(String packageName) {
        HybridProp tmpProp;
        try {
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
            // HybridService is dead
            return mDefaultProp;
        }
    }

    /**
     * Gets the package name of the current HybridProp.
     * @return the current package name
     * @hide
     */
    public String getPackageName(){
        return mCurrentProp.packageName;
    }

    /**
     * Gets the current HybridProp.
     * @return the current HybridProp
     * @hide
     */
    public HybridProp getCurentProp() {
        return mCurrentProp;
    }

    /**
     * Gets the current HybridProps active status
     * @return state
     * @hide
     */
    public boolean getActive() {
        return mCurrentProp.active;
    }

    /**
     * sets the current HybridProps active status
     * @param state
     * @hide
     */
    public void setActive(boolean active) {
        mCurrentProp.active = active;
		saveProp();
    }

    /**
     * Gets the current status bar color
     * @return status bar color
     * @hide
     */
    public int getStatusBarColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC status bar color:" + mCurrentProp.statusBarColor );
        return mCurrentProp.statusBarColor ;
    }

    /**
     * Gets the current status bar color
     * @return status bar color
     * @hide
     */
    public void setStatusBarColor(int color) {
        mCurrentProp.statusBarColor = color;
        setActive(true);
    }

    /**
     * Gets the current status bar icon color
     * @return status bar icon color
     * @hide
     */
    public int getStatusBarIconColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC status bar icon color:" + mCurrentProp.statusBarIconColor );
        return mCurrentProp.statusBarIconColor ;
    }

    /**
     * Gets the current status bar icon color
     * @return status bar icon color
     * @hide
     */
    public void setStatusBarIconColor(int color) {
        mCurrentProp.statusBarIconColor = color;
        setActive(true);
    }

    /**
     * Gets the current nav bar color
     * @return the current nav bar color
     * @hide
     */
    public int getNavBarColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC nav bar color:" + mCurrentProp.navBarColor );
        return mCurrentProp.navBarColor;
    }

    /**
     * Sets the current status bar color
     * @param status bar color
     * @hide
     */
    public void setNavBarColor(int color) {
        mCurrentProp.navBarColor = color;
        setActive(true);
    }

     /**
     * Gets the current nav bar button color
     * @param nav bar button color
     * @hide
     */
    public int getNavBarButtonColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC nav bar button color:" + mCurrentProp.navBarButtonColor );
        return mCurrentProp.navBarButtonColor;
    }

     /**
     * Gets the current nav bar button color
     * @param nav bar button color
     * @hide
     */
    public void setNavBarButonColor(int color) {
        mCurrentProp.navBarButtonColor = color;
        setActive(true);
    }

    /**
     * Gets the current nav bar glow color
     * @return the current nav bar glow color
     * @hide
     */
    public int getNavBarGlowColor() {
        if (DEBUG_PAC) Log.d("TAG", "PAC nav bar glow color:" + mCurrentProp.navBarGlowColor );
        return mCurrentProp.navBarColor;
    }

    /**
     * Sets the current status bar glow color
     * @param status bar glow color
     * @hide
     */
    public void setNavBarGlowColor(int color) {
        mCurrentProp.navBarGlowColor = color;
        setActive(true);
    }

     /**
     * Gets the current dpi
     * @param dots per inch (dpi)
     * @hide
     */
    public int getDpi() {
        return mCurrentProp.dpi;
    }

     /**
     * Sets the dpi
     * @param dpi
     * @hide
     */
    public void setDpi(int dpi) {
        mCurrentProp.dpi = dpi;
        setActive(true);
    }

     /**
     * Gets the current layout
     * @param layout {360,600,720}
     * @hide
     */
    public void setLayout(int layout) {
        mCurrentProp.layout = layout;
        setActive(true);
    }

     /**
     * Gets the current layout
     * @return layout
     * @hide
     */
    public int getLayout() {
        return mCurrentProp.layout;
    }

    //saves the current prop
    private void saveProp() {
        try {
            mService.setHybridProp(mCurrentProp.packageName, mCurrentProp);
        } catch (RemoteException ex) {
            //HybridService is dead
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
