/**
 * Copyright (C) 2017 The ParanoidAndroid Project
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

import android.app.ActivityManager;
import android.app.ActivityOptions;
import android.app.admin.DevicePolicyManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.database.ContentObserver;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.InsetDrawable;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.os.AsyncTask;
import android.os.CancellationSignal;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemClock;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Gravity;
import android.view.IWindowManager;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.android.internal.R;
import com.android.internal.policy.PhoneWindow;
import com.android.internal.util.pa.ColorUtils;
import com.android.internal.widget.LockPatternChecker;
import com.android.internal.widget.LockPatternUtils;
import com.android.internal.widget.LockPatternView;
import com.android.internal.widget.TextViewInputDisabler;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class AppLockUI {

    private static final String TAG = AppLockUI.class.getSimpleName();
    private static final boolean DEBUG = false;
    private static final long ERROR_TIMEOUT_MILLIS = 1500;
    private static final int PATTERN_CLEAR_TIMEOUT_MS = 2000;
    private static final int MAX_WRONG_ATTEMPTS = 5;
    private static final int MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT = 3;

    private static AppLockUI sInstance;

    private AtomicBoolean mShowing = new AtomicBoolean(false);
    private boolean mShowOnWakeUpOnly;
    private int mBackgroundColor;
    private String mPackageName;

    private List<String> mOpenedApplicationsIndex;

    // pattern lock
    private LockPatternUtils mLockPatternUtils;
    private AsyncTask<?, ?, ?> mPendingPatternLockCheck;
    private LockPatternView mLockPatternView;

    // pin/password
    private TextView mPasswordEntry;
    private TextView mErrorText;
    private TextViewInputDisabler mPasswordEntryInputDisabler;
    private AsyncTask<?, ?, ?> mPendingLockCheck;
    private CountDownTimer mTimer;
    private boolean mTimerRunning;
    private boolean mIsPassword;
    private boolean mIsPattern;
    private int mWrongAttempts;
    private long mCountdown;

    private CancellationSignal mCancellationSignal;
    private Context mContext;
    private FingerprintManager mFingerprintManager;
    private Handler mHandler;
    private ImageView mFingerprintIcon;
    private IWindowManager mIWindowManager;
    private InputMethodManager mImm;
    private PackageManager mPackageManager;
    private PhoneWindow mDummyWindow;
    private View mAppLockView;
    private WindowManager mWindowManager;
    private WindowManager.LayoutParams mLayoutParams;

    public static AppLockUI getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new AppLockUI(context);
        }
        return sInstance;
    }

    private AppLockUI(Context context) {
        mContext = context;
        mHandler = new Handler(Looper.getMainLooper());

        mPackageManager = mContext.getPackageManager();
        mFingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        mImm = (InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE);
        mIWindowManager = IWindowManager.Stub.asInterface(
                ServiceManager.getService(Context.WINDOW_SERVICE));
        mWindowManager = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);

        mAppLockView = LayoutInflater.from(mContext).inflate(R.layout.app_lock_view, null);
        mFingerprintIcon = (ImageView) mAppLockView.findViewById(R.id.fingerprint_icon);
        mErrorText = (TextView) mAppLockView.findViewById(R.id.password_entry_error);

        // keeping track of open applications
        mOpenedApplicationsIndex = new ArrayList<>();
        SettingsObserver observer = new SettingsObserver(mHandler);
        observer.observe();

        int userId = ActivityManager.getCurrentUser();
        mLockPatternUtils = new LockPatternUtils(mContext);
        mLockPatternView = (LockPatternView) mAppLockView.findViewById(R.id.lockPattern);
        mLockPatternView.setSaveEnabled(false);
        mLockPatternView.setOnPatternListener(new UnlockPatternListener());
        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
        mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(userId));

        int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(userId);
        mIsPassword = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_MANAGED == storedQuality;

        mPasswordEntry = (TextView) mAppLockView.findViewById(R.id.password_entry);
        mPasswordEntry.setOnEditorActionListener(new EditorActionListener());
        mPasswordEntry.setInputType(mIsPassword ? (InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD) : (InputType.TYPE_CLASS_NUMBER
                | InputType.TYPE_NUMBER_VARIATION_PASSWORD));

        mPasswordEntryInputDisabler = new TextViewInputDisabler(mPasswordEntry);
        mPasswordEntry.setTransformationMethod(PasswordTransformationMethod.getInstance());
    }

    private void reloadPasswordSetup() {
        mWrongAttempts = 0;

        int userId = ActivityManager.getCurrentUser();
        int storedQuality = mLockPatternUtils.getKeyguardStoredPasswordQuality(userId);

        mLockPatternView.setTactileFeedbackEnabled(mLockPatternUtils.isTactileFeedbackEnabled());
        mLockPatternView.setInStealthMode(!mLockPatternUtils.isVisiblePatternEnabled(userId));

        mIsPassword = DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_COMPLEX == storedQuality
                || DevicePolicyManager.PASSWORD_QUALITY_MANAGED == storedQuality;

        mPasswordEntry.setInputType(mIsPassword ? InputType.TYPE_CLASS_TEXT
                 | InputType.TYPE_TEXT_VARIATION_PASSWORD : InputType.TYPE_CLASS_NUMBER
                 | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
    }

    private boolean isFingerprintAuthAvailable() {
        return mFingerprintManager.isHardwareDetected()
                && mFingerprintManager.hasEnrolledFingerprints();
    }

    private void stopListeningForFingerprint(boolean hideIcon) {
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
            mCancellationSignal = null;
        }
        if (hideIcon) {
            if (mFingerprintIcon != null) {
                mFingerprintIcon.setVisibility(View.GONE);
            }
        }
    }

    private void startListeningForFingerprint(boolean showIcon) {
        if (!isFingerprintAuthAvailable()) {
            return;
        }
        if (mCancellationSignal != null) {
            mCancellationSignal.cancel();
        }
        mCancellationSignal = new CancellationSignal();
        mFingerprintManager.authenticate(null, mCancellationSignal, 0,
                new FingerPrintAuthenticationCallback(), null);
        if (showIcon) {
            if (mFingerprintIcon != null) {
                mFingerprintIcon.setVisibility(View.VISIBLE);
            }
        }
    }

    public void show(String packageName) {
        if (DEBUG) {
            Log.i(TAG, "show: packageName= " + packageName);
        }
        if (!mShowing.get() && (!mShowOnWakeUpOnly
                || !mOpenedApplicationsIndex.contains(packageName))) {
            // check how long this call takes
            long startTime = System.nanoTime();

            if (!mOpenedApplicationsIndex.contains(packageName)) {
                mOpenedApplicationsIndex.add(mPackageName);
            }

            boolean sameApp = packageName.equals(mPackageName);

            mPackageName = packageName;
            reloadPasswordSetup();
            createLayoutParams();

            int userId = ActivityManager.getCurrentUser();
            switch (mLockPatternUtils.getKeyguardStoredPasswordQuality(userId)) {
                case DevicePolicyManager.PASSWORD_QUALITY_SOMETHING:
                    mLockPatternView.setVisibility(View.VISIBLE);
                    mLockPatternView.post(mCancelPatternRunnable);
                    mLockPatternView.enableInput();
                    mPasswordEntry.setVisibility(View.GONE);
                    mErrorText.setVisibility(View.VISIBLE);
                    mErrorText.setText(mContext.getString(R.string.lockscreen_pattern_instructions));
                    mIsPattern = true;
                    break;
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_NUMERIC_COMPLEX:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHABETIC:
                case DevicePolicyManager.PASSWORD_QUALITY_ALPHANUMERIC:
                case DevicePolicyManager.PASSWORD_QUALITY_COMPLEX:
                case DevicePolicyManager.PASSWORD_QUALITY_MANAGED:
                    mLockPatternView.setVisibility(View.GONE);
                    mPasswordEntry.setVisibility(View.VISIBLE);
                    mErrorText.setVisibility(View.VISIBLE);
                    mErrorText.setText(mContext.getString(R.string.lockscreen_pin_instructions));
                    mIsPattern = false;
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
            startListeningForFingerprint(false);
            Drawable icon = null;
            try {
                if (!sameApp) {
                    icon = mPackageManager.getApplicationIcon(packageName);
                    mBackgroundColor = ColorUtils.getIconColorFromDrawable(icon);
                    if (mBackgroundColor == -3) {
                        mBackgroundColor = mContext.getResources().getColor(R.color.applock_bg);
                    }
                }
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "failed to find package "+packageName, e);
            }

            int white = mContext.getColor(R.color.white);

            mAppLockView.setBackgroundColor(mBackgroundColor);
            mDummyWindow.setStatusBarColor(mBackgroundColor);
            mDummyWindow.setNavigationBarColor(mBackgroundColor);
            mErrorText.setTextColor(white);
            mFingerprintIcon.setColorFilter(white);
            mPasswordEntry.setBackgroundTintList(ColorStateList.valueOf(white));

            if (icon != null) {
                ImageView appIcon = (ImageView) mAppLockView.findViewById(R.id.app_icon);

                int iconHeight = mContext.getResources().getDimensionPixelSize(
                        R.dimen.app_icon_height);
                int iconWidth = mContext.getResources().getDimensionPixelSize(
                        R.dimen.app_icon_width);
                if (icon.getIntrinsicHeight() != iconHeight
                        || icon.getIntrinsicWidth() != iconWidth) {
                    icon = new IntrinsicSizeDrawable(icon, iconWidth, iconHeight);
                }
                appIcon.setImageDrawable(icon);
            }
            addView();
            updateFingerprintDrawable(R.drawable.ic_fingerprint);

            Log.e(TAG, "show: countdown="+mCountdown);

            if (mCountdown > 0) {
                startCountdown(mCountdown);
            }

            long endTime = System.nanoTime();
            if (DEBUG) {
                Log.i(TAG, "calling show() took " + ((endTime - startTime) / 1000000) + "ms");
            }
        }
    }

    private void updateFingerprintDrawable(int iconId) {
        Drawable icon = mContext.getDrawable(iconId);
        final AnimatedVectorDrawable animation = icon instanceof AnimatedVectorDrawable
                ? (AnimatedVectorDrawable) icon : null;
        int iconHeight = mContext.getResources().getDimensionPixelSize(
                R.dimen.affordance_icon_height);
        int iconWidth = mContext.getResources().getDimensionPixelSize(
                R.dimen.affordance_icon_width);
        if ((icon.getIntrinsicHeight() != iconHeight
                || icon.getIntrinsicWidth() != iconWidth)) {
            icon = new IntrinsicSizeDrawable(icon, iconWidth, iconHeight);
        }
        mFingerprintIcon.setImageDrawable(icon);
        mFingerprintIcon.setVisibility(isFingerprintAuthAvailable() ? View.VISIBLE : View.GONE);
        if (animation != null) {
            animation.forceAnimationOnUI();
            animation.start();
        }
    }

    public boolean isShowingForApp(String pkgName) {
        return mShowing.get() && pkgName != null && pkgName.equals(mPackageName);
    }

    public void hide() {
        if (DEBUG) {
            Log.i(TAG, "hide() with mShowing= " + mShowing.get());
        }

        if (mShowing.get()) {
            stopListeningForFingerprint(false);
            resetState();
            removeView();

            try {
                // Force re-orientation because our view has a portrait orientation.
                // Idealy this should be fixed somewhere inside WM, but this will do for now.
                mIWindowManager.updateRotation(true, true);
            } catch (RemoteException e) {
                // System is dead here
            }

            if (mTimerRunning) {
                mTimer.cancel();
            }
        }
    }

    protected void clearApplicationList() {
        mOpenedApplicationsIndex.clear();
    }

    private void createLayoutParams() {
        mLayoutParams = new WindowManager.LayoutParams();
        mLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        mLayoutParams.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT;
        mLayoutParams.windowAnimations = com.android.internal.R.style.Animation_AppLock;

        mDummyWindow = new PhoneWindow(mContext);

        // Force the window flags: this is a fake window, so it is not really
        // touchable or focusable.  We also add in the ALT_FOCUSABLE_IM
        // flag because we do know that the next window will take input
        // focus, so we want to get the IME window up on top of us right away.
        mDummyWindow.setFlags(
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        mDummyWindow.getAttributes().type = WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
        mDummyWindow.getAttributes().windowAnimations =
                com.android.internal.R.style.Animation_AppLock;
    }

    private void resetState() {
        mHandler.post(() -> {
            mPasswordEntry.setEnabled(true);
            mPasswordEntry.setText("");
            mPasswordEntryInputDisabler.setInputEnabled(true);
            if (!isFingerprintAuthAvailable()) {
               mImm.showSoftInput(mPasswordEntry, InputMethodManager.SHOW_IMPLICIT);
            }
        });
    }

    private void handleNext() {
        if (mPendingLockCheck != null) {
            return;
        }

        final String pin = mPasswordEntry.getText().toString();
        if (pin.isEmpty()) {
            return;
        }

        mPasswordEntryInputDisabler.setInputEnabled(false);
        startCheckPassword(pin);
    }

    private void startCheckPassword(final String pin) {
        final int userId = ActivityManager.getCurrentUser();
        mPasswordEntryInputDisabler.setInputEnabled(false);
        if (mPendingLockCheck != null) {
            mPendingLockCheck.cancel(false);
        }

        if (pin != null && pin.length() <= MINIMUM_PASSWORD_LENGTH_BEFORE_REPORT) {
            // to avoid accidental lockout, only count attempts that are long enough to be a
            // real password. This may require some tweaking.
            mPasswordEntryInputDisabler.setInputEnabled(true);
            onPasswordChecked(userId, false /* matched */, 0, false /* not valid - too short */);
            return;
        }

        mPendingLockCheck = LockPatternChecker.checkPassword(
                mLockPatternUtils, pin, userId, new LockPatternChecker.OnCheckCallback() {
                    @Override
                    public void onEarlyMatched() {
                        onPasswordChecked(userId, true /* matched */, 0 /* timeoutMs */,
                                true /* isValidPassword */);
                    }

                    @Override
                    public void onChecked(boolean matched, int timeoutMs) {
                        mPasswordEntryInputDisabler.setInputEnabled(true);
                        mPendingLockCheck = null;
                        if (!matched) {
                            onPasswordChecked(userId, false /* matched */, timeoutMs,
                                    true /* isValidPassword */);
                        }
                    }
                });
    }

    private void startCountdown(long elapsedRealtimeDeadline) {
        if (mTimerRunning) return;

        // always disable fingerprint and hide the icon
        stopListeningForFingerprint(true);

        if (mIsPattern) {
            // disable the pattern
            mLockPatternView.clearPattern();
            mLockPatternView.setEnabled(false);
        } else {
            // password or pin
            mPasswordEntry.setEnabled(false);
            mPasswordEntry.setText("");
            mPasswordEntryInputDisabler.setInputEnabled(false);
        }

        final long elapsedRealtime = SystemClock.elapsedRealtime();
        mTimer = new CountDownTimer(elapsedRealtimeDeadline - elapsedRealtime, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                final int secondsRemaining = (int) (millisUntilFinished / 1000);
                mCountdown = millisUntilFinished;
                String text = mContext.getString(
                        R.string.lockscreen_too_many_failed_attempts_countdown, secondsRemaining);
                mErrorText.setText(text);
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                mWrongAttempts = 0;
                String text = mContext.getString(mIsPattern
                        ? R.string.lockscreen_pattern_instructions
                        : R.string.lockscreen_pin_instructions);
                mErrorText.setText(text);
                if (mIsPattern) {
                    mLockPatternView.setEnabled(true);
                } else {
                    resetState();
                }

                startListeningForFingerprint(true);
            }
        };
        mTimer.start();
        mTimerRunning = true;
    }

    private void onPasswordChecked(int userId, boolean matched, int timeoutMs,
            boolean isValidPassword) {
        mPasswordEntryInputDisabler.setInputEnabled(true);
        if (matched) {
            mLockPatternUtils.sanitizePassword();
            hide();
        } else {
            resetState();
            if (isValidPassword) {
                mWrongAttempts++;
                boolean startCountdown = timeoutMs > 0;
                if (startCountdown) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(userId, timeoutMs);
                    startCountdown(deadline);
                }
            }
            String errorText = timeoutMs == 0
                    ? mContext.getString(R.string.remaining_attempts_pattern,
                    MAX_WRONG_ATTEMPTS - mWrongAttempts) : mContext.getString(mIsPassword
                    ? R.string.passwordIncorrect : R.string.keyguard_password_wrong_pin_code);
            mErrorText.setText(errorText);
        }
    }

    private void onPatternChecked(int userId, boolean matched, int timeoutMs,
            boolean isValidPattern) {
        if (matched) {
            mLockPatternUtils.sanitizePassword();
            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Correct);
            hide();
        } else {
            mLockPatternView.setDisplayMode(LockPatternView.DisplayMode.Wrong);
            if (isValidPattern) {
                mWrongAttempts++;
                boolean startCountdown = timeoutMs > 0;
                if (startCountdown) {
                    long deadline = mLockPatternUtils.setLockoutAttemptDeadline(userId, timeoutMs);
                    startCountdown(deadline);
                }
            }
            String errorText = timeoutMs == 0
                    ? mContext.getString(R.string.remaining_attempts_pattern,
                    MAX_WRONG_ATTEMPTS - mWrongAttempts)
                    : mContext.getString(R.string.kg_wrong_pattern);
            mErrorText.setText(errorText);
            mLockPatternView.removeCallbacks(mCancelPatternRunnable);
            mLockPatternView.postDelayed(mCancelPatternRunnable, PATTERN_CLEAR_TIMEOUT_MS);
        }
    }

    private void addView() {
        mHandler.post(() -> {
            if (!mShowing.get()) {
                View dummyWindow = mDummyWindow.getDecorView();
                if (dummyWindow != null) {
                    mWindowManager.addView(dummyWindow, mDummyWindow.getAttributes());
                }
                mWindowManager.addView(mAppLockView, mLayoutParams);
                mShowing.set(true);
            }
        });
    }

    private void removeView() {
        mHandler.post(() -> {
            if (mShowing.get()) {
                View dummyWindow = mDummyWindow.getDecorView();
                if (dummyWindow != null) {
                    mWindowManager.removeView(dummyWindow);
                }
                mWindowManager.removeView(mAppLockView);
                mShowing.set(false);
            }
        });
    }

    private final Runnable mCancelPatternRunnable = new Runnable() {
        @Override
        public void run() {
            mLockPatternView.clearPattern();
        }
    };

    private final Runnable mResetErrorTextRunnable = new Runnable() {
        @Override
        public void run() {
            updateFingerprintDrawable(R.drawable.ic_fingerprint);
        }
    };

    private class FingerPrintAuthenticationCallback extends AuthenticationCallback {
        @Override
        public void onAuthenticationError(int errMsgId, CharSequence errString) {
            if (DEBUG) {
                Log.i(TAG, "onAuthenticationError");
            }
        }

        @Override
        public void onAuthenticationHelp(int helpMsgId, CharSequence helpString) {
            if (DEBUG) {
                Log.i(TAG, "onAuthenticationHelp");
            }
        }

        @Override
        public void onAuthenticationFailed() {
            if (DEBUG) {
                Log.i(TAG, "onAuthenticationFailed");
            }
            updateFingerprintDrawable(R.drawable.lockscreen_fingerprint_fp_to_error_state_animation);
            mHandler.removeCallbacks(mResetErrorTextRunnable);
            mHandler.postDelayed(mResetErrorTextRunnable, ERROR_TIMEOUT_MILLIS);
        }

        @Override
        public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult result) {
            if (DEBUG) {
                Log.i(TAG, "onAuthenticationSucceeded");
            }
            updateFingerprintDrawable(R.drawable.lockscreen_fingerprint_draw_off_animation);
            hide();
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
            mLockPatternView.disableInput();
            if (mPendingPatternLockCheck != null) {
                mPendingPatternLockCheck.cancel(false);
            }

            final int userId = ActivityManager.getCurrentUser();
            if (pattern.size() < LockPatternUtils.MIN_PATTERN_REGISTER_FAIL) {
                mLockPatternView.enableInput();
                onPatternChecked(userId, false, 0, false /* not valid - too short */);
                return;
            }

            mPendingPatternLockCheck = LockPatternChecker.checkPattern(
                    mLockPatternUtils,
                    pattern,
                    userId,
                    new LockPatternChecker.OnCheckCallback() {

                        @Override
                        public void onEarlyMatched() {
                            onPatternChecked(userId, true /* matched */, 0 /* timeoutMs */,
                                    true /* isValidPattern */);
                        }

                        @Override
                        public void onChecked(boolean matched, int timeoutMs) {
                            mLockPatternView.enableInput();
                            mPendingPatternLockCheck = null;
                            if (!matched) {
                                onPatternChecked(userId, false /* matched */, timeoutMs,
                                        true /* isValidPattern */);
                            }
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
                handleNext();
                return true;
            }
            return false;
        }
    }

    private class SettingsObserver extends ContentObserver {
        private SettingsObserver(Handler handler) {
            super(handler);
        }

        private void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.APP_LOCK_SHOW_ONLY_ON_WAKE), false, this,
                    UserHandle.USER_ALL);

            // read settings for the first time
            updateSettings();
        }

        private void updateSettings() {
            ContentResolver resolver = mContext.getContentResolver();
            mShowOnWakeUpOnly = Settings.System.getInt(resolver,
                    Settings.System.APP_LOCK_SHOW_ONLY_ON_WAKE, 1) == 1;
        }

        @Override
        public void onChange(boolean selfChange) {
            updateSettings();
        }
    }
}
