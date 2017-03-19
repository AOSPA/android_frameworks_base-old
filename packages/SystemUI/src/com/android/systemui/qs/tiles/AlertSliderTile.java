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
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;

import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.internal.logging.MetricsLogger;

import com.android.systemui.Prefs;
import com.android.systemui.R;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.ZenModeController;
import com.android.systemui.volume.SegmentedButtons;

/** Quick settings tile: Alert slider **/
public class AlertSliderTile extends QSTile<QSTile.State>  {

    private static final Intent ZEN_SETTINGS =
            new Intent(Settings.ACTION_ZEN_MODE_SETTINGS);

    private static final Intent ZEN_PRIORITY_SETTINGS =
            new Intent(Settings.ACTION_ZEN_MODE_PRIORITY_SETTINGS);

    private static final QSTile.Icon TOTAL_SILENCE =
            ResourceIcon.get(R.drawable.ic_qs_dnd_on_total_silence);

    private static final QSTile.Icon ALARMS_ONLY =
            ResourceIcon.get(R.drawable.ic_qs_dnd_on);

    private static final QSTile.Icon PRIORITY_ONLY =
            ResourceIcon.get(R.drawable.ic_qs_dnd_on_priority);

    private static final QSTile.Icon DISABLED =
            ResourceIcon.get(R.drawable.ic_qs_dnd_off);

    private final ZenModeController mController;
    private final AlertSliderDetailAdapter mDetailAdapter;

    private boolean mListening;
    private boolean mHasAlertSlider = false;
    private boolean mCollapseDetailOnZenChanged = true;

    public AlertSliderTile(Host host) {
        super(host);
        mController = host.getZenModeController();
        mDetailAdapter = new AlertSliderDetailAdapter();
        mHasAlertSlider = mContext.getResources().getBoolean(com.android.internal.R.bool.config_hasAlertSlider);
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
        mCollapseDetailOnZenChanged = true;
        final int zen = getZenMode();
        if (zen != Settings.Global.ZEN_MODE_OFF) {
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
        return mHasAlertSlider;
    }

    @Override
    protected void handleUpdateState(State state, Object arg) {
        final int zen = arg instanceof Integer ? (Integer) arg : getZenMode();
        switch (zen) {
            case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                state.icon = PRIORITY_ONLY;
                state.label = mContext.getString(R.string.quick_settings_dnd_priority_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_priority_on);
                break;
            case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                state.icon = TOTAL_SILENCE;
                state.label = mContext.getString(R.string.quick_settings_dnd_none_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_none_on);
                break;
            case Settings.Global.ZEN_MODE_ALARMS:
                state.icon = ALARMS_ONLY;
                state.label = mContext.getString(R.string.quick_settings_dnd_alarms_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_alarms_on);
                break;
            default:
                state.icon = DISABLED;
                state.label = mContext.getString(R.string.quick_settings_alert_slider_none_label);
                state.contentDescription = mContext.getString(
                        R.string.accessibility_quick_settings_dnd_off);
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
        return ""; // TODO
    }

    @Override
    public void setListening(boolean listening) {
        if (mListening == listening) return;
        mListening = listening;
        if (mListening) {
            mController.addCallback(mZenCallback);
        } else {
            mController.removeCallback(mZenCallback);
        }
    }

    private final ZenModeController.Callback mZenCallback = new ZenModeController.Callback() {
        public void onZenChanged(int zen) {
            if (mCollapseDetailOnZenChanged) {
                showDetail(false);
            }
            refreshState(zen);
            mCollapseDetailOnZenChanged = true;
        }
    };

    private final class AlertSliderDetailAdapter implements DetailAdapter {

        private SegmentedButtons mButtons;
        private TextView mMessageText, mZenPriorityIntroductionCustomize;
        private RelativeLayout mMessageBox;
        private RelativeLayout mRemindersSwitchParent, mEventsSwitchParent, mZenPriorityIntroduction;
        private Switch mReminders, mEvents;
        private ImageView mRemindersImageView, mEventsImageView, mZenPriorityIntroductionConfirm;

        @Override
        public CharSequence getTitle() {
            return mContext.getString(R.string.quick_settings_dnd_label); // R.string.quick_settings_alert_slider_title
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

            mCollapseDetailOnZenChanged = true;

            if (convertView == null) {
                mButtons = (SegmentedButtons) details.findViewById(R.id.alert_slider_buttons);
                mMessageText = (TextView) details.findViewById(R.id.alert_slider_introduction_message);
                mMessageBox = (RelativeLayout) details.findViewById(R.id.alert_slider_introduction);
                mRemindersSwitchParent = (RelativeLayout) details.findViewById(R.id.switch_container_reminders);
                mEventsSwitchParent = (RelativeLayout) details.findViewById(R.id.switch_container_events);
                mZenPriorityIntroduction = (RelativeLayout) details.findViewById(R.id.zen_introduction);
                mZenPriorityIntroductionConfirm = (ImageView) details.findViewById(R.id.zen_introduction_confirm);
                mZenPriorityIntroductionConfirm.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Prefs.putBoolean(mContext, Prefs.Key.DND_CONFIRMED_PRIORITY_INTRODUCTION, true);
                    }
                });
                mZenPriorityIntroductionCustomize = (TextView) details.findViewById(R.id.zen_introduction_customize);
                mZenPriorityIntroductionCustomize.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Prefs.putBoolean(mContext, Prefs.Key.DND_CONFIRMED_PRIORITY_INTRODUCTION, true);
                        mHost.startActivityDismissingKeyguard(ZEN_PRIORITY_SETTINGS);
                    }
                });
                mReminders = (Switch) details.findViewById(R.id.toggle_reminders);
                mEvents = (Switch) details.findViewById(R.id.toggle_events);
                mButtons.addButton(R.string.quick_settings_alert_slider_alarms_only_label_twoline,
                        R.string.quick_settings_alert_slider_alarms_only_label,
                        Settings.Global.ZEN_MODE_ALARMS);
                mButtons.addButton(R.string.quick_settings_alert_slider_no_interruptions_label_twoline,
                        R.string.quick_settings_alert_slider_no_interruptions_label,
                        Settings.Global.ZEN_MODE_NO_INTERRUPTIONS);
                mButtons.setCallback(mButtonsCallback);
            }

            mButtons.setSelectedValue(state, false /* fromClick */);
            refresh(state);

            return details;
        }

        private void refresh(int state) {
            switch(state) {
                case Settings.Global.ZEN_MODE_ALARMS:
                    // silent
                    mButtons.setVisibility(View.VISIBLE);
                    mMessageBox.setVisibility(View.VISIBLE);
                    mMessageText.setText(mContext.getString(R.string.quick_settings_alert_slider_detail_alarms_only_description));
                    // priority
                    mRemindersSwitchParent.setVisibility(View.GONE);
                    mEventsSwitchParent.setVisibility(View.GONE);
                    mZenPriorityIntroduction.setVisibility(View.GONE);
                    break;
                case Settings.Global.ZEN_MODE_NO_INTERRUPTIONS:
                    // silent
                    mButtons.setVisibility(View.VISIBLE);
                    mMessageBox.setVisibility(View.VISIBLE);
                    mMessageText.setText(mContext.getString(R.string.quick_settings_alert_slider_detail_no_interruptions_description));
                    // priority
                    mRemindersSwitchParent.setVisibility(View.GONE);
                    mEventsSwitchParent.setVisibility(View.GONE);
                    mZenPriorityIntroduction.setVisibility(View.GONE);
                    break;
                case Settings.Global.ZEN_MODE_IMPORTANT_INTERRUPTIONS:
                    // silent
                    mButtons.setVisibility(View.GONE);
                    mMessageBox.setVisibility(View.GONE);
                    // priority
                    mRemindersSwitchParent.setVisibility(View.VISIBLE);
                    mEventsSwitchParent.setVisibility(View.VISIBLE);
                    final boolean confirmed =  Prefs.getBoolean(mContext, Prefs.Key.DND_CONFIRMED_PRIORITY_INTRODUCTION, false);
                    mZenPriorityIntroduction.setVisibility(!confirmed ? View.VISIBLE : View.GONE);
                    break;
                default:
                    mButtons.setVisibility(View.GONE);
                    mMessageBox.setVisibility(View.VISIBLE);
                    mMessageText.setText(mContext.getString(R.string.quick_settings_alert_slider_detail_no_interruptions_description));
                    mRemindersSwitchParent.setVisibility(View.GONE);
                    mEventsSwitchParent.setVisibility(View.GONE);
                    mZenPriorityIntroduction.setVisibility(View.GONE);
                    break;
            }
        }

        private final SegmentedButtons.Callback mButtonsCallback = new SegmentedButtons.Callback() {

            @Override
            public void onSelected(final Object value, boolean fromClick) {
                if (value != null && mButtons.isShown()) {
                    int state = (Integer) value;
                    if (fromClick) {
                        MetricsLogger.action(mContext, MetricsEvent.QS_DND, state);
                        mCollapseDetailOnZenChanged = false;
                        setSilentMode(state);
                        setZenMode(state);
                        refresh(state);
                    }
                }
            }

            @Override
            public void onInteraction() { }
        };
 
        public void setSilentMode(int silentState) {
            int silentMode = silentState == Settings.Global.ZEN_MODE_ALARMS ? 0 : 1;
            Settings.System.putIntForUser(mContext.getContentResolver(),
                    Settings.System.ALERT_SLIDER_SILENT_MODE, silentMode, UserHandle.USER_CURRENT);
        }
    }
}
