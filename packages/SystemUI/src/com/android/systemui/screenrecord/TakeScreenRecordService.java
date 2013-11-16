package com.android.systemui.screenrecord;

/* Copyright (C) 2011 The Android Open Source Project
 * This code has been modified. Portions copyright (C) 2013, ParanoidAndroid Project.
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

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

// Modelled after com.android.systemui.sreenshot.TakeScreenshotService
public class TakeScreenRecordService extends Service {
    private static final String TAG = "TakeScreenRecordService";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";

    private static GlobalScreenRecord mScreenRecord;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    final Messenger callback = msg.replyTo;
                    if (mScreenRecord == null || !mScreenRecord.isRecording()) {
                        mScreenRecord = new GlobalScreenRecord(TakeScreenRecordService.this);
                        mScreenRecord.takeScreenRecording();
                    } else {
                        mScreenRecord.stopScreenRecording();
                    }

                    Message reply = Message.obtain(null, 1);
                    try {
                        callback.send(reply);
                    } catch (RemoteException e) {
                    }
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return new Messenger(mHandler).getBinder();
    }

    // Modified OmniRom implementation
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction().equals(ACTION_START)) {
            if (mScreenRecord == null) {
                mScreenRecord = new GlobalScreenRecord(TakeScreenRecordService.this);
            }
            mScreenRecord.takeScreenRecording();
        } else if (intent.getAction().equals(ACTION_STOP)) {
            if (mScreenRecord != null) mScreenRecord.stopScreenRecording();
        }

        return super.onStartCommand(intent, flags, startId);
    }
}
