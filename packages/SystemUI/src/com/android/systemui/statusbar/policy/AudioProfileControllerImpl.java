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

package com.android.systemui.statusbar.policy;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;
import android.provider.Settings.Global;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import java.util.ArrayList;

public class AudioProfileControllerImpl implements AudioProfileController {
    private static final String TAG = "AudioControllerImpl";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    private final ArrayList<Callback> mCallbacks = new ArrayList<Callback>();
    private Context mContext;
    private int mRingerMode;
    private AudioProfileObserver mAudioProfileObserver = new AudioProfileObserver();

    public AudioProfileControllerImpl(Context context) {
        mContext = context;
    }

    @Override
    public void addCallback(Callback callback) {
        if (callback == null || mCallbacks.contains(callback))
            return;
        if (DEBUG)
            Log.d(TAG, "addCallback " + callback);
        mCallbacks.add(callback);
        mAudioProfileObserver.setListening(!mCallbacks.isEmpty());
    }

    @Override
    public void removeCallback(Callback callback) {
        if (callback == null)
            return;
        if (DEBUG)
            Log.d(TAG, "removeCallback " + callback);
        mCallbacks.remove(callback);
        mAudioProfileObserver.setListening(!mCallbacks.isEmpty());
    }

    @Override
    public int getRingerMode() {
        try {
            mRingerMode = Settings.Global.getInt(mContext.getContentResolver(),
                    AudioProfileController.AUDIO_PROLILE_MODE);
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        return mRingerMode;
    }

    private void fireCallback(int ringerMode) {
        mRingerMode = ringerMode;
        for (Callback callback : mCallbacks) {
            callback.onAudioRingerModeChanged(mRingerMode);
        }
    }

    private final class AudioProfileObserver extends ContentObserver {
        private boolean mRegistered;

        public AudioProfileObserver() {
            super(new Handler());
        }

        public void setListening(boolean listening) {
            if (listening && !mRegistered) {
                if (DEBUG)
                    Log.d(TAG, "Registering Observer");
                mContext.getContentResolver().registerContentObserver(
                    Settings.Global.getUriFor(AudioProfileController.AUDIO_PROLILE_MODE),
                    false, mAudioProfileObserver);
                mRegistered = true;
            } else if (!listening && mRegistered) {
                if (DEBUG)
                    Log.d(TAG, "Unregistering Observer");
                mContext.getContentResolver().unregisterContentObserver(mAudioProfileObserver);
                mRegistered = false;
            }
        }

        @Override
        public void onChange(boolean selfChange) {
            if (DEBUG)
                Log.d(TAG, "onChange ");
            fireCallback(getRingerMode());
        }
    }
}