/*
 * Copyright (C) 2023 Paranoid Android
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

import android.content.res.Resources;
import android.util.Slog;

import com.android.internal.R;

/** @hide */
public class RichTapVibrationEffect {

    private static final String TAG = "RichTapVibrationEffect";

    public static boolean isSupported() {
        return Resources.getSystem().getBoolean(R.bool.config_usesRichtapVibration);
    }

    public static int[] getInnerEffect(int id) {
        switch (id) {
            case 0:
                return new int[]{1, 4097, 0, 100, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case 1:
                return new int[]{1, 4097, 0, 100, 80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4097, 70, 100, 80, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case 2:
                return new int[]{1, 4097, 0, 100, 20, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case 3:
                return new int[]{1, 4097, 0, 100, 50, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case 4:
                return new int[]{1, 4097, 0, 100, 65, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case 5:
                return new int[]{1, 4097, 0, 100, 57, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            case 21:
                return new int[]{1, 4097, 0, 50, 33, 29, 0, 0, 0, 12, 59, 0, 22, 75, -21, 29, 0, 0, 4097, 30, 100, 30, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
            default:
                Slog.d(TAG, "Exception encountered!", new IllegalStateException("Unexpected effect id: " + id));
                return null;
        }
    }
}
