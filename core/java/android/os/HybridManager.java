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
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

import java.util.List;


/**
 * Hybrid Manager, provides framework access to Hybrid engine properties.
 * Uses a binder to retrieve HybridProps from Hybrid Service in system server.
 * // TODO move window manger here
 *
 * @hide
 */
public class HybridManager {

    public static final String TAG = "HybridManager";

    private static final String PACKAGE_NAME_SYSTEM_UI = "com.android.systemui";
    private static final String PACKAGE_NAME_SETTINGS = "com.android.settings";

    public static final boolean DEBUG = true;
    public static final int DEFAULT_DPI = HybridProp.PHONE_DPI;

    private float mDisplayFactor;

    private Context mContext;
    private HybridProp mCurrentProp;
    private HybridProp mDefaultProp;
    private IHybridService mService;

    /**
     * Intilizes the binder connection and default properties.
     *
     * @param context the applications context
     * @param service the binder implementation
     * @hide
     */
    public HybridManager(Context context, IHybridService service) {
        mContext = context;
        mService = service;
        mDefaultProp = new HybridProp("android");
    }

    /**
     * Gets the HybridProp corresponding to the requested package.
     *
     * @return the requested HybridProp
     * @param packageName the package name of the prop to request
     * @hide
     */
    public synchronized HybridProp setActivePackage(String packageName) {
        HybridProp tmpProp;
        try {
            if (packageName.equals(PACKAGE_NAME_SYSTEM_UI) || packageName.equals(PACKAGE_NAME_SETTINGS)) {
                // You're not allowed to run within stock dpi or layout for now
                HybridProp hardcodedProp = new HybridProp(packageName, true);
                hardcodedProp.dpi = 240;
                hardcodedProp.layout = 480;
                return hardcodedProp;
            }
            if (DEBUG) Log.d(TAG, packageName + " is being set");
            if ((tmpProp = mService.getHybridProp(packageName)) != null) {
                if (DEBUG) Log.d(TAG, tmpProp.packageName + " found in HybridProp map");
                tmpProp.active = true;
                mCurrentProp = tmpProp;
                return mCurrentProp;
            }

            return mDefaultProp;

        } catch (NullPointerException ex) {
            // Prop is not defined for package
            return mDefaultProp;
        } catch (RemoteException ex) {
            // HybridService is dead
            return mDefaultProp;
        }

    }

    /**
     * Gets A list of all available props
     *
     * @return A list of HybridProps
     * @hide
     */
    public synchronized List<HybridProp> getAllProps() {
        List<HybridProp> propList = null;
        try {
            propList = mService.getAllProps();
        } catch (RemoteException ex) {
            //Hybird is dead
        }
        return propList;
    }

    /**
     * Gets the package name of the current HybridProp.
     *
     * @return the current package name
     * @hide
     */
    public String getPackageName() {
        return mCurrentProp.packageName;
    }

    /**
     * Gets the current HybridProp.
     *
     * @return the current HybridProp
     * @hide
     */
    public HybridProp getCurrentProp() {
        return mCurrentProp;
    }

    /**
     * Gets the current HybridProps active status
     *
     * @return state
     * @hide
     */
    public boolean getActive() {
        return mCurrentProp.active;
    }

    /**
     * Sets the current HybridProps active status
     *
     * @param active
     * @hide
     */
    public void setActive(boolean active) {
        mCurrentProp.active = active;
        saveProp();
    }

    /**
     * Gets the current dpi
     *
     * @hide
     */
    public int getDpi() {
        return mCurrentProp.dpi;
    }

    /**
     * Gets default dpi
     *
     * @hide
     */
    public int getDefaultDpi() {
        return SystemProperties.getInt("ro.sf.lcd_density", DEFAULT_DPI);
    }

    /**
     * Sets the dpi
     *
     * @param dpi specify dpi
     * @hide
     */
    public void setDpi(int dpi) {
        mCurrentProp.dpi = dpi;
        setActive(true);
    }

    /**
     * Sets the current layout
     *
     * @param layout {360,600,720}
     * @hide
     */
    public void setLayout(int layout) {
        mCurrentProp.layout = layout;
        setActive(true);
    }

    /**
     * Gets the current layout
     *
     * @return layout
     * @hide
     */
    public int getLayout() {
        return mCurrentProp.layout;
    }

    // Saves the current prop
    private void saveProp() {
        try {
            mService.setHybridProp(mCurrentProp.packageName, mCurrentProp);
        } catch (RemoteException ex) {
            // HybridService is dead
        }
    }

    /**
     * Calculates the size of one pixel for this display
     *
     * @return float the dpi scaling factor
     * @hide
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