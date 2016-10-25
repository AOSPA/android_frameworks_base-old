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

import android.animation.Animator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
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
  
    private static final boolean TEST_WITHOUT_POWER_KEY = false;

    private final Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private Handler mHandler;
    private View mView;
    private View mHintContainer;
    private boolean mAttached;
    private boolean mHiding;

    /**
     * Creates pocket lock objects, inflate view and set layout parameters.
     * @param context
     */
    public PocketLock(Context context) {
        mContext = context;
        mHandler = new Handler();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = getLayoutParams();
        mView = LayoutInflater.from(mContext).inflate(
                com.android.internal.R.layout.pocket_lock_view_layout, null);

        if (TEST_WITHOUT_POWER_KEY) {
            // Just for testing.
            mHintContainer = mView.findViewById(com.android.internal.R.id.pocket_hint_container);
            mHintContainer.setHapticFeedbackEnabled(true);
            mHintContainer.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    hideAnimated();
                    return true;
                }
            });
        }
    }

    /**
     * Returns whether this.mView is showing and not animating out.
     * @return
     */
    public boolean isShowing() {
        return mAttached && !mHiding;
    }

    public void show() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mAttached) {
                    mHiding = false;
                    mView.setAlpha(1.0f); // ensure alpha
                    if (mWindowManager != null) {
                        mWindowManager.addView(mView, mLayoutParams);
                        mAttached = true;
                    }
                }
            }
        });
    }

    public void hide() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isShowing()) {
                    if (mWindowManager != null) {
                        mWindowManager.removeView(mView);
                        mView.setAlpha(1.0f); // restore alpha
                        mAttached = false;
                    }
                }
            }
        });
    }

    public void hideAnimated() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (isShowing()) {
                    mView.animate().alpha(0.0f).setListener(new Animator.AnimatorListener() {
                        @Override
                        public void onAnimationStart(Animator animator) {
                            mHiding = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            if (mHiding) {
                                mHiding = false;
                                hide();
                            }
                        }

                        @Override
                        public void onAnimationCancel(Animator animator) {
                        }

                        @Override
                        public void onAnimationRepeat(Animator animator) {
                        }
                    }).start();
                }
            }
        });
    }

    private WindowManager.LayoutParams getLayoutParams() {
        this.mLayoutParams = new WindowManager.LayoutParams();
        this.mLayoutParams.format = PixelFormat.TRANSLUCENT;
        this.mLayoutParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        this.mLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        this.mLayoutParams.gravity = Gravity.CENTER;
        this.mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        this.mLayoutParams.flags = WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        return this.mLayoutParams;
    }

}
