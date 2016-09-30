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
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.provider.Settings;
import android.service.notification.ZenModeConfig;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
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

    private static final QSTile.Icon DISABLED =
            ResourceIcon.get(R.drawable.ic_qs_dnd_off);

    private final ZenModeController mController;
    private final AlertSliderDetailAdapter mDetailAdapter;

    private boolean mListening;

    public AlertSliderTile(Host host) {
        super(host);
        mController = host.getZenModeController();
        mDetailAdapter = new AlertSliderDetailAdapter();
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_alert_slider_title);
    }

    @Override
    public DetailAdapter getDetailAdapter() {
        return mDetailAdapter;
    }

    @Override
    public Intent getLongClickIntent() {
        return new Intent();
    }

    @Override
    public State newTileState() {
        return new State();
    }

    @Override
    protected void handleClick() {
        if (shouldShow()) {
            showDetail(true);
        }
    }

    @Override
    protected void handleSecondaryClick() {
        // Secondary clicks are header clicks, just toggle.
        handleClick();
    }

    @Override
    public boolean isAvailable() {
        return ZenModeConfig.hasAlertSlider(mContext);
    }

    private boolean shouldShow() {
        return (getZenMode() == Settings.Global.ZEN_MODE_NO_INTERRUPTIONS
                || getZenMode() == Settings.Global.ZEN_MODE_ALARMS);
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        final int zen = arg instanceof Integer ? (Integer) arg : getZenMode();
        state.icon = DISABLED;
        state.label = mContext.getString(R.string.quick_settings_alert_slider_title);
        if (!shouldShow()) {
            Drawable icon = mContext.getDrawable(R.drawable.ic_qs_dnd_off)
                    .mutate();
            final int disabledColor = mContext.getColor(R.color.qs_tile_tint_unavailable);
            icon.setTint(disabledColor);
            state.icon = new DrawableIcon(icon);
            state.label = new SpannableStringBuilder().append(state.label,
                    new ForegroundColorSpan(disabledColor),
                    SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE);

            state.contentDescription = mContext.getString(
                    R.string.quick_settings_alert_slider_unavailable);
            return;
        }
        switch (zen) {
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                state.icon = TOTAL_SILENCE;
                state.label = mContext.getString(R.string.quick_settings_dnd_none_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_none_on);
                if (mDetailAdapter != null) {
                    mDetailAdapter.setMessageText(R.string.quick_settings_alert_slider_detail_no_interruptions_description);
                }
                break;
            case Settings.Global.ZEN_MODE_ALARMS:
                state.icon = ALARMS_ONLY;
                state.label = mContext.getString(R.string.quick_settings_dnd_alarms_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_alarms_on);
                if (mDetailAdapter != null) {
                    mDetailAdapter.setMessageText(R.string.quick_settings_alert_slider_detail_alarms_only_description);
                }
                break;
            default:
                break;
        }
    }

    private int getZenMode() {
        return mController.getZen();
    }

    private void setZenMode(int mode) {
        mController.setZen(mode, null, TAG);
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_DND;
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

    private final class AlertSliderDetailAdapter implements DetailAdapter {

        private SegmentedButtons mButtons;
        private TextView mMessageText;

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_alert_slider_title);
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
            return MetricsEvent.QS_DND_DETAILS;
        }

        @Override
        public View createDetailView(Context context, View convertView, ViewGroup parent) {
            final LinearLayout details = convertView != null ? (LinearLayout) convertView
                    : (LinearLayout) LayoutInflater.from(context).inflate(
                    R.layout.alert_slider_panel, parent, false);

            int state = getZenMode();

            if (convertView == null) {
                mButtons = (SegmentedButtons) details.findViewById(R.id.alert_slider_buttons);
                mMessageText = (TextView) details.findViewById(R.id.alert_slider_introduction_message);
                mButtons.addButton(R.string.quick_settings_alert_slider_alarms_only_label_twoline,
                        R.string.quick_settings_alert_slider_alarms_only_label,
                        Settings.Global.ZEN_MODE_ALARMS);
                mButtons.addButton(R.string.quick_settings_alert_slider_no_interruptions_label_twoline,
                        R.string.quick_settings_alert_slider_no_interruptions_label,
                        Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
                mButtons.setCallback(mButtonsCallback);
                // Init current state.
                mButtons.setSelectedValue(state, false /* fromClick */);
                mMessageText.setText(mContext.getString(state == Settings.Global.ZEN_MODE_NO_INTERRUPTIONS
                    ? R.string.quick_settings_alert_slider_detail_no_interruptions_description
                    : R.string.quick_settings_alert_slider_detail_alarms_only_description));
            }

            return details;
        }

        private final SegmentedButtons.Callback mButtonsCallback = new SegmentedButtons.Callback() {

            @Override
            public void onSelected(final Object value, boolean fromClick) {
                if (value != null && mButtons.isShown()) {
                    int state = (Integer) value;
                    if (fromClick) {
                        MetricsLogger.action(mContext, MetricsEvent.QS_DND, state);
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
