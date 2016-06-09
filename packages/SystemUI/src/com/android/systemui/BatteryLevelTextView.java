/*
 * Copyright (C) 2014-2016 The CyanogenMod Project
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

package com.android.systemui;

import android.content.Context;
import android.icu.text.NumberFormat;
import android.provider.Settings;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.statusbar.policy.BatteryController;

public class BatteryLevelTextView extends TextView implements
        BatteryController.BatteryStateChangeCallback {

    private static final String STATUS_BAR_SHOW_BATTERY_PERCENT =
            Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT;

    private static final String STATUS_BAR_BATTERY_STYLE =
            Settings.System.STATUS_BAR_BATTERY_STYLE;

    private BatteryController mBatteryController;

    public BatteryLevelTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
        setText(NumberFormat.getPercentInstance().format((double) level / 100.0));
    }

    public void setBatteryController(BatteryController batteryController) {
        mBatteryController = batteryController;
        mBatteryController.addStateChangedCallback(this);
    }

    @Override
    public void onPowerSaveChanged(boolean isPowerSave) {
        // Unused
    }

    @Override
    public void onBatteryStyleChanged(int style, int percentMode) {
        switch (style) {
            case BatteryMeterDrawable.BATTERY_STYLE_TEXT:
                setVisibility(View.VISIBLE);
                break;
            case BatteryMeterDrawable.BATTERY_STYLE_HIDDEN:
                setVisibility(View.GONE);
                break;
            default:
                setVisibility(percentMode == 2 ? View.VISIBLE : View.GONE);
                break;
        }
        // Unused
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mBatteryController != null) {
            mBatteryController.removeStateChangedCallback(this);
        }
    }
}
