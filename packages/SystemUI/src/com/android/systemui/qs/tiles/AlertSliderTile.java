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
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsLogger;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.SegmentedButtons;

/** Quick settings tile: Alert slider **/
public class AlertSliderTile extends QSTile<QSTile.State>  {

    private static final Intent ZEN_SETTINGS =
            new Intent(Settings.ACTION_ZEN_MODE_SETTINGS);

    private static final QSTile.Icon TOTAL_SILENCE =
            ResourceIcon.get(R.drawable.ic_qs_dnd_on_total_silence);

    private static final QSTile.Icon ALARMS_ONLY =
            ResourceIcon.get(R.drawable.ic_qs_dnd_on);

    public static final String SPEC = "alertslider";

    private final ZenModeController mController;
    private final AlertSliderDetailAdapter mDetailAdapter;

    private boolean mListening;

    public AlertSliderTile(Host host) {
        super(host, SPEC);
        mController = host.getZenModeController();
        mDetailAdapter = new AlertSliderDetailAdapter();
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    protected State newTileState() {
        return new State();
    }

    @Override
    protected void handleToggleClick() {
        showDetail(true);
    }

    @Override
    protected void handleDetailClick() {
        showDetail(true);
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        final int zen = arg instanceof Integer ? (Integer) arg : mController.getZen();
        switch (zen) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                state.visible = ZenModeConfig.hasAlertSlider(mContext);
                state.icon = TOTAL_SILENCE;
                state.label = mContext.getString(R.string.quick_settings_dnd_none_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_none_on);
                if (mDetailAdapter != null) {
                    mDetailAdapter.setMessageText(R.string.quick_settings_alert_slider_detail_no_interruptions_description);
                }
                break;
            case Settings.Global.ZEN_MODE_ALARMS:
                state.visible = ZenModeConfig.hasAlertSlider(mContext);
                state.icon = ALARMS_ONLY;
                state.label = mContext.getString(R.string.quick_settings_dnd_alarms_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_alarms_on);
                if (mDetailAdapter != null) {
                    mDetailAdapter.setMessageText(R.string.quick_settings_alert_slider_detail_alarms_only_description);
                }
                break;
            default:
                state.visible = false;
                break;
        }
    }

    private int getZenMode() {
        return mController.getZen();
    }

    private void setZenMode(int mode) {
        mController.setZen(mode, null, TAG);
        mHost.collapsePanels();
    }

    @Override
    public int getMetricsCategory() {
        return MetricsLogger.QS_DND;
    }

    @Override
    protected String composeChangeAnnouncement() {
        return "";
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening && ZenModeConfig.hasAlertSlider(mContext);
        if (mListening) {
            mController.addCallback(mZenCallback);
        } else {
            mController.removeCallback(mZenCallback);
        }
    }

    private final ZenModeController.Callback mZenCallback = new ZenModeController.Callback() {
        public void onZenChanged(int zen) {
            refreshState(zen);
        }
    };

    private final class AlertSliderDetailAdapter implements DetailAdapter, AdapterView.OnItemClickListener {

        private SegmentedButtons mButtons;
        private TextView mMessageText;

        @Override
        public int getTitle() {
            return R.string.quick_settings_alert_slider_title;
        }

        @Override
        public Boolean getToggleState() {
            return null;
        }

        @Override
        public Intent getSettingsIntent() {
            return ZEN_SETTINGS;
        }

        @Override
        public void setToggleState(boolean state) { }

        @Override
        public int getMetricsCategory() {
            return MetricsLogger.QS_DND_DETAILS;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final LinearLayout details = convertView != null ? (LinearLayout) convertView
                    : (LinearLayout) LayoutInflater.from(context).inflate(
                    R.layout.alert_slider_panel, parent, false);

            int state = getZenMode();

            if (convertView == null) {
                mButtons = (SegmentedButtons) details.findViewById(R.id.alert_slider_buttons);
                mButtons.addButton(R.string.quick_settings_alert_slider_alarms_only_label_twoline,
                        R.string.quick_settings_alert_slider_alarms_only_label,
                        Settings.Global.ZEN_MODE_ALARMS);
                mButtons.addButton(R.string.quick_settings_alert_slider_no_interruptions_label_twoline,
                        R.string.quick_settings_alert_slider_no_interruptions_label,
                        Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
                mButtons.setCallback(mButtonsCallback);
                mMessageText = (TextView) details.findViewById(R.id.alert_slider_introduction_message);
                mButtons.setSelectedValue(state, false /* fromClick */);
            }

            return details;
        }

        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) { }

        private final SegmentedButtons.Callback mButtonsCallback = new SegmentedButtons.Callback() {

            @Override
            public void onSelected(final Object value, boolean fromClick) {
                if (value != null && mButtons.isShown()) {
                    int state = (Integer) value;
                    if (fromClick) {
                        MetricsLogger.action(mContext, MetricsLogger.QS_DND, state);
                        setSilentState(state);
                        setZenMode(state);
                        refreshState(state);
                        showDetail(false);
                    }
                }
            }

            @Override
            public void onInteraction() { }
        };

        public void setMessageText(int stringRes) {
            if (mMessageText != null) {
                mMessageText.setText(mContext.getString(stringRes));
            }
        }

        public void setSilentState(int state) {
            int silentMode = state != Settings.Global.ZEN_MODE_ALARMS ? 1 : 0;
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.ALERT_SLIDER_SILENT_MODE, silentMode, UserHandle.USER_CURRENT);
        }
    }
}
