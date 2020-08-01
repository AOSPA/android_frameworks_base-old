/*
 * Copyright (C) 2020 Paranoid Android
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
 * limitations under the License
 */
package com.android.systemui.statusbar.phone;

import android.content.Context;
import android.graphics.Rect;
import android.net.TrafficStats;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

import com.android.systemui.Dependency;
import com.android.systemui.DualToneHandler;
import com.android.systemui.FontSizeUtils;
import com.android.systemui.R;
import com.android.systemui.plugins.DarkIconDispatcher;
import com.android.systemui.statusbar.policy.ConfigurationController;
import com.android.systemui.tuner.TunerService;

import java.math.BigDecimal;

/**
 * Network speed view for the status bar.
 */
public class NetworkSpeedView extends TextView implements DarkIconDispatcher.DarkReceiver,
        TunerService.Tunable, ConfigurationController.ConfigurationListener {

    private static final String TAG = "NetworkSpeedView";

    private static final int TRANSMIT_MEGA_UNIT_DECIMAL = 1024000;
    private static final float TRANSMIT_MEGA_UNIT_HEXIMAL = 1048576;
    private static final int TRANSMIT_UNIT_DECIMAL = 1000;
    private static final float TRANSMIT_UNIT_HEXIMAL = 1024;
    private static final int TRANSMIT_UNIT_THREE_DIGIT_LIMIT = 100;
    private static final int TRANSMIT_UNIT_TWO_DIGIT_LIMIT = 10;

    private final Runnable mRefreshUIRunnable = this::refreshSpeed;
    private final Handler mHandler = new Handler();

    private DualToneHandler mDualToneHandler;

    private String mSlotNetworkSpeed;

    private long mTotalBytes = -1;

    private boolean mBlockNetworkSpeed = true;

    /**
     * Color to be set on this {@link TextView}.
     */
    private int mNonAdaptedColor;

    public NetworkSpeedView(Context context) {
        this(context, null);
    }

    public NetworkSpeedView(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        mSlotNetworkSpeed = context.getString(com.android.internal.R.string.status_bar_network_speed);
        try {
            mNonAdaptedColor = getCurrentTextColor();
        } catch (RuntimeException ignored) {
        }

        mDualToneHandler = new DualToneHandler(context);
        onDarkChanged(new Rect(), 0.0f, -1);
    }

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        Dependency.get(TunerService.class).addTunable(this, StatusBarIconController.ICON_BLACKLIST);
        Dependency.get(ConfigurationController.class).addCallback(this);
        setTextColor(mNonAdaptedColor);
        refreshUI();
    }

    @Override
    public void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacks(mRefreshUIRunnable);
        Dependency.get(TunerService.class).removeTunable(this);
        Dependency.get(ConfigurationController.class).removeCallback(this);
    }

    @Override
    public void onDarkChanged(Rect area, float darkIntensity, int tint) {
        mNonAdaptedColor = DarkIconDispatcher.getTint(area, this, tint);
        setTextColor(mNonAdaptedColor);
    }

    private void refreshSpeed() {
        long totalRxBytes = TrafficStats.getTotalRxBytes() + TrafficStats.getTotalTxBytes();
        String rateContent = "";

        if (mTotalBytes < 0) {
            mTotalBytes = totalRxBytes;
        } else {
            long rxData = totalRxBytes - mTotalBytes;
            if (rxData > 0) {
                if (rxData < TRANSMIT_UNIT_DECIMAL) {
                    rateContent = getContext().getString(R.string.status_bar_network_speed_byte,
                            String.valueOf(rxData));
                } else {
                    int unitId;
                    float groupedSpeed;
                    if (rxData >= TRANSMIT_MEGA_UNIT_DECIMAL) {
                        unitId = R.string.status_bar_network_speed_mbyte;
                        groupedSpeed = rxData / TRANSMIT_MEGA_UNIT_HEXIMAL;
                    } else {
                        unitId = R.string.status_bar_network_speed_kbyte;
                        groupedSpeed = rxData / TRANSMIT_UNIT_HEXIMAL;
                    }
                    BigDecimal scaledSpeed = new BigDecimal(groupedSpeed);
                    if (groupedSpeed > TRANSMIT_UNIT_THREE_DIGIT_LIMIT) {
                        scaledSpeed = scaledSpeed.setScale(0, BigDecimal.ROUND_HALF_UP);
                    } else if (groupedSpeed > TRANSMIT_UNIT_TWO_DIGIT_LIMIT) {
                        scaledSpeed = scaledSpeed.setScale(1, BigDecimal.ROUND_HALF_UP);
                    } else {
                        scaledSpeed = scaledSpeed.setScale(2, BigDecimal.ROUND_HALF_UP);
                    }
                    rateContent = getContext().getString(unitId, scaledSpeed.toString());
                }
            }
        }

        setText(rateContent);
        mHandler.postDelayed(mRefreshUIRunnable, 1000);
    }

    private void refreshUI() {
        mHandler.removeCallbacks(mRefreshUIRunnable);
        if (!mBlockNetworkSpeed) {
            mHandler.post(mRefreshUIRunnable);
            setVisibility(View.VISIBLE);
        } else {
            setVisibility(View.GONE);
        }
    }

    public void updateNetworkSpeedView() {
        setTextColor(mNonAdaptedColor);
        refreshUI();
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        boolean contains = StatusBarIconController.getIconBlacklist(newValue).contains(
                mSlotNetworkSpeed);
        if (StatusBarIconController.ICON_BLACKLIST.equals(key) && mBlockNetworkSpeed != contains) {
            mBlockNetworkSpeed = contains;
            refreshUI();
        }
    }

    @Override
    public void onDensityOrFontScaleChanged() {
        FontSizeUtils.updateFontSize(this, R.dimen.status_bar_clock_size);
        setPaddingRelative(0,
                0,
                getContext().getResources().getDimensionPixelSize(
                        R.dimen.network_speed_padding_end),
                0);
    }

    public void setColorsFromContext(Context context) {
        if (context == null) {
            return;
        }

        mDualToneHandler.setColorsFromContext(context);
    }
}
