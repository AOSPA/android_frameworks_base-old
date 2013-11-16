/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.systemui.screenrecord;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.android.systemui.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

class GlobalScreenRecord {
    private static final String TAG = "GlobalScreenRecord";

    private static final int SCREENRECORD_NOTIFICATION_ID = 790;
    private static final String SCREENRECORD_DIR_NAME = "ScreenRecords";
    private static final String SCREENRECORD_FILE_NAME_TEMPLATE = "SCR_%s.scr";

    private static final int SUCCESS = 0;
    private static final int ERROR = 1;

    private File mTmpFile;
    private File mDirectory;
    private Context mContext;
    private Handler mHandler;
    private NotificationManager mNotificationManager;
    private ScreenRecorder mScreenRecorder;

    private boolean mIsRecording;

    /**
     * @param context everything needs a context :(
     */
    public GlobalScreenRecord(Context context) {
        mContext = context;

        mDirectory = new File
                (Environment.getExternalStorageDirectory().getAbsolutePath(),
                        SCREENRECORD_DIR_NAME);

        mNotificationManager =
            (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        mHandler = new Handler() {
            public void handleMessage(Message msg) {
                if (msg.what == SUCCESS) {
                    stopScreenRecording();
                } else if (msg.what == ERROR) {
                    mScreenRecorder = null;
                    notifyScreenRecordError();
                }
            }
        };
    }

    /**
     * Starts a screen recording of the current display.
     */
    void takeScreenRecording() {
        if (mScreenRecorder != null) {
            return;
        }

        // Create directory if it doesn't already exist
        mDirectory.mkdirs();

        String recordFileName = String.format(SCREENRECORD_FILE_NAME_TEMPLATE, "tmp");

        mTmpFile = new File(mDirectory,recordFileName);

        mScreenRecorder = new ScreenRecorder();
        mScreenRecorder.start();

        mIsRecording = true;
        
        mNotificationManager
                        .notify(SCREENRECORD_NOTIFICATION_ID, createRunningNotification(mContext));
                        
        mScreenRecorder.setPriority(Thread.MIN_PRIORITY);
    }

    void stopScreenRecording() {
        if (mScreenRecorder == null) return;

        try {
            mScreenRecorder.interrupt();
        } catch (Exception e) {
            // should not occur 
        }

        while (mScreenRecorder.isAlive()) {
            // pause
        }

        mIsRecording = false;        
        
        long recordTime = System.currentTimeMillis();
        String recordDate = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date(recordTime));
        
        File newFileName = new File (mTmpFile.toString()
                        .substring(0,mTmpFile.toString().length()-7) + recordDate + ".mp4");

        if (!mTmpFile.renameTo(newFileName)) {
            notifyScreenRecordError();
            return;
        }

        mNotificationManager.cancel(SCREENRECORD_NOTIFICATION_ID);

        mNotificationManager.notify(SCREENRECORD_NOTIFICATION_ID,
                createFinishedNotification(mContext, newFileName));

        MediaScannerConnection.scanFile(mContext,
                new String[] { newFileName.getAbsolutePath() }, null,
                new MediaScannerConnection.OnScanCompletedListener() {
                public void onScanCompleted(String path, Uri uri) {
                    // Show in gallery
                }
            });
    }

    public boolean isRecording(){
        return mIsRecording;
    }

    /**
     * Create thread for screen recording
     */
    private class ScreenRecorder extends Thread {
        public void run() {  
            List<String> params = java.util.Arrays.asList("/system/bin/screenrecord", mTmpFile.toString());
            ProcessBuilder scr = new ProcessBuilder(params);
            scr.redirectErrorStream(true);           

            try {
                Process recordProcess = scr.start();

                InputStream in = recordProcess.getInputStream();

                while (!isInterrupted()) {
                    try {
                        int value = recordProcess.exitValue();
                        
                        Message msg = Message.obtain(mHandler, SUCCESS, value, 0, null);
                        mHandler.sendMessage(msg);
                        return;
                    } catch (IllegalThreadStateException e) {
                        // Process is still running
                    }
                }
                Log.i("screenrecord", "lets destroy this process!");
                // Could find no other way to stop recording  so ... used Omni hack
                params = java.util.Arrays.asList("killall", "-2", "screenrecord");
                scr.command(params);
                scr.start();
                Log.i("screenrecord", "does recording get saved after destroying?");
            } catch (IOException e) {
                Message msg = Message.obtain(mHandler, ERROR);
                mHandler.sendMessage(msg);
            }            
        }
    };

    private Notification createRunningNotification(Context context) {
        Intent intent = new Intent(mContext, TakeScreenRecordService.class)
                .setAction(TakeScreenRecordService.ACTION_STOP);
        PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Resources r = mContext.getResources();

        Notification.Builder mBuilder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.presence_video_busy)
                .setContentTitle(r.getString(R.string.screenrecord_start_title))
                .setContentText(r.getString(R.string.screenrecord_start_text))
                .setContentIntent(pendingIntent)
                .setTicker(r.getString(R.string.screenrecord_start_ticker))
                .setPriority(Integer.MIN_VALUE)
                .setOngoing(true);
        return mBuilder.build();
    }

    private Notification createFinishedNotification(Context context, File recordFile) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.addFlags(intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setDataAndType(Uri.fromFile(recordFile), "video/mp4");

        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Resources r = mContext.getResources();

        Notification.Builder mBuilder = new Notification.Builder(context)
                .setSmallIcon(android.R.drawable.presence_video_online)
                .setContentTitle(r.getString(R.string.screenrecord_complete_title))
                .setContentText(r.getString(R.string.screenrecord_complete_text))
                .setTicker(r.getString(R.string.screenrecord_complete_ticker))
                .setContentIntent(pendingIntent)
                .setOngoing(false)
                .setAutoCancel(true);
        return mBuilder.build();
    }

   
    void notifyScreenRecordError() {
        mIsRecording = false;
        
        mTmpFile.delete();

        Resources r = mContext.getResources();

        // Clear all existing notification, compose the new notification and show it
        Notification.Builder b = new Notification.Builder(mContext)
            .setTicker(r.getString(R.string.screenrecord_failed_title))
            .setContentTitle(r.getString(R.string.screenrecord_failed_title))
            .setContentText(r.getString(R.string.screenrecord_failed_text))
            .setSmallIcon(R.drawable.stat_notify_image_error)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true);
        Notification n =
            new Notification.BigTextStyle(b)
                .bigText(r.getString(R.string.screenrecord_failed_text))
                .build();
        mNotificationManager.notify(SCREENRECORD_NOTIFICATION_ID, n);
    }
}
