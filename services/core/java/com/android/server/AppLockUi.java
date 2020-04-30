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

package com.android.server;

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
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
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

import com.android.internal.R;
import com.android.internal.util.pa.ColorUtils;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.TextViewInputDisabler;

import java.util.List;

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
    private static final AppLockService sAppLockService = LocalServices.getService(AppLockService.class);
    private FingerprintManager mFingerprintManager;
    private Handler mHandler;
    private WindowManager mWindowManager;
    private InputMethodManager mImm;
    private PackageManager mPackageManager;

    private Context mContext;

    private String mPackageName;
    private ApplicationInfo mApplicationInfo;

    private boolean mShowing; // True if the view is attached to the app windows
    private boolean mLayoutChanged;
    private boolean mBlockingAppView;
    private int mUserId;

    // Keep track of the window tokens for a particular app as they come and go
    private final ArraySet<IBinder> mWindowTokens = new ArraySet<>();

    /* UI related code */
    private LinearLayout mLockView; // The AppLockView itself
    private LinearLayout mBlockView; // The AppLockView itself
    private TextView mAppName; // App label
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
    private CancellationSignal mCancellationSignal;

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

    public AppLockUi(Context context, String packageName, int userId) {
        mContext = context;
        mPackageName = packageName;
        mUserId = userId;
        mHandler = new Handler(Looper.getMainLooper());
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
        mCanvas = new Canvas();
        mCanvas.setDrawFilter(new PaintFlagsDrawFilter(
                Paint.DITHER_FLAG, Paint.FILTER_BITMAP_FLAG));
        mLayoutParams = new LayoutParams(LayoutParams.TYPE_APPLICATION_PANEL);
        mLayoutParams.windowAnimations = com.android.internal.R.style.Animation_AppLock;

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

        inflateViews();
    }

    private void inflateViews() {
        mLockView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.app_lock_view, null);
        mBlockView = (LinearLayout) LayoutInflater.from(mContext).inflate(R.layout.app_lock_view, null);
        updateViews();
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
                Log.d(TAG, "mPasswordEntry onTouch " + mPackageName + " ImmActive:" + mImm.isActive(mPasswordEntry));
                if (mPasswordEntry.hasFocus() && mHasFOD && mImm.isActive(mPasswordEntry)) {
                    mHandler.post(() -> stopListeningForFingerprint());
                }
                return false;
            }
        });
        mPasswordEntry.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange (View v, boolean hasFocus) {
                Log.d(TAG, "mPasswordEntry onFocusChange " + mPackageName + " ImmActive:" + mImm.isActive(mPasswordEntry) + " hasFocus:" + hasFocus);
                if (hasFocus && mHasFOD && mImm.isActive(mPasswordEntry)) {
                    mHandler.post(() -> stopListeningForFingerprint());
                }
            }
        });

        
        /*mLockView.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey (View v, int keyCode, KeyEvent event) {
                Log.d(TAG, "mLockView onKey() " + mPackageName + " ImmActive:" + mImm.isActive() + " keyCode:" + keyCode + " event:" + event);
                return false;
            }
        });
        mLockView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange (View v, boolean hasFocus) {
                Log.d(TAG, "mLockView onFocusChange " + mPackageName + " ImmActive:" + mImm.isActive(mPasswordEntry) + " hasFocus:" + hasFocus);
                if (!hasFocus) {

                }
            }
        });
        mLockView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch (View v, MotionEvent event) {
                Log.d(TAG, "mLockView onTouch " + mPackageName + " ImmActive:" + mImm.isActive(mPasswordEntry) + " event:" + event);
                return false;
            }
        });*/

        mPasswordEntry.setTransformationMethod(PasswordTransformationMethod.getInstance());
        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);

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

        ((TextView) mBlockView.findViewById(R.id.app_name)).setText(mApplicationInfo == null ?
                "" : mPackageManager.getApplicationLabel(mApplicationInfo));
        mBlockView.findViewById(R.id.password_entry_error).setVisibility(View.GONE);
        mBlockView.findViewById(R.id.password_entry).setVisibility(View.GONE);
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

    public String getPackageName() {
        return mPackageName;
    }

    public boolean isShowing() {
        return mShowing;
    }

    public boolean isAppWindowsVisible() {
        return mWindowTokens.size() > 0;
    }

    public void updateAppBinderToken(IBinder token) {
        Log.v(TAG, "updateAppBinderToken(): packageName=" + mPackageName + " add token: " + token + " to: " + mWindowTokens);
        if (token != null && (mLayoutParams.token != token || !mWindowTokens.contains(token))) {
            mLayoutParams.token = token;
            mWindowTokens.add(mLayoutParams.token);
            if (mShowing) {
                Log.v(TAG, "updateAppBinderToken(): wasShowing pkg:" + mPackageName);
                hide();
                show(mLayoutParams.token);
            }
            if (mLayoutChanged) {
                show(mLayoutParams.token);
            }
        }
    }

    // There may be some remaining rogue tokens that haven't been cleared yet
    public void clearAllAppWindows() {
        mWindowTokens.clear();
    }

    public void setAppWindowsGone(IBinder token) {
        if (token != null) {
            Log.v(TAG, "setAppWindowsGone(): packageName=" + mPackageName + " remove token: " + token + " from: " + mWindowTokens);
            mWindowTokens.remove(token);
        } else {
            Log.v(TAG, "setAppWindowsGone(): packageName=" + mPackageName + " remove token: " + mLayoutParams.token + " from: " + mWindowTokens);
            mWindowTokens.remove(mLayoutParams.token);
            mLayoutParams.token = null;
        }
        //mIsAppWindowVisible = false;
        hide();
    }

    public void updateConfig(Configuration newConfig) {
        final int rot = newConfig.windowConfiguration.getRotation();
        if (mRotation != rot) {
            Log.d(TAG, "Orientation Changed 1 packageName=" + mPackageName);
            mRotation = rot;
            mLayoutChanged = mShowing;
            hide();
            inflateViews();
        }
    }

    // We block the app view till they are ready(fully drawn).
    public void blockAppView() {
        if (DEBUG) Log.v(TAG, "blockAppView(): packageName=" + mPackageName);
        addView(mBlockView, mLayoutParamsSys);
        mBlockingAppView = true;
        mHandler.postDelayed(() -> {
            removeView(mBlockView);
            mBlockingAppView = false;
        }, 3000);
    }

    // Once the app is fully drawn, we switch to the AppLock view
    public void show(IBinder token) {
        if (token == null) return;
        reloadPasswordSetup();
        Log.v(TAG, "show(): packageName=" + mPackageName + " add token: " + token + " to: " + mWindowTokens);
        mWindowTokens.add(token);
        mLayoutParams.token = token;
        //mIsAppWindowVisible = true;
        addView(mLockView, mLayoutParams);
        if (sTimerRunning) {
            setInputEnabled(false);
        } else {
            setInputEnabled(true);
        }
        mLayoutChanged = false;
    }

    public void hide() {
        if (DEBUG) Log.v(TAG, "hide(): packageName=" + mPackageName);
        removeView(mBlockView);
        removeView(mLockView);
        setInputEnabled(false);
        mShowing = false;
    }

    private void startListeningForFingerprint() {
        if (DEBUG) Log.v(TAG, "startListeningForFingerprint(): packageName=" + mPackageName);
        if (!isFingerprintAuthAvailable()) {
            mFODview.setVisibility(View.GONE);
            return;
        }
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
        }
        mCancellationSignal = new CancellationSignal();
        mFingerprintManager.authenticate(null, mCancellationSignal, 0,
                new FingerPrintAuthenticationCallback(), null);
    }

    private void stopListeningForFingerprint() {
        if (DEBUG) Log.v(TAG, "stopListeningForFingerprint(): packageName=" + mPackageName);
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
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

    private void addView(LinearLayout v, LayoutParams l) {
        mHandler.post(() -> {
            if(!v.isAttachedToWindow()) {
                try {
                    mWindowManager.addView(v, l);
                    mShowing = true;
                    if (DEBUG) Log.v(TAG, "addView(): done packageName=" + mPackageName);
                } catch (RuntimeException e) {
                    Log.e(TAG, "Failed to add View", e);
                } finally {
                    if (!mShowing) {
                        mWindowManager.removeViewImmediate(v);
                    }
                    if (v == mLockView && mBlockingAppView) {
                        removeView(mBlockView);
                        mBlockingAppView = false;
                    }
                }
            }
        });
    }

    private void removeView(LinearLayout v) {
        mHandler.postDelayed(() -> {
            if (v.isAttachedToWindow()) {
                try {
                    mWindowManager.removeView(v);
                    if (DEBUG) Log.d(TAG, "removeView(): Done");
                } catch(IllegalStateException | IllegalArgumentException e) {
                    Log.e(TAG, "Failed to remove View", e);
                }
            }
        }, mBlockingAppView ? 200 : 0);
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

    private void onUnlockSucceed() {
        if (DEBUG) Log.v(TAG, "onUnlockSucceed(): packageName=" + mPackageName);
        if (sTimerRunning) {
            sTimer.cancel();
        }
        clearInput();
        sWrongAttempts = 0;
        sAppLockService.addOpenedApp(mPackageName);
        hide();
    }

    private void startCountdown(long elapsedRealtimeDeadline) {
        if (DEBUG) Log.v(TAG, "startCountdown(): packageName=" + mPackageName);
        if (sTimerRunning) return;
        sAppLockService.notifyTimerStartForAll();
        final long elapsedRealtime = SystemClock.elapsedRealtime();
        sTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                sCountdown = millisUntilFinished;
                sAppLockService.notifyTimerTickForAll();
            }

            @Override
            public void onFinish() {
                sTimerRunning = false;
                sWrongAttempts = 0;
                sTimer = null;
                sAppLockService.notifyTimerStopForAll();
            }
        };
        sTimer.start();
        sTimerRunning = true;
    }

    private void setInputEnabled(boolean enabled) {
        if (DEBUG) Log.v(TAG, "setInputEnabled(" + String.valueOf(enabled) +
                "): packageName=" + mPackageName);
        mHandler.post(() -> {
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
                startListeningForFingerprint();
                if (!isFingerprintAuthAvailable()) {
                    mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                clearInput();
                notifyTimerTick();
                mPasswordEntry.clearFocus();
                mLockPatternView.disableInput();
                stopListeningForFingerprint();
            }
        });
    }

    private void clearInput() {
        mHandler.post(() -> {
            mPasswordEntry.setText("");
            mLockPatternView.clearPattern();
        });
    }

    private void setTextColor(int color) {
        mPasswordEntry.setTextColor(color);
        mPasswordEntry.setBackgroundTintList(ColorStateList.valueOf(color));
        mAppName.setTextColor(color);
        mErrorText.setTextColor(color);
        mLockPatternView.setRegularColor(color);

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

    private class FingerPrintAuthenticationCallback extends AuthenticationCallback {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            Log.i(TAG, "onAuthenticationError");
            Log.i(TAG, "Error: Id=" + errMsgId + " Name=" + errString);
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            Log.i(TAG, "onAuthenticationHelp");
            Log.i(TAG, "Help: Id=" + helpMsgId + " Name=" + helpString);
        }

        @Override
        public void onAuthenticationFailed() {
            Log.i(TAG, "onAuthenticationFailed");
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            Log.i(TAG, "onAuthenticationSucceeded");
            onUnlockSucceed();
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
}
