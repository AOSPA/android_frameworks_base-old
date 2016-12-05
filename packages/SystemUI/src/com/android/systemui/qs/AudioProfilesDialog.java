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

package com.android.systemui.qs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.android.systemui.R;
import com.android.systemui.statusbar.policy.AudioProfileController;
import com.android.systemui.statusbar.policy.AudioProfileControllerImpl;
import android.view.KeyEvent;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.text.TextUtils;

public class AudioProfilesDialog extends AlertDialog implements
        android.view.View.OnClickListener {

    private Context mContext;
    private int mRingerMode;
    private ImageView mImgGeneral, mImgSilent, mImgMetting, mImgOutdoor;

    public AudioProfilesDialog(Context context, int ringerMode) {
        super(context);
        mContext = context;
        mRingerMode = ringerMode;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    public void init() {
        View view = LayoutInflater.from(mContext).inflate(
                R.layout.quick_settings_audioprofiles_dialog, null);
        setContentView(view);

        mImgGeneral = (ImageView) view.findViewById(R.id.img_general);
        mImgSilent = (ImageView) view.findViewById(R.id.img_silent);
        mImgMetting = (ImageView) view.findViewById(R.id.img_metting);
        mImgOutdoor = (ImageView) view.findViewById(R.id.img_outdoor);
        mImgGeneral.setOnClickListener(this);
        mImgSilent.setOnClickListener(this);
        mImgMetting.setOnClickListener(this);
        mImgOutdoor.setOnClickListener(this);

        updateCurrentModeIcon();

        Window dialogWindow = getWindow();
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
        dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        dialogWindow.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        WindowManager.LayoutParams lp = dialogWindow.getAttributes();
        DisplayMetrics d = mContext.getResources().getDisplayMetrics();
        lp.width = (int) (d.widthPixels * 0.9);
        lp.privateFlags |=
        WindowManager.LayoutParams.PRIVATE_FLAG_SHOW_FOR_ALL_USERS;
        dialogWindow.setAttributes(lp);
    }

    private void updateCurrentModeIcon() {
        setDefaultIcon();
        switch (mRingerMode) {
        case AudioProfileController.RINGER_MODE_GENERAL:
            mImgGeneral
                    .setImageResource(R.drawable.ic_audio_profile_general_focused);
            break;
        case AudioProfileController.RINGER_MODE_SILENT:
            mImgSilent
                    .setImageResource(R.drawable.ic_audio_profile_silent_focused);
            break;
        case AudioProfileController.RINGER_MODE_MEETING:
            mImgMetting
                    .setImageResource(R.drawable.ic_audio_profile_meeting_focused);
            break;
        case AudioProfileController.RINGER_MODE_OUTDOOR:
            mImgOutdoor
                    .setImageResource(R.drawable.ic_audio_profile_outdoor_focused);
            break;
        default:
            break;
        }
    }

    private void setDefaultIcon() {
        mImgGeneral
                .setImageResource(R.drawable.ic_audio_profile_general_normal);
        mImgSilent.setImageResource(R.drawable.ic_audio_profile_silent_normal);
        mImgMetting
                .setImageResource(R.drawable.ic_audio_profile_meeting_normal);
        mImgOutdoor
                .setImageResource(R.drawable.ic_audio_profile_outdoor_normal);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.img_general:
            mRingerMode = AudioProfileController.RINGER_MODE_GENERAL;
            updateCurrentModeIcon();
            setAudioProFile();
            break;
        case R.id.img_silent:
            mRingerMode = AudioProfileController.RINGER_MODE_SILENT;
            updateCurrentModeIcon();
            setAudioProFile();
            break;
        case R.id.img_metting:
            mRingerMode = AudioProfileController.RINGER_MODE_MEETING;
            updateCurrentModeIcon();
            setAudioProFile();
            break;
        case R.id.img_outdoor:
            mRingerMode = AudioProfileController.RINGER_MODE_OUTDOOR;
            updateCurrentModeIcon();
            setAudioProFile();
            break;
        default:
            break;
        }
        this.dismiss();
    }

    private void setAudioProFile() {
        Intent intent = new Intent(
                AudioProfileController.AUDIO_PROLILE_QS_ACTION);
        intent.putExtra(AudioProfileController.CURRENT_PROFILE_ID, mRingerMode);
        mContext.sendBroadcast(intent);
    }
}
