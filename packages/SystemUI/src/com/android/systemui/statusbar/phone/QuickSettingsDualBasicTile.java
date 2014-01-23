/*
 *  Copyright (C) 2013 The OmniROM Project
 *  Copyright (C) 2014 ParanoidAndroid Project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.android.systemui.statusbar.phone;

import com.android.systemui.R;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.animation.OvershootInterpolator;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.GestureDetector;
import android.widget.FrameLayout;
import android.widget.ImageView;

import java.lang.Runnable;

public class QuickSettingsDualBasicTile extends QuickSettingsTileView {

    private static final int TRANSLATION_X = 300;
    private static final int SWIPE_MIN_DISTANCE = 120;
    private static final int SWIPE_MAX_OFF_DISTANCE = 200;
    private static final int LONG_PRESS_MAX_OFF_DISTANCE = 50;

    public QuickSettingsBasicTile mFront;
    public QuickSettingsBasicTile mBack;

    private Runnable mFrontLongPressRunnable;
    private Runnable mBackLongPressRunnable;
    private Handler mHandler = new Handler();

    private Context mContext;
    private boolean mAnimationLock;

    private float mInitialX;
    private float mInitialY;

    public QuickSettingsDualBasicTile(Context context) {
        this(context, null);
    }

    public QuickSettingsDualBasicTile(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
    }

    public void setDefaultContent() {
        setLayoutParams(new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                        mContext.getResources().getDimensionPixelSize(R.dimen.quick_settings_cell_height)
        ));

        mFront = new QuickSettingsBasicTile(mContext);
        mFront.setupDualTile(this);
        mBack = new QuickSettingsBasicTile(mContext);
        mBack.setupDualTile(this);

        addView(mBack,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));

        addView(mFront,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT));
    }

    public void setFrontImageDrawable(Drawable drawable) {
        mFront.setImageDrawable(drawable);
    }

    public void setBackImageDrawable(Drawable drawable) {
        mBack.setImageDrawable(drawable);
    }

    public void setFrontImageResource(int id) {
        mFront.setImageResource(id);
    }

    public void setBackImageResource(int id) {
        mBack.setImageResource(id);
    }

    public void setFrontText(CharSequence text) {
        mFront.setText(text);
    }

    public void setBackText(CharSequence text) {
        mBack.setText(text);
    }

    public void setFrontTextResource(int id) {
        mFront.setTextResource(id);
    }

    public void setBackTextResource(int id) {
        mBack.setTextResource(id);
    }

    public void setFrontContentDescription(CharSequence text) {
        mFront.setContentDescription(text);
    }

    public void setBackContentDescription(CharSequence text) {
        mBack.setContentDescription(text);
    }

    public QuickSettingsTileView getFront() {
        return mFront;
    }

    public QuickSettingsTileView getBack() {
        return mBack;
    }

    public void setFrontOnClickListener(final View.OnClickListener listener) {
        mFront.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(mFront);
                bounce(mFront);
            }
        });
    }

    public void setBackOnClickListener(final View.OnClickListener listener) {
        mBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onClick(mBack);
                bounce(mBack);
            }
        });
    }

    public void setFrontOnLongClickListener(final View.OnLongClickListener listener) {
        mFrontLongPressRunnable = new Runnable() {
            @Override
            public void run() {
                listener.onLongClick(mFront);
            }
        };
    }

    public void setBackOnLongClickListener(final View.OnLongClickListener listener) {
        mBackLongPressRunnable = new Runnable() {
            @Override
            public void run() {
                listener.onLongClick(mBack);
            }
        };
    }

    public void enableDualTileGestures() {
        if (mFront != null && mBack != null) {
            final GestureDetector gestureDetector = new GestureDetector(mContext, new TileGestureDetector());
            final View.OnTouchListener touchListener = new View.OnTouchListener() {
                public boolean onTouch(View v, MotionEvent event) {
                    if(event.getAction() == MotionEvent.ACTION_DOWN) {
                        mInitialX = event.getX();
                        mInitialY = event.getY();
                        mHandler.postDelayed(
                                mFront.getVisibility() == View.VISIBLE ? mFrontLongPressRunnable : mBackLongPressRunnable,
                                ViewConfiguration.getLongPressTimeout());
                    } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        final float deltaX = Math.abs(event.getX() - mInitialX);
                        final float deltaY = Math.abs(event.getY() - mInitialY);
                        if (deltaX > LONG_PRESS_MAX_OFF_DISTANCE || deltaY > LONG_PRESS_MAX_OFF_DISTANCE) {
                            mHandler.removeCallbacks(mFrontLongPressRunnable);
                            mHandler.removeCallbacks(mBackLongPressRunnable);
                        }
                    } else if (event.getAction() == MotionEvent.ACTION_UP) {
                        mHandler.removeCallbacks(mFrontLongPressRunnable);
                        mHandler.removeCallbacks(mBackLongPressRunnable);
                    }

                    return gestureDetector.onTouchEvent(event);
                }
            };

            mFront.setOnTouchListener(touchListener);
            mBack.setOnTouchListener(touchListener);
        }
    }

    public void swapTiles() {
        swapTiles(false, false);
    }

    public void swapTiles(boolean bounce) {
        swapTiles(false, bounce);
    }

    private void swapTiles(boolean animateLeft, final boolean bounce) {
        if(mAnimationLock) return;
        if(mFront.getVisibility() == View.VISIBLE) {
            mFront.animate().translationXBy(animateLeft ? -TRANSLATION_X : TRANSLATION_X).setListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mBack.setVisibility(View.VISIBLE);
                            mAnimationLock = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mBack.bringToFront();
                            mFront.setVisibility(View.GONE);
                            mFront.setTranslationX(0);
                            mAnimationLock = false;
                        }
                    });
        } else {
            mBack.animate().translationXBy(animateLeft ? -TRANSLATION_X : TRANSLATION_X).setListener(
                    new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationStart(Animator animation) {
                            mFront.setVisibility(View.VISIBLE);
                            mAnimationLock = true;
                        }

                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mFront.bringToFront();
                            if (bounce) {
                                bounce(mFront);
                            }
                            mBack.setVisibility(View.GONE);
                            mBack.setTranslationX(0);
                            mAnimationLock = false;
                        }
                    });
        }
    }

    @Override
    public void setEditMode(boolean enabled) {
        super.setEditMode(enabled);
        int visibility = enabled ? View.INVISIBLE : View.VISIBLE;
        if (mFront.getVisibility() == View.VISIBLE) {
            mFront.setSwitchViewVisibility(visibility);
        } else {
            mBack.setSwitchViewVisibility(visibility);
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        // If edit mode is enabled, don't allow childrens to receive touch events
        if(isEditModeEnabled()) {
            return true;
        } else {
            return super.onInterceptTouchEvent(ev);
        }
    }

    private class TileGestureDetector extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (Math.abs(e1.getY() - e2.getY()) > SWIPE_MAX_OFF_DISTANCE) {
                return false;
            }

            final float delta = e1.getX() - e2.getX();
            if(Math.abs(delta) > SWIPE_MIN_DISTANCE) {
                swapTiles(delta > 0, false);
                return true;
            }

            return false;
        }
    }
}
