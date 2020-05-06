/**
 * Copyright (C) 2019 The ParanoidAndroid Project
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
package com.android.systemui.secureapps;

import android.app.ActivityManager;
import android.app.IActivityManager;
import android.app.admin.DevicePolicyManager;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Process;
import android.os.RemoteException;
import android.os.StrictMode;
import android.os.SystemClock;
import android.os.Trace;
import android.os.UserHandle;
import android.text.Editable;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Gravity;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.systemui.R;
import com.android.systemui.secureapps.ColorUtils;

import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.TextViewInputDisabler;

import java.util.List;
import java.util.ArrayList;

public class AppLockUi {
    // TODO: Unlock, onResult stuff + check password
    // TODO: set wasAppOpen --> AppLockService?
    /*
     * R.id.app_icon
     * R.id.app_name
     * R.id.password_entry_error
     * R.id.password_entry
     * R.id.lock_pattern
     * R.id.pin_input
     */
    private static final String TAG = "AppLockUi";
    private static final boolean DEBUG = true;
    private static final String FOD = "vendor.pa.biometrics.fingerprint.inscreen";
    private static final long ERROR_TIMEOUT_MILLIS = 1500;
    private static final int PATTERN_CLEAR_TIMEOUT_MS = 2000;
    private static final int MAX_WRONG_ATTEMPTS = 5;
    private static final int MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT = 3;

    /* Manager and Services */
    private static final SecureAppsService mService;
    private FingerprintManager mFingerprintManager;
    private Handler mHandler;
    private Handler mSecureAppsHandler;
    private WindowManager mWindowManager;
    private InputMethodManager mImm;
    private PackageManager mPackageManager;
    private Context mContext;
    private String mPackageName;
    private ApplicationInfo mApplicationInfo;
    private boolean mShowing; // True if the view is attached to the app windows
    private boolean mLayoutChanged;
    private boolean mBlockingAppView;
    private boolean mFingerprintListening;
    private boolean mIsAppWindowVisible;
    private int mUserId;

    // Keep track of the window tokens for a particular app as they come and go
    private final ArrayList<IBinder> mWindowTokens = new ArrayList<>();

    /* UI related code */
    private LinearLayout mLockView; // The AppLockView itself
    private LinearLayout mBlockView; // The AppLockView itself
    private TextView mAppName; // App label
    private ImageView mCloseButton;
    private Canvas mCanvas; // Icon + badge
    private LayoutParams mLayoutParams;
    private LayoutParams mLayoutParamsSys;
    private int mRotation;  //0,2:Portrait 1,3:Landscape

    // Lock related stuff
    private TextView mErrorText;
    // Pattern
    private boolean mIsPattern;
    private AsyncTask<?, ?, ?> mPendingPatternLockCheck;
    private LockPatternView mLockPatternView;
    // Fingerprint
    private boolean mHasFingerprint;
    private boolean mHasFOD;
    private View mFODview;
    // Pin/password
    private boolean mIsPassword;
    private boolean mIsPin;
    private TextView mPasswordEntry;
    private TextViewInputDisabler mPasswordEntryInputDisabler;
    private AsyncTask<?, ?, ?> mPendingLockCheck;

    /* Global unlock stuff */
    private static int sWrongAttempts = 0;
    private static CountDownTimer sTimer;
    private static boolean sTimerRunning = false;
    private static long sCountdown = 0;
    private static LockPatternUtils sLockPatternUtils;
    private final Runnable mCancelPatternRunnable = new Runnable() {
        @Override
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    public AppLockUi(SecureAppsService service, String packageName, int userId) {
        mContext = service.this;
        mPackageName = packageName;
        mUserId = userId;
        mService = service;
        mHandler = new Handler(Looper.getMainLooper());
        mSecureAppsHandler = SecureAppsThread.getHandler();
        mImm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mPackageManager = mContext.getPackageManager();
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
        mHasFingerprint = mPackageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT);
        mHasFOD = mPackageManager.hasSystemFeature(FOD);
        mRotation = mContext.getResources().getConfiguration().windowConfiguration.getRotation();
        if (mHasFingerprint) {
            mFingerprintManager = (FingerprintManager) mContext.
                getSystemService(Context.FINGERPRINT_SERVICE);
        }
        try {
            mApplicationInfo = mPackageManager.getApplicationInfoAsUser(mPackageName, 0, mUserId);
        } catch(PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to find package " + mPackageName, e);
        }
        mShowing = false;
        mBlockingAppView = false;
        mLayoutChanged = false;
        mFingerprintListening = false;
        mCanvas = new Canvas();
        mCanvas.setDrawFilter(new PaintFlagsDrawFilter(
                Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
        mLayoutParams = new LayoutParams(LayoutParams.TYPE_APPLICATION_PANEL);
        mLayoutParams.windowAnimations = R.style.Animation_AppLock;
        mLayoutParamsSys = new LayoutParams(LayoutParams.TYPE_SYSTEM_ALERT);
        mLayoutParamsSys.format = PixelFormat.OPAQUE;
        mLayoutParamsSys.height = LayoutParams.MATCH_PARENT;
        mLayoutParamsSys.width = LayoutParams.MATCH_PARENT;
        mLayoutParamsSys.gravity = Gravity.CENTER;
        mLayoutParamsSys.layoutInDisplayCutoutMode =
                LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        mLayoutParamsSys.flags = LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
                | LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | LayoutParams.FLAG_HARDWARE_ACCELERATED
                | LayoutParams.FLAG_LAYOUT_IN_SCREEN;
        mBlockView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.app_lock_view, null);
        ((TextView) mBlockView.findViewById(R.id.app_name)).setText(mApplicationInfo == null ?
                "" : mPackageManager.getApplicationLabel(mApplicationInfo));
        mBlockView.findViewById(R.id.password_entry_error).setVisibility(View.GONE);
        mBlockView.findViewById(R.id.password_entry).setVisibility(View.GONE);
        inflateViews();
    }

    private void inflateViews() {
        mHandler.post(() -> {
            mLockView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.app_lock_view, null);
            updateViews();
        });
    }

    private void updateViews() {
        mPasswordEntry = (TextView) mLockView.findViewById(R.id.password_entry);
        mPasswordEntry.setOnEditorActionListener(new EditorActionListener());
        mPasswordEntry.addTextChangedListener(new TextWatcher() {  
            @Override
            public void onTextChanged(CharSequence cs, int start, int before, int count) {
                if (mIsPin && cs.length() == 4) {
                    checkPassword(mPasswordEntry.getText().toString().getBytes());
                }
            }
            @Override
            public void beforeTextChanged(CharSequence arg0, int start, int count, int after) { }
            @Override
            public void afterTextChanged(Editable arg0) { }
        });

        mPasswordEntry.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch (View v, MotionEvent event) {
                if (mPasswordEntry.hasFocus() && mHasFOD && mImm.isActive(mPasswordEntry)) {
                    Log.d(TAG, "mPasswordEntry onTouch stopListeningForFingerprint" + mPackageName);
                    mService.stopListeningForFingerprint();
                }
                return false;
            }
        });

        mPasswordEntry.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange (View v, boolean hasFocus) {
                if (hasFocus && mHasFOD && mImm.isActive(mPasswordEntry)) {
                    Log.d(TAG, "mPasswordEntry onFocusChange stopListeningForFingerprint" + mPackageName);
                    mService.stopListeningForFingerprint();
                }
            }
        });

        mPasswordEntry.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);
        mCloseButton = (ImageView) mLockView.findViewById(R.id.close_button);
        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                killApplication();
            }
        });

        mLockPatternView = (LockPatternView) mLockView.findViewById(R.id.lock_pattern);
        mLockPatternView.setSaveEnabled(false);
        mLockPatternView.setOnPatternListener(new UnlockPatternListener());
        mLockPatternView.setTactileFeedbackEnabled(sLockPatternUtils.isTactileFeedbackEnabled());
        if (mRotation == 1 || mRotation == 0) {
            mFODview = mLockView.findViewById(R.id.FOD_view);
            mFODview.setVisibility((mHasFOD && mHasFingerprint) ? View.VISIBLE : View.GONE);
        } else if (mRotation == 3) {
            mFODview = mLockView.findViewById(R.id.FOD_view_start);
            mFODview.setVisibility((mHasFOD && mHasFingerprint) ? View.VISIBLE : View.GONE);
        }
        mErrorText = (TextView) mLockView.findViewById(R.id.password_entry_error);
        mAppName = (TextView) mLockView.findViewById(R.id.app_name);
        mAppName.setText(mApplicationInfo == null ? "" :
                mPackageManager.getApplicationLabel(mApplicationInfo));
        loadUi();
    }

    private void loadUi() {
        int backgroundColor;
        int textColor;
        Drawable icon = mApplicationInfo == null ?
                null : mPackageManager.getApplicationIcon(mApplicationInfo);
        if (icon != null) {
            backgroundColor = ColorUtils.getIconColorFromDrawable(icon);
            if (backgroundColor == -3) {
                backgroundColor = mContext.getResources().getColor(R.color.applock_bg);
            }
            textColor = ColorUtils.isColorDark(backgroundColor) ?
                    mContext.getColor(R.color.white) : mContext.getColor(R.color.black);
            ImageView appIcon = (ImageView) mLockView.findViewById(R.id.app_icon);
            ImageView appIcon2 = (ImageView) mBlockView.findViewById(R.id.app_icon);
            int badgeSize = mContext.getResources().getDimensionPixelSize(
                    R.dimen.app_lock_badge_size);
            int iconHeight = mContext.getResources().getDimensionPixelSize(
                    R.dimen.app_icon_height);
            int iconWidth = mContext.getResources().getDimensionPixelSize(
                    R.dimen.app_icon_width);
            if (icon.getIntrinsicHeight() != iconHeight
                    || icon.getIntrinsicWidth() != iconWidth) {
                icon = new IntrinsicSizeDrawable(icon, iconWidth, iconHeight);
            }
            Bitmap bitmap = ColorUtils.drawableToBitmap(icon);
            if (bitmap != null) {
                mCanvas.setBitmap(bitmap);
                LayerDrawable badgeDrawable = (LayerDrawable) mContext.getResources().getDrawable(
                        R.drawable.ic_locked_app_badge);
                Drawable lock = badgeDrawable.findDrawableByLayerId(R.id.lock);
                Drawable circle = badgeDrawable.findDrawableByLayerId(R.id.circle);
                lock.setTint(textColor);
                circle.setTint(backgroundColor);
                badgeDrawable.setBounds(iconHeight - badgeSize, iconHeight - badgeSize,
                        iconHeight, iconHeight);
                badgeDrawable.draw(mCanvas);
                mCanvas.setBitmap(null);
                appIcon.setImageBitmap(bitmap);
                appIcon2.setImageBitmap(bitmap);
            } else {
                appIcon.setImageDrawable(icon);
                appIcon2.setImageDrawable(icon);
            }
        } else {
            backgroundColor = mContext.getResources().getColor(R.color.applock_bg);
            textColor = ColorUtils.isColorDark(backgroundColor) ?
                    mContext.getColor(R.color.white) : mContext.getColor(R.color.black);
        }
        setBackgroundColor(backgroundColor);
        setTextColor(textColor);
    }

    static void setLockPatternUtils(LockPatternUtils utils) {
        sLockPatternUtils = utils;
    }

    private void reloadPasswordSetup() {
        int storedQuality = sLockPatternUtils.getKeyguardStoredPasswordQuality(mUserId);
        mIsPattern = false;
        mIsPassword = false;
        mIsPin = false;
        switch (storedQuality) {
            case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                mLockPatternView.setVisibility(View.VISIBLE);
                mLockPatternView.post(mCancelPatternRunnable);
                mLockPatternView.enableInput();
                mLockPatternView.setInStealthMode(!sLockPatternUtils.isVisiblePatternEnabled(mUserId));
                mPasswordEntry.setVisibility(View.GONE);
                mErrorText.setVisibility(View.VISIBLE);
                mErrorText.setText(mContext.getString(R.string.lockscreen_pattern_instructions));
                mIsPattern = true;
                mIsPassword = false;
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                mIsPin = true;
                break;
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
            case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
            case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
            case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                mIsPassword = true;
                break;
            default:
                mLockPatternView.setVisibility(View.GONE);
                mPasswordEntry.setVisibility(View.GONE);
                mErrorText.setVisibility(View.GONE);
                removeBlockView();
                hide();
                if (!isFingerprintAuthAvailable()) {
                    return;
                }
                break;
        }
        if (mIsPassword || mIsPin) {
            mLockPatternView.setVisibility(View.GONE);
            mPasswordEntry.setVisibility(View.VISIBLE);
            mPasswordEntry.clearFocus();
            mErrorText.setVisibility(View.VISIBLE);
            mErrorText.setText(mContext.getString(R.string.lockscreen_pin_instructions));
            if (mIsPassword) {
                mPasswordEntry.setInputType(InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            } else {
                mPasswordEntry.setInputType(InputType.TYPE_CLASS_NUMBER
                        | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
            }
        }
    }

    private boolean isFingerprintAuthAvailable() {
        return mHasFingerprint
                && mFingerprintManager.hasEnrolledFingerprints(mUserId);
    }

    public boolean isGame() {
        Log.d(TAG, "isGame() " + mPackageName + " category:" + mApplicationInfo.category);
        return (mApplicationInfo.category == ApplicationInfo.CATEGORY_GAME ||
                      (mApplicationInfo.flags & ApplicationInfo.FLAG_IS_GAME)
                        == ApplicationInfo.FLAG_IS_GAME);
    }

    public void setIsGame() {
        mHandler.post(() -> {
            Log.d(TAG, "mLockView setOnKeyListener() " + mPackageName + " isGame: true" );
            mLockView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey (View v, int keyCode, KeyEvent event) {
                    Log.d(TAG, "mLockView onKey() " + mPackageName + " keyCode:" + keyCode + " event:" + event);
                    if (keyCode == KeyEvent.KEYCODE_BACK || keyCode == KeyEvent.KEYCODE_HOME) {
                        hide();
                    }
                    return false;
                }
            });
            mLockView.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch (View v, MotionEvent event) {
                    Log.d(TAG, "mLockView onTouch() " + mPackageName + " event:" + event);
                    return false;
                }
            });
        });
    }

    public String getPackageName() {
        return mPackageName;
    }

    public boolean isShowing() {
        return mShowing;
    }

    public boolean isAppWindowsVisible() {
        boolean visible = false;
        visible |= mIsAppWindowVisible;
        Log.v(TAG, "isAppWindowsVisible(): packageName=" + mPackageName + " visible: " + visible);
        return visible;
    }

    public void updateAppWindow(IBinder token, boolean isAppWindowsVisible) {
        mIsAppWindowVisible = isAppWindowsVisible;
        Log.v(TAG, "updateAppWindow(): packageName=" + mPackageName + " add window: " + token + " to: " + mWindowTokens);
        if (!mWindowTokens.contains(token)) {
            mWindowTokens.add(token);
        }
    }

    // There may be some remaining rogue tokens that haven't been cleared yet
    public void clearAllAppWindows() {
        mWindowTokens.clear();
    }

    public void setAppWindowGone(IBinder token) {
        Log.v(TAG, "setAppWindowsGone(): packageName=" + mPackageName + " remove window: " + token + " from: " + mWindowTokens);
        if (mWindowTokens.contains(token)) {
            mWindowTokens.remove(token);
        }
        if (mWindowTokens.size() == 0) {
            hide();
            removeBlockView();
        }
    }

    public boolean updateConfig(Configuration newConfig) {
        final int rot = newConfig.windowConfiguration.getRotation();
        if (mRotation != rot) {
            Log.d(TAG, "Orientation Changed 1 packageName=" + mPackageName);
            mRotation = rot;
            mLayoutChanged = mShowing;
            hide();
            inflateViews();
            if (mLayoutChanged) {
                final int N = mWindowTokens.size();
                if (N > 0) {
                    show(mWindowTokens.get(N-1));
                    return true;
                }
            }
        }
        mLayoutChanged = false;
        return false;
    }

    public void launchBeforeActivity() {
        reloadPasswordSetup();
        if (sTimerRunning) {
            setInputEnabled(false);
        } else {
            setInputEnabled(true);
        }
        if(!mShowing) {
            try {
                mWindowManager.addView(mLockView, mLayoutParamsSys);
                mShowing = true;
                if (DEBUG) Log.v(TAG, "launchBeforeActivity(): done packageName=" + mPackageName);
            } catch (RuntimeException e) {
                Log.e(TAG, "Failed to add View", e);
            }
        }
    }

    private void killApplication() {
        IActivityManager am = ActivityManager.getService();
        if (am != null) {
            Log.d(TAG, "Killing application: " + mPackageName);
            try {
                am.killApplication(mPackageName, UserHandle.getAppId(mApplicationInfo.uid), mUserId, "Kill");
                hide();
                mService.stopListeningForFingerprint();
            } catch (RemoteException e) {
                Log.e(TAG, "Failed to kill application: ", e);
            }
        }
    }

    // We block the app view till they are ready(fully drawn).
    public void onAppLaunching() {
        if (DEBUG) Log.v(TAG, "onAppLaunching(): packageName=" + mPackageName + " mShowing=" + mShowing);
        final int N = mWindowTokens.size();
        if (N > 0) show(mWindowTokens.get(N-1));
    }

    // Once the app is fully drawn, we switch to the AppLock view
    public void show(IBinder token, boolean isAppWindowsVisible) {
        mIsAppWindowVisible = isAppWindowsVisible;
        reloadPasswordSetup();
        Log.v(TAG, "show(): packageName=" + mPackageName + " add window: " + token + " to: " + mWindowTokens);
        if (!mWindowTokens.contains(token)) {
            mWindowTokens.add(token);
            hide();
        }
        mLayoutParams.token = token;
        addView();
        if (sTimerRunning) {
            setInputEnabled(false);
        } else {
            setInputEnabled(true);
        }
    }

    public void hide() {
        if (DEBUG) Log.v(TAG, "hide(): packageName=" + mPackageName);
        removeView();
        setInputEnabled(false);
    }

    public void notifyTimerTick() {
        String text = mContext.getString(
                R.string.lockscreen_too_many_failed_attempts_countdown, (int) (sCountdown / 1000));
        mErrorText.setText(text);
    }

    public void notifyTimerStart() {
        if (DEBUG) Log.v(TAG, "addView(): notifyTimerStart");
        setInputEnabled(false);
    }

    public void notifyTimerStop() {
        if (DEBUG) Log.v(TAG, "addView(): notifyTimerStop");
        setInputEnabled(true);
    }

    private void addView() {
        mSecureAppsHandler.post(() -> {
            if(!mShowing) {
                try {
                    mWindowManager.addView(mLockView, mLayoutParams);
                    mShowing = true;
                    if (DEBUG) Log.v(TAG, "addView(): DONE packageName=" + mPackageName);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to add View", e);
                } finally {
                    if (!mShowing) {
                        try {
                            mWindowManager.removeView(mLockView);
                            if (DEBUG) Log.d(TAG, "addView() FAILED, now removeView(): Done");
                        } catch(IllegalStateException | IllegalArgumentException e) {
                            Log.e(TAG, "Failed to remove View", e);
                        }
                    }
                }
            }
        });
    }

    private void addBlockView() {
        mSecureAppsHandler.post(() -> {
            if(!mBlockView.isAttachedToWindow()) {
                try {
                    mWindowManager.addView(mBlockView, mLayoutParamsSys);
                    if (DEBUG) Log.v(TAG, "addBlockView(): done packageName=" + mPackageName);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to add View", e);
                }
            }
        });
    }

    private void removeBlockView() {
        if (mBlockingAppView) {
            try {
                mBlockingAppView = false;
                mWindowManager.removeView(mBlockView);
                if (DEBUG) Log.d(TAG, "removeBlockView(): Done");
            } catch(IllegalStateException | IllegalArgumentException e) {
                Log.e(TAG, "Failed to remove View", e);
            }
        }
    }

    private void removeView() {
        if (mShowing) {
            try {
                mShowing = false;
                mWindowManager.removeView(mLockView);
                if (DEBUG) Log.d(TAG, "removeView(): Done");
            } catch(IllegalStateException | IllegalArgumentException e) {
                Log.e(TAG, "Failed to remove View", e);
            }
        }
    }

    private void onUnlockFailed(int timeoutMs) {
        if (DEBUG) Log.v(TAG, "onUnlockFailed(): packageName=" + mPackageName);
        sWrongAttempts++;
        String errorText;
        if (timeoutMs > 0) {
            long deadline = sLockPatternUtils.setLockoutAttemptDeadline(mUserId, timeoutMs);
            startCountdown(deadline);
            int resWrong;
            if (mIsPattern) {
                resWrong = R.string.kg_wrong_pattern;
            } else if (mIsPin) {
                resWrong = R.string.kg_wrong_pin;
            } else {
                resWrong = R.string.kg_wrong_password;
            }
            errorText = mContext.getString(resWrong);
        } else {
            errorText = mContext.getString(R.string.remaining_attempts_pattern,
                    MAX_WRONG_ATTEMPTS - sWrongAttempts);
        }
        mErrorText.setText(errorText);
        clearInput();
    }

    public void onUnlockSucceed() {
        if (DEBUG) Log.v(TAG, "onUnlockSucceed(): packageName=" + mPackageName);
        if (sTimerRunning) {
            sTimer.cancel();
        }
        mService.addOpenedApp(mPackageName);
        clearInput();
        sWrongAttempts = 0;
        hide();
        removeBlockView();
    }

    private void startCountdown(long elapsedRealtimeDeadline) {
        if (DEBUG) Log.v(TAG, "startCountdown(): packageName=" + mPackageName);
        if (sTimerRunning) return;
        mService.notifyTimerStartForAll();
        final long elapsedRealtime = SystemClock.elapsedRealtime();
        sTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                sCountdown = millisUntilFinished;
                mService.notifyTimerTickForAll();
            }
            @Override
            public void onFinish() {
                sTimerRunning = false;
                sWrongAttempts = 0;
                sTimer = null;
                mService.notifyTimerStopForAll();
            }
        };
        sTimer.start();
        sTimerRunning = true;
    }

    private void setInputEnabled(boolean enabled) {
        //mHandler.post(() -> {
            if (DEBUG) Log.v(TAG, "setInputEnabled(" + String.valueOf(enabled) +
                    "): packageName=" + mPackageName);
            mLockPatternView.clearPattern();
            mLockPatternView.setEnabled(enabled);
            mPasswordEntry.setEnabled(enabled);
            mPasswordEntryInputDisabler.setInputEnabled(enabled);
            if (enabled) {
                String text = mContext.getString(mIsPattern
                        ? R.string.lockscreen_pattern_instructions
                        : R.string.lockscreen_pin_instructions);
                mErrorText.setText(text);
                mLockPatternView.enableInput();
                if (!isFingerprintAuthAvailable()) {
                    mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                clearInput();
                notifyTimerTick();
                mPasswordEntry.clearFocus();
                mLockPatternView.disableInput();
            }
        //});
    }

    private void clearInput() {
        //mHandler.post(() -> {
            mPasswordEntry.setText("");
            mLockPatternView.clearPattern();
        //});
    }

    private void setTextColor(int color) {
        mPasswordEntry.setTextColor(color);
        mPasswordEntry.setBackgroundTintList(ColorStateList.valueOf(color));
        mAppName.setTextColor(color);
        mErrorText.setTextColor(color);
        //mLockPatternView.setRegularColor(color);
        mCloseButton.setColorFilter(color);
        ((TextView) mBlockView.findViewById(R.id.app_name)).setTextColor(color);
    }

    private void setBackgroundColor(int color) {
        mLockView.setBackgroundColor(color);
        mBlockView.setBackgroundColor(color);
    }

    private void checkPassword(final byte[] password) {
        if (password != null && password.length <= MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT) {
            return;
        }
        mPasswordEntryInputDisabler.setInputEnabled(false);
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
        }
        mPendingLockCheck = LockPatternChecker.checkPassword(
                sLockPatternUtils, password, mUserId, new LockPatternChecker.OnCheckCallback() {
                    @Override
                    public void onEarlyMatched() {
                        onUnlockSucceed();
                    }
                    @Override
                    public void onChecked(boolean matched, int timeoutMs) {
                        mPasswordEntryInputDisabler.setInputEnabled(true);
                        if (!matched) {
                            onUnlockFailed(timeoutMs);
                        }
                        mPendingLockCheck = null;
                    }
                    @Override
                    public void onCancelled() {
                        mPasswordEntryInputDisabler.setInputEnabled(true);
                        mPendingLockCheck = null;
                    }
                });
    }

    private class UnlockPatternListener implements LockPatternView.OnPatternListener {
        @Override
        public void onPatternStart() {
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);
        }
        @Override
        public void onPatternCleared() {
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);
        }
        @Override
        public void onPatternCellAdded(List<LockPatternView.Cell> pattern) {
        }
        @Override
        public void onPatternDetected(final List<LockPatternView.Cell> pattern) {
            if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                return;
            }
            mLockPatternView.disableInput();
            if (mPendingPatternLockCheck != null) {
                mPendingPatternLockCheck.cancel(false);
            }
            mPendingPatternLockCheck = LockPatternChecker.checkPattern(
                    sLockPatternUtils,
                    pattern,
                    mUserId,
                    new LockPatternChecker.OnCheckCallback() {
                        @Override
                        public void onEarlyMatched() {
                            sLockPatternUtils.sanitizePassword();
                            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
                            onUnlockSucceed();
                        }
                        @Override
                        public void onChecked(boolean matched, int timeoutMs) {
                            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
                            mLockPatternView.enableInput();
                            if (!matched) {
                                onUnlockFailed(timeoutMs);
                                // XXX: removeCallback(), postDelayed() ???
                            }
                            mPendingPatternLockCheck = null;
                        }
                        @Override
                        public void onCancelled() {
                            mLockPatternView.enableInput();
                            mPendingPatternLockCheck = null;
                        }
                    });
        }
    }

    private class EditorActionListener implements OnEditorActionListener {
        @Override
        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
            // Check if this was the result of hitting the enter or "done" key
            if (actionId == EditorInfo.IME_NULL
                    || actionId == EditorInfo.IME_ACTION_DONE
                    || actionId == EditorInfo.IME_ACTION_NEXT) {
                checkPassword(mPasswordEntry.getText().toString().getBytes());
                return true;
            }
            return false;
        }
    }

    /**
     * A wrapper around another Drawable that overrides the intrinsic size.
     */
    private static class IntrinsicSizeDrawable extends InsetDrawable {
        private final int mIntrinsicWidth;
        private final int mIntrinsicHeight;
        public IntrinsicSizeDrawable(Drawable drawable, int intrinsicWidth, int intrinsicHeight) {
            super(drawable, 0);
            mIntrinsicWidth = intrinsicWidth;
            mIntrinsicHeight = intrinsicHeight;
        }
        @Override
        public int getIntrinsicWidth() {
            return mIntrinsicWidth;
        }
        @Override
        public int getIntrinsicHeight() {
            return mIntrinsicHeight;
        }
    }

    public class SecureAppsThread extends HandlerThread {

        private static SecureAppsThread sInstance;
        private static Handler sHandler;

        public SecureAppsThread(String name, int priority) {
            super(name, priority);
        }

        private static void ensureThreadLocked() {
            if (sInstance == null) {
                sInstance = new SecureAppsThread("android.display_secureapps", Process.THREAD_PRIORITY_DISPLAY + 1);
                sInstance.start();
                sInstance.getLooper().setTraceTag(Trace.TRACE_TAG_SYSTEM_SERVER);
                sHandler = new Handler(sInstance.getLooper());
            }
        }

        public static Handler getHandler() {
            synchronized (SecureAppsThread.class) {
                ensureThreadLocked();
                return sHandler;
            }
        }

        @Override
        public void run() {
            Process.setCanSelfBackground(false);
            StrictMode.initThreadDefaults(null);
            super.run();
        }

    }
}