/*package com.android.systemui;

import android.app.IThemeCallback;
import android.app.ThemeManager;
import android.content.Context;

public class ThemeController implements IThemeCallback.Stub() {

    private static final String TAG = "ThemeController";
    private static final boolean DEBUG = true;

    private Context mContext;

    private ThemeManager mThemeManager;
    private boolean mIsThemeApplied;

    public ThemeController(Context context) {
        mContext = context;
    }

    @Override
    public void onThemeChanged(boolean isThemeApplied) {
        if (DEBUG) Log.d(TAG, "onThemeChanged callback called");
        boolean changed = false;
        if (isThemeApplied != mIsThemeApplied) {
            mIsThemeApplied = isThemeApplied;
            changed = true;
        }
        if (changed) {
            if (DEBUG) Log.d(TAG, "ThemeManager state has been changed");
            //updateResources();
        }
    }
}
*/
