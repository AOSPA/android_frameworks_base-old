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

import com.android.internal.logging.MetricsLogger;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.ListView;
import com.android.systemui.R;
import com.android.systemui.qs.QSDetailItemsList;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.KeyguardMonitor;
import com.android.systemui.statusbar.policy.LocationController;
import com.android.systemui.statusbar.policy.LocationController.LocationSettingsChangeCallback;
import com.android.systemui.volume.SegmentedButtons;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Quick settings tile: Location **/
public class LocationTile extends QSTile<QSTile.BooleanState> {
    public static final String SPEC = "location";

    private static final Intent LOCATION_SETTINGS_INTENT
            = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
    public static final Integer[] LOCATION_SETTINGS = new Integer[]{
            Settings.Secure.LOCATION_MODE_HIGH_ACCURACY,
            Settings.Secure.LOCATION_MODE_BATTERY_SAVING,
            Settings.Secure.LOCATION_MODE_SENSORS_ONLY
    };

    private final AnimationIcon mEnable =
            new AnimationIcon(R.drawable.ic_signal_location_enable_animation);
    private final AnimationIcon mDisable =
            new AnimationIcon(R.drawable.ic_signal_location_disable_animation);

    private final List<Integer> mLocationList = new ArrayList<>();
    private final LocationController mController;
    private final LocationDetailAdapter mDetailAdapter;
    private final KeyguardMonitor mKeyguard;
    private final Callback mCallback = new Callback();
    private int mLastState;

    public LocationTile(Host host) {
        super(host, SPEC);
        mController = host.getLocationController();
        mDetailAdapter = new LocationDetailAdapter();
        mKeyguard = host.getKeyguardMonitor();
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    protected BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mController.addSettingsChangedCallback(mCallback);
            mKeyguard.addCallback(mCallback);
        } else {
            mController.removeSettingsChangedCallback(mCallback);
            mKeyguard.removeCallback(mCallback);
        }
    }

    @Override
    protected void handleToggleClick() {
        final boolean wasEnabled = mState.value;
        MetricsLogger.action(mContext, getMetricsCategory(), !wasEnabled);
        mController.setLocationEnabled(!wasEnabled);
        mEnable.setAllowAnimation(true);
        mDisable.setAllowAnimation(true);
    }

    @Override
    protected void handleDetailClick() {
        if (!mState.value) {
            mState.value = true;
            mController.setLocationEnabled(true);
        }
        showDetail(true);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        final int currentState = mController.getLocationCurrentState();
        final boolean locationEnabled = currentState != Settings.Secure.LOCATION_MODE_OFF;

        // Work around for bug 15916487: don't show location tile on top of lock screen. After the
        // bug is fixed, this should be reverted to only hiding it on secure lock screens:
        // state.visible = !(mKeyguard.isSecure() && mKeyguard.isShowing());
        state.visible = !mKeyguard.isShowing();
        state.value = locationEnabled;
        state.label = mContext.getString(getStateLabelRes(currentState));

        switch (currentState) {
            case Settings.Secure.LOCATION_MODE_OFF:
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_location_off);
                state.icon = mDisable;
                break;
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_location_high_accuracy);
                state.icon = mEnable;
                break;
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_location_battery_saving);
                state.icon = ResourceIcon.get(R.drawable.ic_qs_location_battery_saving);
                break;
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_location_gps_only);
                state.icon = ResourceIcon.get(R.drawable.ic_qs_location_sensors_only);
                break;
            default:
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_location_on);
        }
    }

    private int getStateLabelRes(int currentState) {
        switch (currentState) {
            case Settings.Secure.LOCATION_MODE_OFF:
                return R.string.quick_settings_location_off_label;
            case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                return R.string.quick_settings_location_high_accuracy_label;
            case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                return R.string.quick_settings_location_battery_saving_label;
            case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                return R.string.quick_settings_location_gps_only_label;
            default:
                return R.string.quick_settings_location_label;
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_LOCATION;
    }

    @Override
    protected String composeChangeAnnouncement() {
        if (mState.value) {
            return mContext.getString(R.string.accessibility_quick_settings_location_changed_on);
        } else {
            return mContext.getString(R.string.accessibility_quick_settings_location_changed_off);
        }
    }

    private final class Callback implements LocationSettingsChangeCallback,
            KeyguardMonitor.Callback {
        @Override
        public void onLocationSettingsChanged(boolean enabled) {
            refreshState();
        }

        @Override
        public void onKeyguardChanged() {
            refreshState();
        }
    };

    private class LocationDetailAdapter implements DetailAdapter, AdapterView.OnItemClickListener {

        private SegmentedButtons mButtons;
        private ViewGroup mMessageContainer;
        private TextView mMessageText;

        @Override
        public int getMetricsCategory() {
            return MetricsLogger.LOCATION_DETAIL_ADAPTER;
        }

        @Override
        public int getTitle() {
            return R.string.quick_settings_location_detail_title;
        }

        @Override
        public Boolean getToggleState() {
            return mState.value;
        }

        @Override
        public Intent getSettingsIntent() {
            return LOCATION_SETTINGS_INTENT;
        }

        @Override
        public void setToggleState(boolean state) {
            mController.setLocationEnabled(state);
            showDetail(false);
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final LinearLayout details = convertView != null ? (LinearLayout) convertView
                    : (LinearLayout) LayoutInflater.from(context).inflate(
                            R.layout.location_mode_panel, parent, false);

            mLastState = mController.getLocationCurrentState();

            if (convertView == null) {
                mButtons = (SegmentedButtons) details.findViewById(R.id.location_buttons);
                mButtons.addButton(R.string.quick_settings_location_high_accuracy_label_twoline,
                        R.string.quick_settings_location_high_accuracy_label,
                        Settings.Secure.LOCATION_MODE_HIGH_ACCURACY);
                mButtons.addButton(R.string.quick_settings_location_battery_saving_label_twoline,
                        R.string.quick_settings_location_battery_saving_label,
                        Settings.Secure.LOCATION_MODE_BATTERY_SAVING);
                mButtons.addButton(R.string.quick_settings_location_gps_only_label_twoline,
                        R.string.quick_settings_location_gps_only_label,
                        Settings.Secure.LOCATION_MODE_SENSORS_ONLY);
                mButtons.setCallback(mButtonsCallback);
                mMessageContainer = (ViewGroup) details.findViewById(R.id.location_introduction);
                mMessageText = (TextView) details.findViewById(R.id.location_introduction_message);
                mMessageContainer.setVisibility(View.GONE);
                mButtons.setSelectedValue(mLastState, false /* fromClick */);
                refresh(mLastState);
            }

            return details;
        }

        private void refresh(int state) {
            mController.setLocationMode(mLastState);
            switch (state) {
                case Settings.Secure.LOCATION_MODE_HIGH_ACCURACY:
                    mMessageText.setText(mContext.getString(R.string.quick_settings_location_detail_mode_high_accuracy_description));
                    mMessageContainer.setVisibility(View.VISIBLE);
                    break;
                case Settings.Secure.LOCATION_MODE_BATTERY_SAVING:
                    mMessageText.setText(mContext.getString(R.string.quick_settings_location_detail_mode_battery_saving_description));
                    mMessageContainer.setVisibility(View.VISIBLE);
                    break;
                case Settings.Secure.LOCATION_MODE_SENSORS_ONLY:
                    mMessageText.setText(mContext.getString(R.string.quick_settings_location_detail_mode_sensors_only_description));
                    mMessageContainer.setVisibility(View.VISIBLE);
                    break;
                default:
                    mMessageContainer.setVisibility(View.GONE);
                    break;
            }
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            mController.setLocationMode((Integer) parent.getItemAtPosition(position));
        }

        private final SegmentedButtons.Callback mButtonsCallback = new SegmentedButtons.Callback() {
            @Override
            public void onSelected(final Object value, boolean fromClick) {
                if (value != null && mButtons.isShown()) {
                    mLastState = (Integer) value;
                    if (fromClick) {
                        MetricsLogger.action(mContext, MetricsLogger.QS_LOCATION, mLastState);
                        refresh(mLastState);
                    }
                }
            }

            @Override
            public void onInteraction() {
            }
        };
    }
}
