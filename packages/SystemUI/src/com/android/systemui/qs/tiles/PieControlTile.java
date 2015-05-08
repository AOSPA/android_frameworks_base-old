/*
 * Copyright (C) 2015 The Fusion Project
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
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.systemui.R;
import com.android.systemui.qs.QSTile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PieControlTile extends QSTile<QSTile.BooleanState> {
    public static final String SPEC = "pie";

    private boolean mListening;
    private PieControlObserver mObserver;

    public PieControlTile(Host host) {
        super(host, SPEC);
        mObserver = new PieControlObserver(mHandler);
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleToggleClick() {
        toggleState();
        refreshState();
    }

    @Override
    protected void handleDetailClick() {
        // There are no additional details and we do not want to link this up to Display Settings.
        handleToggleClick();
    }

 protected void toggleState() {
         Settings.System.putInt(mContext.getContentResolver(),
                        Settings.System.PA_PIE_STATE, !piecontrolEnabled() ? 1 : 0);
    }


    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        state.visible = true;
	if (piecontrolEnabled()) {
        state.icon = ResourceIcon.get(R.drawable.ic_qs_pie_global_on);
        state.label = mContext.getString(R.string.quick_settings_pa_pie_on);
	} else {
        state.icon = ResourceIcon.get(R.drawable.ic_qs_pie_global_off);
	state.label = mContext.getString(R.string.quick_settings_pa_pie_off);
	    }
	}

    private boolean piecontrolEnabled() {
        return Settings.System.getInt(mContext.getContentResolver(),
                Settings.System.PA_PIE_STATE, 1) == 1;
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (listening) {
            mObserver.startObserving();
        } else {
            mObserver.endObserving();
        }
    }

    private class PieControlObserver extends ContentObserver {
        public PieControlObserver(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            refreshState();
        }

        public void startObserving() {
            mContext.getContentResolver().registerContentObserver(
                    Settings.System.getUriFor(Settings.System.PA_PIE_STATE),
                    false, this);
        }

        public void endObserving() {
            mContext.getContentResolver().unregisterContentObserver(this);
        }
    }
}
