/*
 * Copyright (C) 2015 The ParanoidAndroid Project
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

import static android.view.View.SYSTEM_DESIGN_FLAG_IMMERSIVE_NAV;
import static android.view.View.SYSTEM_DESIGN_FLAG_IMMERSIVE_STATUS;

import android.content.Context;
import android.content.Intent;
import android.os.UserHandle;
import android.provider.Settings.Secure;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.SecureSetting;
import com.android.systemui.qs.QSDetailItemsList;
import com.android.systemui.qs.QSTile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Quick settings tile: Immersive mode **/
public class ImmersiveTile extends QSTile<QSTile.BooleanState> {
    public static final String SPEC = "immersive";

    private static final int IMMERSIVE_OFF = 0;
    private static final int IMMERSIVE_FLAGS_FULL = SYSTEM_DESIGN_FLAG_IMMERSIVE_STATUS |
                                          SYSTEM_DESIGN_FLAG_IMMERSIVE_NAV;

    public static final Integer[] IMMERSIVE_STATES = new Integer[]{
            IMMERSIVE_FLAGS_FULL,
            SYSTEM_DESIGN_FLAG_IMMERSIVE_NAV,
            SYSTEM_DESIGN_FLAG_IMMERSIVE_STATUS
    };

    private final SecureSetting mSetting;
    private final ImmersiveDetailAdapter mDetailAdapter;

    private final List<Integer> mDetailList = new ArrayList<>();

    private boolean mListening;
    private int mLastState;

    public ImmersiveTile(Host host) {
        super(host, SPEC);

        mSetting = new SecureSetting(mContext, mHandler, Secure.SYSTEM_DESIGN_FLAGS) {
            @Override
            protected void handleValueChanged(int value, boolean observedChange) {
                handleRefreshState(value);
            }
        };
        mDetailAdapter = new ImmersiveDetailAdapter();
        mLastState = Secure.getIntForUser(mContext.getContentResolver(), Secure.LAST_SYSTEM_DESIGN_FLAGS,
                IMMERSIVE_FLAGS_FULL, UserHandle.USER_CURRENT);
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
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
    public void handleToggleClick() {
        setEnabled(!mState.value);
    }

    @Override
    public void handleDetailClick() {
        showDetail(true);
    }

    private void setEnabled(boolean enabled) {
        mSetting.setValue(enabled ? mLastState : IMMERSIVE_OFF);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int value = mSetting.getValue();
        final boolean immersiveMode = value != IMMERSIVE_OFF;
        state.value = value != IMMERSIVE_OFF;
        state.visible = true;
        switch (value) {
            case IMMERSIVE_FLAGS_FULL:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_immersive_full);
                state.label = mContext.getString(R.string.quick_settings_immersive_mode_label_hide_all);
                state.contentDescription =  mContext.getString(
                        R.string.accessibility_quick_settings_immersive_mode_full);
                break;
            case SYSTEM_DESIGN_FLAG_IMMERSIVE_NAV:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_immersive_hide_nav_bar);
                state.label = mContext.getString(R.string.quick_settings_immersive_mode_label_hide_nav);
                state.contentDescription =  mContext.getString(
                        R.string.accessibility_quick_settings_immersive_mode_hide_nav_bar);
                break;
            case SYSTEM_DESIGN_FLAG_IMMERSIVE_STATUS:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_immersive_hide_status_bar);
                state.label = mContext.getString(R.string.quick_settings_immersive_mode_label_hide_status);
                state.contentDescription =  mContext.getString(
                        R.string.accessibility_quick_settings_immersive_mode_hide_status_bar);
                break;
            default:
                state.icon = ResourceIcon.get(R.drawable.ic_qs_immersive_off);
                state.label = mContext.getString(R.string.quick_settings_immersive_mode_label);
                state.contentDescription =  mContext.getString(
                        R.string.accessibility_quick_settings_immersive_mode_off);
                break;
        }
    }

    private int getStateLabelRes(int currentState) {
        switch (currentState) {
            case IMMERSIVE_FLAGS_FULL:
                return R.string.quick_settings_immersive_mode_detail_hide_all;
            case SYSTEM_DESIGN_FLAG_IMMERSIVE_NAV:
                return R.string.quick_settings_immersive_mode_detail_hide_nav;
            case SYSTEM_DESIGN_FLAG_IMMERSIVE_STATUS:
                return R.string.quick_settings_immersive_mode_detail_hide_status;
            default:
                return R.string.quick_settings_immersive_mode_label;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_IMMERSIVE;
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

    private class ImmersiveAdapter extends ArrayAdapter<Integer> {
        public ImmersiveAdapter(Context context) {
            super(context, android.R.layout.simple_list_item_single_choice, mDetailList);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = LayoutInflater.from(mContext);
            CheckedTextView label = (CheckedTextView) inflater.inflate(
                    android.R.layout.simple_list_item_single_choice, parent, false);
            label.setText(getStateLabelRes(getItem(position)));
            return label;
        }
    }

    private final class ImmersiveDetailAdapter implements DetailAdapter, AdapterView.OnItemClickListener {

        private ImmersiveAdapter mAdapter;
        private QSDetailItemsList mDetails;

        @Override
        public int getTitle() {
            return R.string.quick_settings_immersive_mode_label;
        }

        @Override
        public Boolean getToggleState() {
            rebuildDetailList(mState.value);
            return mState.value;
        }

        @Override
        public Intent getSettingsIntent() {
            return null;
        }

        @Override
        public void setToggleState(boolean state) {
            setEnabled(state);
            rebuildDetailList(state);
            fireToggleStateChanged(state);
        }

        @Override
        public int getMetricsCategory() {
            return MetricsLogger.QS_IMMERSIVE_DETAILS;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            mDetails = QSDetailItemsList.convertOrInflate(context, convertView, parent);
            mDetails.setEmptyState(R.drawable.ic_qs_immersive_off,
                    R.string.accessibility_quick_settings_immersive_mode_off);
            mAdapter = new ImmersiveTile.ImmersiveAdapter(context);
            mDetails.setAdapter(mAdapter);

            final ListView list = mDetails.getListView();
            list.setChoiceMode(AbsListView.CHOICE_MODE_SINGLE);
            list.setOnItemClickListener(this);

            return mDetails;
        }

        private void rebuildDetailList(boolean populate) {
            mDetailList.clear();
            if(populate) {
                mDetailList.addAll(Arrays.asList(IMMERSIVE_STATES));
                mDetails.getListView().setItemChecked(mAdapter.getPosition(
                        mLastState), true);
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mLastState = (Integer) parent.getItemAtPosition(position);
            fireToggleStateChanged(true);
            mSetting.setValue(mLastState);
            Secure.putIntForUser(mContext.getContentResolver(), Secure.LAST_SYSTEM_DESIGN_FLAGS,
                    mLastState, UserHandle.USER_CURRENT);
        }
    }
}
