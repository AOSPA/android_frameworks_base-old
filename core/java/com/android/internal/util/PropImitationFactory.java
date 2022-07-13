/*
 * Copyright (C) 2020 The Pixel Experience Project
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

import android.content.Context;
import android.os.Build;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.android.internal.R;

public class PropImitationFactory {

    private static final String TAG = "PropImitationFactory";
    private static final boolean DEBUG = false;
    private static volatile boolean mIsCallerPackageGms;

    public static final String PACKAGE_GMS = "com.google.android.gms";
    public static final String PACKAGE_ARCORE = "com.google.ar.core";

    private final Map<String, Object> mPropsToChangePlatform;
    private final Map<String, Object> mPropsToChangeV1;
    private final Map<String, Object> mPropsToChangeV2;
    private final Map<String, ArrayList<String>> mPropsToKeep;

    private static final String[] PACKAGES_TO_CHANGE_EXTRA = {
            "com.android.chrome"
    };

    private static final String[] PACKAGES_TO_CHANGE_V1 = {
            "com.google.android.apps.photos"
    };

    private static final String[] PACKAGES_TO_KEEP = {
            "com.google.android.GoogleCamera",
            "com.google.android.GoogleCamera.Cameight",
            "com.google.android.GoogleCamera.Go",
            "com.google.android.GoogleCamera.Urnyx",
            "com.google.android.GoogleCameraAsp",
            "com.google.android.GoogleCameraCVM",
            "com.google.android.GoogleCameraEng",
            "com.google.android.GoogleCameraEng2",
            "com.google.android.MTCL83",
            "com.google.android.UltraCVM",
            "com.google.android.apps.cameralite"
    };

    public PropImitationFactory(Context context) {
        mPropsToChangePlatform = new HashMap<>();
        mPropsToChangeV1 = new HashMap<>();
        mPropsToChangeV2 = new HashMap<>();
        mPropsToKeep = new HashMap<>();
        String[] platformValue = context.getResources().getStringArray(R.array.config_systemPropertiesImitationPlatform);
        String[] propValueV1 = context.getResources().getStringArray(R.array.config_systemPropertiesImitationValuesV1);
        String[] propValueV2 = context.getResources().getStringArray(R.array.config_systemPropertiesImitationValuesV2);

        if (platformValue.length > 0) {
            mPropsToChangePlatform.put("BRAND", platformValue[0]);
            mPropsToChangePlatform.put("MANUFACTURER", platformValue[1]);
        }

        if (propValueV1.length > 0) {
            mPropsToChangeV1.put("DEVICE", propValueV1[0]);
            mPropsToChangeV1.put("PRODUCT", propValueV1[1]);
            mPropsToChangeV1.put("MODEL", propValueV1[2]);
            mPropsToChangeV1.put("FINGERPRINT", propValueV1[3]);
        }

        if (propValueV2.length > 0) {
            mPropsToChangeV2.put("DEVICE", propValueV2[0]);
            mPropsToChangeV2.put("PRODUCT", propValueV2[1]);
            mPropsToChangeV2.put("MODEL", propValueV2[2]);
            mPropsToChangeV2.put("FINGERPRINT", propValueV2[3]);
        }
    }

    public void setProps(String packageName) {
        if (packageName == null) {
            return;
        }
        mIsCallerPackageGms = packageName.equals(PACKAGE_GMS);

        if (mPropsToChangePlatform.isEmpty()) {
            // Realistically, we should be checking for each configuration
            // but, we can assume is the platform config isn't set, then
            // the others aren't as well.
            if (DEBUG) Log.d(TAG, "No imitated props were found, bailing out");
            return;
        }

        String stockBuildFingerprint = packageName.equals(PACKAGE_ARCORE)
                ? SystemProperties.get("ro.stock.build.fingerprint")
                : null;
        if (!stockBuildFingerprint.isEmpty()) {
            setPropValue("FINGERPRINT", stockBuildFingerprint);
            return;
        }

        if ((packageName.startsWith("com.google.") && !Arrays.asList(PACKAGES_TO_KEEP).contains(packageName))
                || Arrays.asList(PACKAGES_TO_CHANGE_EXTRA).contains(packageName)) {
            if (DEBUG) Log.d(TAG, "Defining props for: " + packageName);
            if (Arrays.asList(PACKAGES_TO_CHANGE_V1).contains(packageName)) {
                mPropsToChangePlatform.putAll(mPropsToChangeV1);
            } else {
                mPropsToChangePlatform.putAll(mPropsToChangeV2);
            }
            for (Map.Entry<String, Object> prop : mPropsToChangePlatform.entrySet()) {
                String key = prop.getKey();
                Object value = prop.getValue();
                if (mPropsToKeep.containsKey(packageName) && mPropsToKeep.get(packageName).contains(key)){
                    if (DEBUG) Log.d(TAG, "Not defining " + key + " prop for: " + packageName);
                    continue;
                }
                if (DEBUG) Log.d(TAG, "Defining " + key + " prop for: " + packageName);
                setPropValue(key, value);
            }
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

    private static boolean isCallerSafetyNet() {
        return Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet
        if (mIsCallerPackageGms && isCallerSafetyNet()) {
            throw new UnsupportedOperationException();
        }
    }
}
