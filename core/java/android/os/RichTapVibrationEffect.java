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
import android.os.VibrationEffect;
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
            case VibrationEffect.EFFECT_CLICK:
                return getInnerEffectFromOverlay(R.string.config_richtapVibrationEffectDoubleClick);
            case VibrationEffect.EFFECT_DOUBLE_CLICK:
                return getInnerEffectFromOverlay(R.string.config_richtapVibrationEffectDoubleClick);
            case VibrationEffect.EFFECT_TICK:
                return getInnerEffectFromOverlay(R.string.config_richtapVibrationEffectTick);
            case VibrationEffect.EFFECT_THUD:
                return getInnerEffectFromOverlay(R.string.config_richtapVibrationEffectThud);
            case VibrationEffect.EFFECT_POP:
                return getInnerEffectFromOverlay(R.string.config_richtapVibrationEffectPop);
            case VibrationEffect.EFFECT_HEAVY_CLICK:
                return getInnerEffectFromOverlay(R.string.config_richtapVibrationEffectHeavyClick);
            case VibrationEffect.EFFECT_TEXTURE_TICK:
                return getInnerEffectFromOverlay(R.string.config_richtapVibrationEffectTextureTick);
            default:
                Slog.d(TAG, "Exception encountered!", new IllegalStateException("Unexpected effect id: " + id));
                return null;
        }
    }

    private static int[] getInnerEffectFromOverlay(int id) {
        try {
            String effectRaw = Resources.getSystem().getString(id);
            String[] effectArrString = effectRaw.replaceAll(" ", "").split(",");
            int[] effectArrInt = new int[effectArrString.length];
            for (int i = 0; i < effectArrString.length; i++) {
                effectArrInt[i] = Integer.valueOf(effectArrString[i]);
            }
            return effectArrInt;
        } catch {
            Slog.d(TAG, "Exception encountered!", new IllegalStateException("Unexpected issue converting effect"));
            return null;
        }
    }

    public static int getInnerEffectStrength(int strength) {
        switch (strength) {
            case VibrationEffect.EFFECT_STRENGTH_LIGHT:
                return 150;
            case VibrationEffect.EFFECT_STRENGTH_MEDIUM:
                return 200;
            case VibrationEffect.EFFECT_STRENGTH_STRONG:
                return 250;
            default:
                Slog.e(TAG, "Wrong Effect Strength!!");
                return 0;
        }
    }
}
