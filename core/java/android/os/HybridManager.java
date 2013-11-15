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


/*
 *@hide
 */
public class HybridManager { 

    private static final String TAG = "Hybrid Manager";

    public static final boolean DEBUG = true;

    //TODO look up aosp white, and get default dpi/layout
    public static final int DEFAULT_COLOR = -16711681;
    public static final int DEFAULT_LAYOUT = 360;
    public static final int DEFAULT_DPI = 320; 

    private final Context mContext;
    private final IHybridService mService;

    private boolean mActive;

    /*
    *@hide
    */
    public HybridManager(Context context, IHybridService service) {
        mContext = context;
        mService = service;
    }

    public void setPackageName(String pName) {
        try {
            mService.setPackageName(pName);
            mActive = mService.getActive();
			//mActive = true;
        } catch (RemoteException ex) {
            if (DEBUG) Log.e(TAG, "Error setting package name:" + ex.toString());
            mActive = false;
        }
    }

    public String getPackageName() {
        try {
           return mService.getPackageName();
        } catch (RemoteException ex) {
            if (DEBUG) Log.e(TAG, "Error getting package name:" + ex.toString());
        }
        return "com.android.systemui";
    }

    /*
    *@hide
    */
    public int getStatusBarColor() {
        if(mActive) {
            try {
               return mService.getStatusBarColor();
            } catch (RemoteException ex) {
                if (DEBUG) Log.e(TAG, "Error getting statusBar color:" + ex.toString());
            }
            return DEFAULT_COLOR;
        }
        return DEFAULT_COLOR;
    }

    /*
    *@hide
    */
    public void setStatusBarColor(int color) {
        try {
           mService.setStatusBarColor(color);
        } catch (RemoteException ex) {
            if (DEBUG) Log.e(TAG, "Error setting color:" + ex.toString());
        }
    }

    public int getNavBarColor() {
        if(mActive) {
            try {
			   final int color = mService.getNavBarColor();
			   Log.d(TAG, "getting navBar color" + color);
               return color;
            } catch (RemoteException ex) {
                if (DEBUG) Log.e(TAG, "Error getting navBar color:" + ex.toString());
            }
            return DEFAULT_COLOR;
        }
        return DEFAULT_COLOR;
    }

    public void setNavBarColor(int color) {
        try {
           mService.setNavBarColor(color);
        } catch (RemoteException ex) {
            if (DEBUG) Log.e(TAG, "Error setting color:" + ex.toString());
        }
    }

   public int getNavBarButtonColor() {
        if(mActive) {
            try {
               return mService.getNavBarButtonColor();
            } catch (RemoteException ex) {
                if (DEBUG) Log.e(TAG, "Error getting navBarButton color:" + ex.toString());
            }
            return DEFAULT_COLOR;
        }
        return DEFAULT_COLOR;
    }

    public void setNavBarButonColor(int color) {
        try {
           mService.setNavBarButonColor(color);
        } catch (RemoteException ex) {
            if (DEBUG) Log.e(TAG, "Error setting NavBarButton color:" + ex.toString());
        }
    }

    public int getLayout() {
        if(mActive) {
             try {
               return mService.getLayout();
            } catch (RemoteException ex) {
                if (DEBUG) Log.e(TAG, "Error getting layout " + ex.toString());
            }
            return DEFAULT_LAYOUT;
        }
        return DEFAULT_LAYOUT;
    }

    public void setLayout(int layout) {
        try {
           mService.setLayout(layout);
        } catch (RemoteException ex) {
            if (DEBUG) Log.e(TAG, "Error setting layout:" + ex.toString());
        }  
    }

    public int getDpi() {
        if (mActive) {
            try {
               return mService.getDpi();
            } catch (RemoteException ex) {
                if (DEBUG) Log.e(TAG, "Error getting dpi:" + ex.toString());
            }
            return DEFAULT_DPI;
        }
        return DEFAULT_DPI;
    }

    public void setDpi(int dpi) {
        try {
            mService.setDpi(dpi);
        } catch (RemoteException ex) {
            if (DEBUG) Log.e(TAG, "Error setting dpi:" + ex.toString());
        }
    }

    /*
     * @hide
     *  @return the dpi scalling factor
     */
    public float getDisplayFactor() {
        final Display display = ((WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay(); 
        final Point size = new Point();
        display.getSize(size);
        return (float)Math.max(size.x, size.y) / (float)Math.min(size.x, size.y);
    }
}
