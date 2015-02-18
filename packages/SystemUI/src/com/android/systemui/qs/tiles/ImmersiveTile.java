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

import android.provider.Settings.System;
import android.view.View;
import com.android.systemui.R;
import com.android.systemui.qs.SystemSetting;
import com.android.systemui.qs.QSTile;

/** Quick settings tile: Immersive mode **/
public class ImmersiveTile extends QSTile<QSTile.BooleanState> {
    private final SystemSetting mSetting;

    private static final int IMMERSIVE_OFF = 0;
    private static final int IMMERSIVE_FLAGS = View.SYSTEM_UI_FLAG_IMMERSIVE |
                                                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    private static final int IMMERSIVE_FLAGS_FULL = IMMERSIVE_FLAGS |
                                                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                                                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;

    private boolean mListening;

    public ImmersiveTile(Host host) {
        super(host);

        mSetting = new SystemSetting(mContext, mHandler, System.SYSTEM_UI_FLAGS) {
            @Override
            protected void handleValueChanged(int value) {
                handleRefreshState(value);
            }
        };
    }

    @Override
    protected void handleUserSwitch(int newUserId) {
        mSetting.setUserId(newUserId);
        handleRefreshState(mSetting.getValue());
    }
    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void handleClick() {
        setEnabled(!mState.value);
    }

    private void setEnabled(boolean enabled) {
        mSetting.setValue(enabled ? IMMERSIVE_FLAGS_FULL : IMMERSIVE_OFF);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = mSetting.getValue();
        final boolean immersiveMode = value != 0;
        state.value = immersiveMode;
        state.visible = true;
        state.label = mContext.getString(R.string.quick_settings_immersive_mode_label);
        if (immersiveMode) {
            state.iconId =  R.drawable.ic_qs_immersive_full;
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_immersive_mode_on);
        } else {
            state.iconId = R.drawable.ic_qs_immersive_off;
            state.contentDescription =  mContext.getString(
                    R.string.accessibility_quick_settings_immersive_mode_off);
        }
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_immersive_mode_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_immersive_mode_changed_off);
        }
    }

    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        mSetting.setListening(listening);
    }
}
