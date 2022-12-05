/*
 * Copyright (C) 2022 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.android.systemui.util;

import android.content.Context;
import android.os.RichTapVibrationEffect;
import android.util.Log;

import com.android.internal.R;
import com.sysaac.haptic.AACHapticUtils;

import java.io.File;

public class RichTapVibrationUtils {

    private static RichTapVibrationUtils sInstance;
    private AACHapticUtils mAACHapticUtils;
    private Context mContext;
    private boolean mSupportRichTap;

    public static RichTapVibrationUtils getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new RichTapVibrationUtils(context);
        }
        return sInstance;
    }

    public RichTapVibrationUtils(Context context) {
        mContext = context.getApplicationContext();
        mAACHapticUtils = AACHapticUtils.getInstance();
        mAACHapticUtils.init(mContext);
        mSupportRichTap = mAACHapticUtils.isSupportedRichTap();
    }

    public boolean playVerityVibrate(String heFile) {
        if (!mSupportRichTap) {
            return false;
        }
        File file = new File(heFile);
        if (!file.exists()) {
            return false;
        }
        mAACHapticUtils.playPattern(file, 1);
        return true;
    }

    public boolean isSupported() {
        return RichTapVibrationEffect.isSupported();
    }
}
