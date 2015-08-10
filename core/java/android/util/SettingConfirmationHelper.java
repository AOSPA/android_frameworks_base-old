/*
 * Copyright (C) 2014-2015 ParanoidAndroid Project
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

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.view.WindowManager;
import android.provider.Settings;

import com.android.internal.R;

/**
 * Helper for managing OnTheSpot settings.
 *
 * @hide
 */
public class SettingConfirmationHelper {

    /** Default value of no preference. */
    public static final int NOT_SET = 0;

    /** Value of automatic acceptance. */
    public static final int ALWAYS = 1;

    /** Value of automatic denial. */
    public static final int NEVER = 2;

    /** No-op listener implementation for fallback use. */
    private static final OnSelectListener FALLBACK_LISTENER = new OnSelectListener() {

        @Override
        public void onSelect(boolean enabled) {
            // no-op
        }

    };

    /**
     * Initializes the helper object. Should never be used as all interaction with this class
     * is supposed to be static only.
     */
    private SettingConfirmationHelper() {
        // no-op
    }

    /**
     * @hide
     */
    public static interface OnSelectListener {
        void onSelect(boolean enabled);
    }

    /**
     * @throws IllegalArgumentException when either context or setting is null
     * @hide
     */
    public static void request(final Context context, final String setting,
            final String dialog_title, final String dialog_message,
            final OnSelectListener listener) {
        // check the arguments passed in real quick

        if (context == null) {
            throw new IllegalArgumentException("context == null");
        }
        if (setting == null) {
            throw new IllegalArgumentException("setting == null");
        }

        final OnSelectListener callback = listener == null ? FALLBACK_LISTENER : listener;
        final ContentResolver resolver = context.getContentResolver();

        // check if the status has already been set previously

        final int status = Settings.System.getInt(resolver, setting, NOT_SET);
        if (status == ALWAYS) {
            callback.onSelect(true);
            return;
        } else if (status == NEVER) {
            callback.onSelect(false);
            return;
        }

        // check if we can actually create a dialog box

        if (dialog_title == null || dialog_message == null) {
            // default to false here as the safe choice when we can't confirm with the user
            callback.onSelect(false);
            return;
        }

        // create the actual dialog box

        final int currentUserId = ActivityManager.getCurrentUser();

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle(dialog_title);
        builder.setMessage(dialog_message);

        builder.setPositiveButton(R.string.setting_confirmation_always,
                new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                Settings.System.putIntForUser(resolver, setting, ALWAYS, currentUserId);
                callback.onSelect(true);
            }

        });
        builder.setNeutralButton(R.string.setting_confirmation_just_once,
                new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                Settings.System.putIntForUser(resolver, setting, NOT_SET, currentUserId);
                callback.onSelect(true);
            }

        });
        builder.setNegativeButton(R.string.setting_confirmation_never,
                new DialogInterface.OnClickListener() {

            @Override
            public void onClick(final DialogInterface dialog, final int which) {
                Settings.System.putIntForUser(resolver, setting, NEVER, currentUserId);
                callback.onSelect(false);
            }

        });

        builder.setCancelable(false);

        final AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_STATUS_BAR_PANEL);
        dialog.show();
    }

}
