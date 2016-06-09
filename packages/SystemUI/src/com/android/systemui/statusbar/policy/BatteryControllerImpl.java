/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import com.android.systemui.DemoMode;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Default implementation of a {@link BatteryController}. This controller monitors for battery
 * level change events that are broadcasted by the system.
 */
public class BatteryControllerImpl extends BroadcastReceiver implements BatteryController {
    private static final String TAG = "BatteryController";

    public static final String ACTION_LEVEL_TEST = "com.android.systemui.BATTERY_LEVEL_TEST";

    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    public static final int STYLE_ICON_PORTRAIT = 0;
    public static final int STYLE_CIRCLE = 2;
    public static final int STYLE_GONE = 4;
    public static final int STYLE_TEXT = 5;

    private final ArrayList<BatteryController.BatteryStateChangeCallback> mChangeCallbacks = new ArrayList<>();
    private final PowerManager mPowerManager;
    private final Handler mHandler;
    private final Context mContext;

    protected int mLevel;
    protected boolean mPluggedIn;
    protected boolean mCharging;
    protected boolean mCharged;
    protected boolean mPowerSave;
    private boolean mTestmode = false;

    private int mStyle;
    private int mPercentMode;
    private int mUserId;
    private SettingsObserver mObserver;

    public BatteryControllerImpl(Context context, Handler handler) {
        mContext = context;
        mHandler = new Handler();
        mPowerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);

        registerReceiver();
        updatePowerSave();

        mObserver = new SettingsObserver(context, handler);
        mObserver.observe();
    }

    public void setUserId(int userId) {
        mUserId = userId;
        mObserver.observe();
    }

    private void registerReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED);
        filter.addAction(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING);
        filter.addAction(ACTION_LEVEL_TEST);
        mContext.registerReceiver(this, filter);
    }

    @Override
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("BatteryController state:");
        pw.print("  mLevel="); pw.println(mLevel);
        pw.print("  mPluggedIn="); pw.println(mPluggedIn);
        pw.print("  mCharging="); pw.println(mCharging);
        pw.print("  mCharged="); pw.println(mCharged);
        pw.print("  mPowerSave="); pw.println(mPowerSave);
    }

    @Override
    public void setPowerSaveMode(boolean powerSave) {
        mPowerManager.setPowerSaveMode(powerSave);
    }

    @Override
    public void addStateChangedCallback(BatteryController.BatteryStateChangeCallback cb) {
        mChangeCallbacks.add(cb);
        cb.onBatteryLevelChanged(mLevel, mPluggedIn, mCharging);
        cb.onPowerSaveChanged(mPowerSave);
        cb.onBatteryStyleChanged(mStyle, mPercentMode);
    }

    @Override
    public void removeStateChangedCallback(BatteryController.BatteryStateChangeCallback cb) {
        mChangeCallbacks.remove(cb);
    }

    @Override
    public void onReceive(final Context context, Intent intent) {
        final String action = intent.getAction();
        if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
            if (mTestmode && !intent.getBooleanExtra("testmode", false)) return;
            mLevel = (int)(100f
                    * intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
                    / intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100));
            mPluggedIn = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;

            final int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN);
            mCharged = status == BatteryManager.BATTERY_STATUS_FULL;
            mCharging = mCharged || status == BatteryManager.BATTERY_STATUS_CHARGING;

            fireBatteryLevelChanged();
        } else if (action.equals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)) {
            updatePowerSave();
        } else if (action.equals(PowerManager.ACTION_POWER_SAVE_MODE_CHANGING)) {
            setPowerSave(intent.getBooleanExtra(PowerManager.EXTRA_POWER_SAVE_MODE, false));
        } else if (action.equals(ACTION_LEVEL_TEST)) {
            mTestmode = true;
            mHandler.post(new Runnable() {
                int curLevel = 0;
                int incr = 1;
                int saveLevel = mLevel;
                boolean savePlugged = mPluggedIn;
                Intent dummy = new Intent(Intent.ACTION_BATTERY_CHANGED);
                @Override
                public void run() {
                    if (curLevel < 0) {
                        mTestmode = false;
                        dummy.putExtra("level", saveLevel);
                        dummy.putExtra("plugged", savePlugged);
                        dummy.putExtra("testmode", false);
                    } else {
                        dummy.putExtra("level", curLevel);
                        dummy.putExtra("plugged", incr > 0 ? BatteryManager.BATTERY_PLUGGED_AC
                                : 0);
                        dummy.putExtra("testmode", true);
                    }
                    context.sendBroadcast(dummy);

                    if (!mTestmode) return;

                    curLevel += incr;
                    if (curLevel == 100) {
                        incr *= -1;
                    }
                    mHandler.postDelayed(this, 200);
                }
            });
        }
    }

    @Override
    public boolean isPowerSave() {
        return mPowerSave;
    }

    private void updatePowerSave() {
        setPowerSave(mPowerManager.isPowerSaveMode());
    }

    private void setPowerSave(boolean powerSave) {
        if (powerSave == mPowerSave) return;
        mPowerSave = powerSave;
        if (DEBUG) Log.d(TAG, "Power save is " + (mPowerSave ? "on" : "off"));
        firePowerSaveChanged();
    }

    protected void fireBatteryLevelChanged() {
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onBatteryLevelChanged(mLevel, mPluggedIn, mCharging);
        }
    }

    private void firePowerSaveChanged() {
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onPowerSaveChanged(mPowerSave);
        }
    }

    private void fireSettingsChanged() {
        final int N = mChangeCallbacks.size();
        for (int i = 0; i < N; i++) {
            mChangeCallbacks.get(i).onBatteryStyleChanged(mStyle, mPercentMode);
        }
    }

    private boolean mDemoMode;

    @Override
    public void dispatchDemoCommand(String command, Bundle args) {
        if (!mDemoMode && command.equals(COMMAND_ENTER)) {
            mDemoMode = true;
            mContext.unregisterReceiver(this);
        } else if (mDemoMode && command.equals(COMMAND_EXIT)) {
            mDemoMode = false;
            registerReceiver();
            updatePowerSave();
        } else if (mDemoMode && command.equals(COMMAND_BATTERY)) {
           String level = args.getString("level");
           String plugged = args.getString("plugged");
           if (level != null) {
               mLevel = Math.min(Math.max(Integer.parseInt(level), 0), 100);
           }
           if (plugged != null) {
               mPluggedIn = Boolean.parseBoolean(plugged);
           }
            fireBatteryLevelChanged();
        }
    }

    private final class SettingsObserver extends ContentObserver {
        private ContentResolver mResolver;
        private boolean mRegistered;

        private final Uri STYLE_URI =
                Settings.System.getUriFor(Settings.System.STATUS_BAR_BATTERY_STYLE);
        private final Uri PERCENT_URI =
                Settings.System.getUriFor(Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT);

        public SettingsObserver(Context context, Handler handler) {
            super(handler);
            mResolver = context.getContentResolver();
        }

        public void observe() {
            if (mRegistered) {
                mResolver.unregisterContentObserver(this);
            }
            mResolver.registerContentObserver(STYLE_URI, false, this, mUserId);
            mResolver.registerContentObserver(PERCENT_URI, false, this, mUserId);
            mRegistered = true;

            update();
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            update();
        }

        private void update() {
            mStyle = Settings.System.getIntForUser(mResolver,
                    Settings.System.STATUS_BAR_BATTERY_STYLE, 0, mUserId);
            mPercentMode = Settings.System.getIntForUser(mResolver,
                    Settings.System.STATUS_BAR_SHOW_BATTERY_PERCENT, 0, mUserId);

            fireSettingsChanged();
        }
    };
}
