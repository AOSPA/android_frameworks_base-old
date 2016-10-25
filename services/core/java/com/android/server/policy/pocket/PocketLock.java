package com.android.server.policy.pocket;

import android.animation.Animator;
import android.content.Context;
import android.graphics.PixelFormat;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

public class PocketLock {

    private final Context mContext;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;
    private Handler mHandler;
    private View mView;
    private View mHintContainer;
    private boolean mAttached;
    private boolean mAnimating;

    public PocketLock(Context context) {
        mContext = context;
        mHandler = new Handler();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mLayoutParams = getLayoutParams();
        mView = LayoutInflater.from(mContext).inflate(
                com.android.internal.R.layout.pocket_lock_view_layout, null);

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

    public boolean isShowing() {
        return mAttached && !mAnimating;
    }

    public void show() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                if (!mAttached) {
                    mAnimating = false;
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
                            mAnimating = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animator) {
                            if (mAnimating) {
                                mAnimating = false;
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
        this.mLayoutParams.format = PixelFormat.RGB_888; // PixelFormat.OPAQUE;
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
