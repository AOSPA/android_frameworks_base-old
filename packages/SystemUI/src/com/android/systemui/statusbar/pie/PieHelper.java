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

package com.android.systemui.statusbar.pie;

import android.app.KeyguardManager;
import android.app.SearchManager;
import android.content.*;
import android.os.UserHandle;

import com.android.systemui.statusbar.BaseStatusBar;

/**
 * Pie Helper
 * Util class: handles system status changes and getting system state.
 * Singleton that must be intialized.
 */
public class PieHelper {
    private static PieHelper mInstance;

    private BaseStatusBar mBar;
    private Context mContext;

    private KeyguardManager mKeyguardManager;

    protected void init(Context context, BaseStatusBar bar) {
        mBar = bar;
        mContext = context;
        mKeyguardManager = (KeyguardManager) mContext.getSystemService(Context.KEYGUARD_SERVICE);
    }

    protected static PieHelper getInstance() {
        if (mInstance == null) mInstance = new PieHelper();
        return mInstance;
    }

    protected boolean isAssistantAvailable() {
        Intent intent = ((SearchManager) mContext.getSystemService(Context.SEARCH_SERVICE))
                .getAssistIntent(mContext, true, UserHandle.USER_CURRENT);
        return intent != null;
    }

    protected void startAssistActivity() {
        mBar.getSearchPanelView().startAssistActivity();
    }

    protected boolean isKeyguardShowing() {
        return mKeyguardManager.isKeyguardLocked();
    }
}
