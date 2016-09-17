/*
 * Copyright (C) 2016 The ParanoidAndroid Project
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

import android.content.Context;
import android.content.Intent;
import android.os.HybridManager;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.logging.MetricsLogger;

import com.android.systemui.qs.QSTile;
import com.android.systemui.R;

public class HybridTile extends QSTile<QSTile.BooleanState> {
    private HybridManager mHybridManager;

    public HybridTile(Host host) {
        super(host);
        mHybridManager = (HybridManager) mContext.getSystemService(Context.HYBRID_SERVICE);
    }

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public CharSequence getTileLabel() {
        return "Hybrid Tile";
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent();
    }

    @Override
    public void handleClick() {
        mHybridManager.injectDummyProps();
    }

    @Override
    public void handleSecondaryClick() {
        handleClick();
    }

    @Override
    public void handleLongClick() {/* Do nothing */}

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_IMMERSIVE;
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.label = "hybrid tile";
        state.icon = ResourceIcon.get(R.drawable.ic_qs_immersive_full);
    }

    @Override
    public void setListening(boolean listening) {/* Do nothing */}
}
