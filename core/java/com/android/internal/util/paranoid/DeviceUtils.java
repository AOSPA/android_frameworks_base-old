/*
* Copyright (C) 2013 SlimRoms Project
* This code has been modified. Portions copyright (C) 2014, ParanoidAndroid Project.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/

package com.android.internal.util.paranoid;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.util.DisplayMetrics;
import android.view.DisplayInfo;
import android.view.Surface;
import android.view.WindowManager;

import java.util.ArrayList;

public class DeviceUtils {

    // Device Types
    private static final int DEVICE_PHONE  = 0;
    private static final int DEVICE_HYBRID = 1;
    private static final int DEVICE_TABLET = 2;

    private DeviceUtils() {
    }

    private static int getScreenType(Context con) {
        WindowManager wm = (WindowManager)con.getSystemService(Context.WINDOW_SERVICE);
        DisplayInfo outDisplayInfo = new DisplayInfo();
        wm.getDefaultDisplay().getDisplayInfo(outDisplayInfo);
        int shortSize = Math.min(outDisplayInfo.logicalHeight, outDisplayInfo.logicalWidth);
        int shortSizeDp =
            shortSize * DisplayMetrics.DENSITY_DEFAULT / outDisplayInfo.logicalDensityDpi;
        if (shortSizeDp < 600) {
            return DEVICE_PHONE;
        } else if (shortSizeDp < 720) {
            return DEVICE_HYBRID;
        } else {
            return DEVICE_TABLET;
        }
    }

    public static boolean isPhone(Context con) {
        return getScreenType(con) == DEVICE_PHONE;
    }

    public static boolean isHybrid(Context con) {
        return getScreenType(con) == DEVICE_HYBRID;
    }

    public static boolean isTablet(Context con) {
        return getScreenType(con) == DEVICE_TABLET;
    }

    /**
     * Locks the activity orientation to the current device orientation
     * @param act
     */
    public static void lockCurrentOrientation(Activity act) {
        int currentRotation = act.getWindowManager().getDefaultDisplay().getRotation();
        int frozenRotation = 0;
        boolean isTablet = isTablet(act);
        switch(currentRotation) {
                case Surface.ROTATION_0:
                        frozenRotation = isTablet ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE :
                            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
                    break;
                case Surface.ROTATION_90:
                        frozenRotation = isTablet ? ActivityInfo.SCREEN_ORIENTATION_PORTRAIT :
                            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
                    break;
                case Surface.ROTATION_180:
                        frozenRotation = isTablet ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE :
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT;
                    break;
                case Surface.ROTATION_270:
                        frozenRotation = isTablet ? ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT :
                            ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE;
                    break;
            }
        act.setRequestedOrientation(frozenRotation);
    }
}
