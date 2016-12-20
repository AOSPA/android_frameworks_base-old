/**
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
package android.app;

import android.content.Context;
import android.os.RemoteException;
import android.util.Log;

/**
 * A class that handles theme changes
 * <p>
 * Use {@link android.content.Context#getSystemService(java.lang.String)}
 * with argument {@link android.content.Context#THEME_SERVICE} to get
 * an instance of this class.
 *
 * @author Anas Karbila
 * @hide
 */
public class ThemeManager {

    private static final String TAG = "ThemeManager";

    private Context mContext;
    private IThemeService mService;

    public ThemeManager(Context context, IThemeService service) {
        mContext = context;
        mService = service;
    }

    public void addCallback(IThemeCallback callback) {
        if (mService != null) {
            try {
                mService.addCallback(callback);
            } catch (RemoteException ex) {
                Log.e(TAG, "Failed to dispatch callback");
            }
        }
    }
}
