/*
 * Copyright (C) 2020 Paranoid Android
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

package com.android.systemui.screenrecord;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.CountDownTimer;
import android.util.Log;

import com.android.systemui.statusbar.policy.CallbackController;
import com.android.systemui.screenrecord.RecordingController.RecordingStateListener;

import java.util.ArrayList;
import java.util.Iterator;

public class RecordingControllerImpl implements RecordingController {

    private static final String TAG = "RecordingController";

    private final Context mContext;
    private CountDownTimer mCountDownTimer = null;

    public boolean mIsRecording;
    public boolean mIsStarting;

    public ArrayList<RecordingStateListener> mListeners = new ArrayList<>();
    private PendingIntent mStopIntent;

    public RecordingControllerImpl(Context context) {
        mContext = context;
    }

    public void launchRecordPrompt() {
        ComponentName cn = new ComponentName("com.android.systemui", "com.android.systemui.screenrecord.ScreenRecordDialog");
        Intent i = new Intent();
        i.setComponent(cn);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mContext.startActivity(i);
    }

    public void startCountdown(long millisInFuture, long countDownInterval, PendingIntent startIntent, PendingIntent stopIntent) {
        mIsStarting = true;
        mStopIntent = stopIntent;
        CountDownTimer timer = new CountDownTimer(millisInFuture, countDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                Iterator it = mListeners.iterator();
                while (it.hasNext()) {
                    ((RecordingStateListener) it.next()).onCountdown(millisUntilFinished);
                }
            }

            @Override
            public void onFinish() {
                mIsStarting = false;
                mIsRecording = true;
                Iterator it = mListeners.iterator();
                while (it.hasNext()) {
                    ((RecordingStateListener) it.next()).onCountdownEnd();
                }
                try {
                    startIntent.send();
                    Log.d(TAG, "sent start intent");
                } catch (PendingIntent.CanceledException e) {
                    Log.e(TAG, "Pending intent was cancelled: " + e.getMessage());
                }
            }
        };
        mCountDownTimer = timer;
        timer.start();
    }

    public void cancelCountdown() {
        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        } else {
            Log.e(TAG, "Timer was null");
        }
        mIsStarting = false;
        Iterator<RecordingStateListener> it = mListeners.iterator();
        while (it.hasNext()) {
            it.next().onCountdownEnd();
        }
    }

    public boolean isStarting() {
        return mIsStarting;
    }

    public boolean isRecording() {
        return mIsRecording;
    }

    public void stopRecording() {
        try {
            mStopIntent.send();
            updateState(false);
        } catch (PendingIntent.CanceledException e) {
            Log.e(TAG, "Error stopping: " + e.getMessage());
        }
    }

    public void updateState(boolean isRecording) {
        mIsRecording = isRecording;
        Iterator<RecordingStateListener> it = mListeners.iterator();
        while (it.hasNext()) {
            RecordingStateListener cb = it.next();
            if (isRecording) {
                cb.onRecordingStart();
            } else {
                cb.onRecordingEnd();
            }
        }
    }

    public void addCallback(RecordingStateListener cb) {
        mListeners.add(cb);
    }

    public void removeCallback(RecordingStateListener cb) {
        mListeners.remove(cb);
    }
}
