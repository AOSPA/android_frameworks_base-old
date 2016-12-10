/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.systemui.qs.tiles;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.nfc.NfcAdapter;
import android.provider.Settings;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Enable/disable NFC **/
public class NfcTile extends QSTile<QSTile.BooleanState> {

    private NfcAdapter mAdapter;

    private boolean mIsNfcSupported;
    private boolean mListening;

    public NfcTile(Host host) {
        super(host);
        try {
            mAdapter = NfcAdapter.getNfcAdapter(mContext);
        // Don't crash if NFC is unsupported
        } catch (UnsupportedOperationException e) {
            mAdapter = null;
        }
    }

    @Override
    protected void handleDestroy() {
        super.handleDestroy();
        mContext.unRegisterReceiver(mNfcReceiver);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening || mAdapter == null) return;
        mListening = listening;
        if (mListening) {
            mContext.registerReceiver(mNfcReceiver,
                    new IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED));
        }
    }

    @Override
    public boolean isAvailable() {
        return mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_NFC);
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent(Settings.ACTION_NFC_SETTINGS);
    }

    @Override
    protected void handleClick() {
        MetricsLogger.action(mContext, getMetricsCategory(), !mState.value);
        if (mAdapter != null) {
            if (!mAdapter.isEnabled()) {
                mAdapter.enable();
            } else {
                mAdapter.disable();
            }
        }
    }

    @Override
    protected void handleSecondaryClick() {
        handleClick();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_inversion_label);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = mContext.getString(R.string.quick_settings_nfc_label);
        if (!isAvailable() || mAdapter == null) {
            Drawable icon = mContext.getDrawable(R.drawable.ic_qs_nfc_disabled).mutate();
            final int disabledColor = mContext.getColor(R.color.qs_tile_tint_unavailable);
            icon.setTint(disabledColor);
            state.icon = new DrawableIcon(icon);
            state.label = new SpannableStringBuilder().append(state.label,
                    new ForegroundColorSpan(disabledColor),
                    SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);
            return;
        }
        state.value = mAdapter.isEnabled();
        state.icon = ResourceIcon.get(R.drawable.ic_qs_nfc_disabled);//state.value ? mEnable : mDisable;
        state.minimalAccessibilityClassName = state.expandedAccessibilityClassName
                = Switch.class.getName();
        state.contentDescription = state.label;
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_NFC;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.quick_settings_nfc_on);
        } else {
            return mContext.getString(R.string.quick_settings_nfc_off);
        }
    }

    private BroadcastReceiver mNfcReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            refreshState();
        }
    };
}
