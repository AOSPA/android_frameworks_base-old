/*
 * Copyright (C) 2022 Paranoid Android
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

package com.android.internal.util;

import android.content.res.Resources;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;

import com.android.internal.R;

public class PropImitationFactory {

    private static final String TAG = "PropImitationFactory";
    private static final boolean DEBUG = false;

    private static final String pixel_fp =
            Resources.getSystem().getString(com.android.internal.R.string.pixel_fingerprint);

    private static final String stock_fp =
            Resources.getSystem().getString(com.android.internal.R.string.stock_fingerprint);

    private static final String PACKAGE_ARCORE = "com.google.ar.core";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PROCESS_UNSTABLE = "com.google.android.gms.unstable";

    public void setProps(String packageName) {
        if (packageName == null) {
            return;
        }

        public static void initApplicationBeforeOnCreate(Application app) {
            if (PACKAGE_GMS.equals(app.getPackageName()) &&
                    PROCESS_UNSTABLE.equals(Application.getProcessName())) {
                sIsGms = true;
            }

            if (PACKAGE_FINSKY.equals(app.getPackageName())) {
                sIsFinsky = true;
            }
        }

        private static boolean isCallerSafetyNet() {
            return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
        }

        if (!pixel_fp.isEmpty() && !(sIsGms ^ sIsFinsky)) {
            if (DEBUG) Log.d(TAG, "Defining fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", pixel_fp);
        }

        if (!stock_fp.isEmpty() && (packageName.equals(PACKAGE_ARCORE) {
            if (DEBUG) Log.d(TAG, "Defining fingerprint for: " + packageName);
            setPropValue("FINGERPRINT", stock_fp);
        }
    }

    private void setPropValue(String key, Object value){
        try {
            if (DEBUG) Log.d(TAG, "Defining prop " + key + " to " + value.toString());
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);
            field.set(null, value);
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to set prop " + key, e);
        }
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet
        if (sIsGms && isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }

        // Check stack for PlayIntegrity
        if (sIsFinsky) {
            throw new UnsupportedOperationException();
        }
    }
}
