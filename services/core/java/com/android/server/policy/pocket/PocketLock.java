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
package com.android.server.policy.pocket;

import android.app.KeyguardManager;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

/**
 * This class provides a fullscreen overlays view, displaying itself
 * even on top of lock screen. While this view is displaying touch
 * inputs are not passed to the the views below.
 * @see android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
 * @author Carlo Savignano
 */
public class PocketLock {

    private final Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private Handler mHandler;
    private View mView;
    private boolean mShowing;
    private PowerManager mPowerManager;
    private KeyguardManager mKeyguardManager;
    
    private Runnable sleep = new Runnable() {
		@Override
		public void run() {
			boolean interactive = mPowerManager.isInteractive();
			boolean locked = mKeyguardManager.isKeyguardLocked();
			if(interactive && mShowing && locked) {
				mPowerManager.goToSleep(SystemClock.uptimeMillis(), PowerManager.GO_TO_SLEEP_REASON_TIMEOUT, PowerManager.GO_TO_SLEEP_FLAG_NO_DOZE);
			}	
		}
	};
	
	private Runnable show = new Runnable() {
		@Override
		public void run() {
			mView.setAlpha(0.9f);
			mView.setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE
					| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
					| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
			mWindowManager.addView(mView, mLayoutParams);
			mShowing = true;
		}
	};
	
	private Runnable hide = new Runnable() {
		@Override
		public void run() {
			mView.setSystemUiVisibility(
					View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
			mWindowManager.removeView(mView);
			mShowing = false;
		}
	};

    /**
     * Creates pocket lock objects, inflate view and set layout parameters.
     * @param context
     */
    public PocketLock(Context context) {
        mContext = context;
        mHandler = new Handler();
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = getLayoutParams();
        mView = LayoutInflater.from(mContext).inflate(com.android.internal.R.layout.pocket_lock_view_layout, null);
        mView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        mShowing = false;
    }

    public void show() {
		if(!mShowing){
			mHandler.post(show);
		}else {
			mHandler.removeCallbacks(hide);
			mHandler.removeCallbacks(show);
		}	
    }

    public void hide() {
        if(mShowing){
			mHandler.post(hide);
		}else {
			mHandler.removeCallbacks(show);
			mHandler.removeCallbacks(hide);
		}	
    }
    
    public void wokeUp(long timeout){
		mHandler.postDelayed(sleep, timeout);
	}	
	
	public void cancelPendingCallbacks(){
		mHandler.removeCallbacks(sleep);
	}
	
	public boolean isShowing() {
		return mShowing;
	}	

    private WindowManager.LayoutParams getLayoutParams() {
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.format = PixelFormat.TRANSLUCENT;
        mLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        mLayoutParams.gravity = Gravity.CENTER;
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        mLayoutParams.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED
                | WindowManager.LayoutParams.FLAG_FULLSCREEN
                | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        return mLayoutParams;
    }

}
