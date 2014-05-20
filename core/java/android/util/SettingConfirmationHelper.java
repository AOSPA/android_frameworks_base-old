/*
 * Copyright (C) 2013 ParanoidAndroid Project
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

package android.util;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.Movie;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.provider.Settings;

import java.io.InputStream;

import com.android.internal.R;

public class SettingConfirmationHelper {

    private static final int NOT_SET = 0;
    private static final int ENABLED = 1;
    private static final int DISABLED = 2;
    private static final int ASK_LATER = 3;

    public static interface OnSelectListener {
        void onSelect(boolean enabled);
    }

    public static void showConfirmationDialogForSetting(final Context mContext, String title, String msg, Drawable hint,
                                                        final String setting, final OnSelectListener mListener) {

        int mCurrentStatus = Settings.System.getInt(mContext.getContentResolver(), setting, NOT_SET);
        if (mCurrentStatus == ENABLED || mCurrentStatus == DISABLED) return;

        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View dialogLayout = layoutInflater.inflate(R.layout.setting_confirmation_dialog, null);
        final ImageView visualHint = (ImageView)
                dialogLayout.findViewById(R.id.setting_confirmation_dialog_visual_hint);
        visualHint.setImageDrawable(hint);
        visualHint.setVisibility(View.VISIBLE);

        AlertDialog dialog = createDialog(mContext,title,msg,visualHint,setting,mListener);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);

        dialog.show();
    }

    public static void showConfirmationDialogForSetting(final Context mContext, String title, String msg, InputStream gif,
                                                        final String setting, final OnSelectListener mListener) {

        int mCurrentStatus = Settings.System.getInt(mContext.getContentResolver(), setting, NOT_SET);
        if (mCurrentStatus == ENABLED || mCurrentStatus == DISABLED) return;

        LayoutInflater layoutInflater = LayoutInflater.from(mContext);
        View dialogLayout =  layoutInflater.inflate(R.layout.setting_confirmation_dialog, null);
        final GifView gifView = (GifView) dialogLayout.findViewById(R.id.setting_confirmation_dialog_visual_gif);
        gifView.setVisibility(View.VISIBLE);

        AlertDialog dialog = createDialog(mContext,title,msg,gifView,setting,mListener);
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);

        dialog.show();
    }

    private static AlertDialog createDialog(final Context mContext, String title, String msg, View display,
                                                        final String setting, final OnSelectListener mListener) {

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        
        builder.setView(display, 10, 10, 10, 20);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.setting_confirmation_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(mContext.getContentResolver(), setting, ENABLED);
                        if (mListener == null) return;
                        mListener.onSelect(true);
                    }
                }
        );
        builder.setNeutralButton(R.string.setting_confirmation_ask_me_later,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(mContext.getContentResolver(), setting, ASK_LATER);
                        if (mListener == null) return;
                        mListener.onSelect(false);
                    }
                }
        );
        builder.setNegativeButton(R.string.setting_confirmation_no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(mContext.getContentResolver(), setting, DISABLED);
                        if (mListener == null) return;
                        mListener.onSelect(false);
                    }
                }
        );
        builder.setCancelable(false);

        return builder.create();
    }


    private static class GifView extends View {

        private static final int DEFAULT_MOVIEW_DURATION = 1000;

        private Movie mMovie;

        private long mMovieStart;
        private int mCurrentAnimationTime = 0;


        private float mLeft;
        private float mTop;
        private float mScale;

        private int mMeasuredMovieWidth;
        private int mMeasuredMovieHeight;


        GifView(Context aContext, Movie aMovie) {
            super(aContext);

            if (aMovie == null)
                return;

            mMovie = aMovie;
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

            if (mMovie != null) {
                int movieWidth = mMovie.width();
                int movieHeight = mMovie.height();

                //horizontal scaling
                float scaleH = 1f;
                int measureModeWidth = MeasureSpec.getMode(widthMeasureSpec);

                if (measureModeWidth != MeasureSpec.UNSPECIFIED) {
                    int maximumWidth = MeasureSpec.getSize(widthMeasureSpec);
                    if (movieWidth > maximumWidth) {
                        scaleH = (float) movieWidth / (float) maximumWidth;
                    }
                }

                //vertical scaling
                float scaleW = 1f;
                int measureModeHeight = MeasureSpec.getMode(heightMeasureSpec);

                if (measureModeHeight != MeasureSpec.UNSPECIFIED) {
                    int maximumHeight = MeasureSpec.getSize(heightMeasureSpec);
                    if (movieHeight > maximumHeight) {
                        scaleW = (float) movieHeight / (float) maximumHeight;
                    }
                }

                //overall scale
                mScale = 1f / Math.max(scaleH, scaleW);

                mMeasuredMovieWidth = (int) (movieWidth * mScale);
                mMeasuredMovieHeight = (int) (movieHeight * mScale);

                setMeasuredDimension(mMeasuredMovieWidth, mMeasuredMovieHeight);

            } else {
                setMeasuredDimension(getSuggestedMinimumWidth(), getSuggestedMinimumHeight());
            }
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
            super.onLayout(changed, l, t, r, b);
            mLeft = (getWidth() - mMeasuredMovieWidth) / 2f;
            mTop = (getHeight() - mMeasuredMovieHeight) / 2f;
        }

        @Override
        protected void onDraw(Canvas canvas) {
            if (mMovie != null) {
                updateAnimationTime();
                drawMovieFrame(canvas);
                postInvalidateOnAnimation();
            }
        }

        private void updateAnimationTime() {
            long now = android.os.SystemClock.uptimeMillis();

            if (mMovieStart == 0) {
                mMovieStart = now;
            }

            int dur = mMovie.duration();

            if (dur == 0) {
                dur = DEFAULT_MOVIEW_DURATION;
            }

            mCurrentAnimationTime = (int) ((now - mMovieStart) % dur);
        }

        private void drawMovieFrame(Canvas canvas) {

            mMovie.setTime(mCurrentAnimationTime);

            canvas.save(Canvas.MATRIX_SAVE_FLAG);
            canvas.scale(mScale, mScale);
            mMovie.draw(canvas, mLeft / mScale, mTop / mScale);
            canvas.restore();
        }

    }
}