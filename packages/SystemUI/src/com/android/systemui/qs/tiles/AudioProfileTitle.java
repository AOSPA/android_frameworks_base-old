/*
Copyright (c) 2016, The Linux Foundation. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:
    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.
    * Neither the name of The Linux Foundation nor the names of its
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESS OR IMPLIED
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT
ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package com.android.systemui.qs.tiles;

import com.android.systemui.qs.AudioProfilesDialog;
import com.android.systemui.qs.QSTile;
import com.android.systemui.statusbar.policy.AudioProfileController;
import android.content.Intent;
import com.android.internal.logging.MetricsLogger;
import com.android.internal.logging.MetricsProto.MetricsEvent;
import com.android.systemui.R;

public class AudioProfileTitle extends QSTile<QSTile.BooleanState> {

    private final static String TAG = "AudioTitle";
    private AudioProfileController mAudioController;
    private static final String STARTPROFILE = "com.codeaurora.STARTPROFILE";

    public AudioProfileTitle(QSTile.Host host) {
        super(host);
        mAudioController = host.getAudioController();
    }

    @Override
    public void setListening(boolean listening) {
        if (listening) {
            mAudioController.addCallback(mCallback);
        } else {
            mAudioController.removeCallback(mCallback);
        }
    }

    AudioProfileController.Callback mCallback = new AudioProfileController.Callback() {

        @Override
        public void onAudioRingerModeChanged(int ringerMode) {
            refreshState(ringerMode);
        }

    };

    @Override
    public BooleanState newTileState() {
        return new BooleanState();
    }

    @Override
    protected void handleClick() {
        AudioProfilesDialog dialog = new AudioProfilesDialog(mContext,
                mAudioController.getRingerMode());
        dialog.show();
    }

    @Override
    public void longClick() {
        Intent intent = new Intent(STARTPROFILE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(intent);
        mHost.collapsePanels();
    }

    @Override
    public Intent getLongClickIntent() {
        Intent intent = new Intent(STARTPROFILE);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mHost.collapsePanels();
        return intent;
    }

    @Override
    public CharSequence getTileLabel() {
        return mContext.getString(R.string.quick_settings_audio_title);
    }

    @Override
    protected void handleUpdateState(BooleanState state, Object arg) {
        Integer ringerMode = null;
        if (arg instanceof Integer) {
            ringerMode = (Integer) arg;
        }
        if (ringerMode == null) {
            ringerMode = mAudioController.getRingerMode();
        }
        state.label = mContext.getString(R.string.quick_settings_audio_title);
        state.icon = getIcon(ringerMode);
    }

    private Icon getIcon(int ringerMode) {
        switch (ringerMode) {
        case AudioProfileController.RINGER_MODE_GENERAL:
            return ResourceIcon.get(R.drawable.ic_qs_general);
        case AudioProfileController.RINGER_MODE_SILENT:
            return ResourceIcon.get(R.drawable.ic_qs_silent);
        case AudioProfileController.RINGER_MODE_MEETING:
            return ResourceIcon.get(R.drawable.ic_qs_meeting);
        case AudioProfileController.RINGER_MODE_OUTDOOR:
            return ResourceIcon.get(R.drawable.ic_qs_outdoor);
        default:
            return ResourceIcon.get(R.drawable.ic_qs_disable);
        }
    }

    @Override
    public int getMetricsCategory() {
        return MetricsEvent.QS_AUDIOPROFILE;
    }
}
