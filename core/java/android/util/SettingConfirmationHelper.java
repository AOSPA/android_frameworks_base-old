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
import android.content.pm.ThemeUtils;
import android.graphics.drawable.Drawable;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.android.internal.R;

/**
 * Hide from public API
 * @hide
 */
public class SettingConfirmationHelper {

    private static final int NOT_SET = 0;
    private static final int ENABLED = 1;
    private static final int DISABLED = 2;
    private static final int ASK_LATER = 3;

    private static final boolean DEBUG_SHOW_RESET_HELP_ALL_TIME = false;
    private static boolean mFirstRun;

    private static Context mUiContext; /* theme engine context for getting just resources */

    /**
     * @hide
     */
    public static interface OnSelectListener {
        void onSelect(boolean enabled);
    }

    /**
     * @hide
     */
    public static void showConfirmationDialogForSetting(final Context mContext, String title, String msg, Drawable hint,
                                                        final String setting, final OnSelectListener mListener) {
        mUiContext = ThemeUtils.createUiContext(mContext); // avoid package mismatch

        int mCurrentStatus = Settings.System.getInt(/*use system context to read*/mContext.getContentResolver(), setting, NOT_SET);
        if (mCurrentStatus == ENABLED || mCurrentStatus == DISABLED) return;

        mFirstRun = Settings.System.getInt(mContext.getContentResolver(), Settings.System.ON_THE_SPOT_FIRST_TIME, 1) == 1;

        AlertDialog.Builder builder = new AlertDialog.Builder(mUiContext);
        LayoutInflater layoutInflater = LayoutInflater.from(mUiContext);
        View dialogLayout = layoutInflater.inflate(R.layout.setting_confirmation_dialog, null);
        final ImageView visualHint = (ImageView)
                dialogLayout.findViewById(R.id.setting_confirmation_dialog_visual_hint);
        visualHint.setImageDrawable(hint);
        builder.setView(dialogLayout, 10, 10, 10, 20);
        builder.setTitle(title);
        builder.setMessage(msg);
        builder.setPositiveButton(R.string.setting_confirmation_yes,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(/*use system context to write*/mContext.getContentResolver(), setting, ENABLED);
                        if (DEBUG_SHOW_RESET_HELP_ALL_TIME || mFirstRun) showHowToReset(mContext);
                        if (mListener == null) return;
                        mListener.onSelect(true);
                    }
                }
        );
        builder.setNeutralButton(R.string.setting_confirmation_ask_me_later,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(/*use system context to write*/mContext.getContentResolver(), setting, ASK_LATER);
                        if (mListener == null) return;
                        mListener.onSelect(false);
                    }
                }
        );
        builder.setNegativeButton(R.string.setting_confirmation_no,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Settings.System.putInt(/*use system context to write*/mContext.getContentResolver(), setting, DISABLED);
                        if (DEBUG_SHOW_RESET_HELP_ALL_TIME || mFirstRun) showHowToReset(mContext);
                        if (mListener == null) return;
                        mListener.onSelect(false);
                    }
                }
        );
        builder.setCancelable(false);
        AlertDialog dialog = builder.create();
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);

        dialog.show();
    }

    /**
     * @hide
     */
    public static void showHowToReset(final Context mContext) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mUiContext);
        builder.setTitle(R.string.setting_reset_title);
        builder.setMessage(R.string.setting_reset_message);
        builder.setCancelable(true);
        builder.setNeutralButton(android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        Settings.System.putInt(mContext.getContentResolver(), Settings.System.ON_THE_SPOT_FIRST_TIME, 0);
                    }
                }
        );
        AlertDialog dialog = builder.create();
        Window dialogWindow = dialog.getWindow();
        dialogWindow.setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);

        dialog.show();
    }

}
