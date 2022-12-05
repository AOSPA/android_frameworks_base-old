package com.sysaac.haptic;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import com.sysaac.haptic.base.ApiInfo;
import com.sysaac.haptic.base.PreBakedEffect;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.player.GooglePlayer;
import com.sysaac.haptic.player.Player;
import com.sysaac.haptic.player.RichtapPlayer;
import com.sysaac.haptic.player.TencentPlayer;
import com.sysaac.haptic.sync.SyncCallback;
import java.io.File;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AACHapticUtils extends ApiInfo {
    private static final boolean DEBUG = false;
    private static final int MAX_SCALE = 100;
    private static final int MAX_STRENGTH_VALUE = 255;
    private static final String TAG = "AACHapticUtils";
    private Context mContext;
    public Player mPlayer;
    private Vibrator mVibrator;
    private static AACHapticUtils sInstance = null;
    private static final ExecutorService mExcutor = Executors.newSingleThreadExecutor();

    private AACHapticUtils() {
    }

    public static int getDuration(File file) {
        return Util.m83j(Util.m102b(file));
    }

    public static int getDuration(String str) {
        return Util.m83j(str);
    }

    public static AACHapticUtils getInstance() {
        if (sInstance == null) {
            synchronized (AACHapticUtils.class) {
                if (sInstance == null) {
                    sInstance = new AACHapticUtils();
                }
            }
        }
        return sInstance;
    }

    private Player getPlayer(Context context) {
        String str;
        if (this.mVibrator == null) {
            str = "Please call the init method first";
        } else if (Build.VERSION.SDK_INT >= 26) {
            this.mPlayer = RichtapPlayer.m56j() ? new RichtapPlayer(this.mContext) : TencentPlayer.m16j() ? new TencentPlayer(this.mContext) : new GooglePlayer(this.mContext);
            return this.mPlayer;
        } else {
            str = "OS is lower than Android O!";
        }
        Log.e("AACHapticUtils", str);
        return null;
    }

    public static String getPreBakedEffectNameById(int i) {
        return PreBakedEffect.m135a(i);
    }

    public AACHapticUtils init(Context context) {
        if (context != null) {
            Log.i("AACHapticUtils", "init ,version:" + VERSION_NAME + " versionCode:" + VERSION_CODE);
            this.mVibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            this.mContext = context;
            useNonRichTap(false);
            return sInstance;
        }
        throw new IllegalArgumentException("context shouldn't be null");
    }

    public boolean isLRSwapped() {
        Context context = this.mContext;
        if (context == null) {
            Log.e("AACHapticUtils", "init() not called.");
            return false;
        }
        try {
            return Util.m118a(context);
        } catch (Throwable th) {
            th.printStackTrace();
            return false;
        }
    }

    @Deprecated
    public boolean isNonRichTapMode() {
        return this.mPlayer instanceof GooglePlayer;
    }

    public boolean isSupportedRichTap() {
        return RichtapPlayer.m56j() || TencentPlayer.m16j();
    }

    public void playEnvelope(int[] iArr, float[] fArr, int[] iArr2, boolean z) {
        playEnvelope(iArr, fArr, iArr2, z, 255);
    }

    public void playEnvelope(int[] iArr, float[] fArr, int[] iArr2, boolean z, int i) {
        if (this.mVibrator == null) {
            Log.e("AACHapticUtils", "Please call the init method");
            return;
        }
        for (int i2 = 0; i2 < 4; i2++) {
            if (iArr[i2] < 0) {
                throw new IllegalArgumentException("relative time can not be negative");
            }
            if (fArr[i2] < 0.0f) {
                throw new IllegalArgumentException("scale can not be negative");
            }
            if (iArr2[i2] < 0) {
                throw new IllegalArgumentException("freq must be positive");
            }
        }
        if (i < -1 || i == 0 || i > 255) {
            throw new IllegalArgumentException("amplitude must either be DEFAULT_AMPLITUDE, or between 1 and 255 inclusive (amplitude=" + i + ")");
        }
        int[] copyOfRange = Arrays.copyOfRange(iArr, 0, 4);
        int[] iArr3 = new int[fArr.length];
        for (int i3 = 0; i3 < fArr.length; i3++) {
            iArr3[i3] = (int) (fArr[i3] * 100.0f);
        }
        int[] copyOfRange2 = Arrays.copyOfRange(iArr2, 0, 4);
        if (Build.VERSION.SDK_INT >= 26) {
            mExcutor.execute(new RunnableC3986c(this, copyOfRange, iArr3, copyOfRange2, z, i));
        } else {
            Log.e("AACHapticUtils", "The system is low than 26,does not support richTap!!");
        }
    }

    public void playExtPreBaked(int i, int i2) {
        if (this.mVibrator == null) {
            Log.e("AACHapticUtils", "Please call the init method");
        } else if (i < 0) {
            throw new IllegalArgumentException("Wrong parameter effect is null!");
        } else {
            if (i2 < 1 || i2 > 100) {
                throw new IllegalArgumentException("Wrong parameter {strength=" + i2 + "}, which should be between 1 and 100 included!");
            } else if (Build.VERSION.SDK_INT >= 26) {
                mExcutor.execute(new RunnableC3954a(this, i, i2));
            } else {
                Log.e("AACHapticUtils", "OS is low than 26, which does not support richTap!");
            }
        }
    }

    public void playExtPreBakedForHe(int i, int i2) {
        if (i < 0 || i > 100) {
            throw new IllegalArgumentException("Wrong parameter {intensity:" + i + "}, which should be between [1, 100]!");
        } else if (i2 < 0 || i2 > 100) {
            throw new IllegalArgumentException("Wrong parameter {freq:" + i2 + "}, which should be between [1, 100]!");
        } else if (Build.VERSION.SDK_INT >= 26) {
            mExcutor.execute(new RunnableC3956b(this, i, i2));
        } else {
            Log.e("AACHapticUtils", "OS is low than 26, which does not support richTap!");
        }
    }

    public void playOneShot(long j, int i) {
        if (this.mVibrator == null || 0 == j) {
            Log.e("AACHapticUtils", "playOneShot,mVibrator == null or 0 == milliseconds");
            return;
        }
        Player player = this.mPlayer;
        if (player != null) {
            player.mo53a();
        } else {
            Log.w("AACHapticUtils", "mPlayer == null");
        }
        if (Build.VERSION.SDK_INT >= 26) {
            this.mVibrator.vibrate(VibrationEffect.createOneShot(j, i));
        } else {
            this.mVibrator.vibrate(j);
        }
    }

    public void playPattern(File file, int i) {
        playPattern(file, i, 0, 255, 0);
    }

    public void playPattern(File file, int i, int i2) {
        playPattern(file, i, 0, i2, 0);
    }

    public void playPattern(File file, int i, int i2, int i3, int i4) {
        if (this.mVibrator == null) {
            Log.e("AACHapticUtils", "Please call the init method");
        } else if (i < 1) {
            throw new IllegalArgumentException("Wrong parameter {loop: " + i + "} less than 1!");
        } else if (Util.m112a(file.getPath(), ".he")) {
            if (Util.m99b(file.getPath(), ".he")) {
                mExcutor.execute(new RunnableC3987d(this, file, i, i2, i3, i4));
            } else {
                Log.e("AACHapticUtils", "Input file is not he format!!");
            }
        } else {
            throw new IllegalArgumentException("Wrong parameter {patternFile: " + file.getPath() + "} doesn't exist or has wrong file format!");
        }
    }

    public void playPattern(String str, int i) {
        playPattern(str, i, 255);
    }

    public void playPattern(String str, int i, int i2) {
        playPattern(str, i, 0, i2, 0);
    }

    public void playPattern(String str, int i, int i2, int i3, int i4) {
        if (str == null || str.isEmpty()) {
            throw new IllegalArgumentException("Wrong parameter {string: " + str + "} is null!");
        } else if (i < 1) {
            throw new IllegalArgumentException("Wrong parameter {loop: " + i + "} less than 1!");
        } else if (i2 >= 0 && i2 <= 1000) {
            mExcutor.execute(new RunnableC3988e(this, str, i, i2, i3, i4));
        } else {
            throw new IllegalArgumentException("Wrong parameter {interval: " + i2 + "} less than 0, or greater than 1000!");
        }
    }

    public void playPattern(String str, int i, int i2, SyncCallback syncCallback) {
        if (str != null && !str.isEmpty()) {
            mExcutor.execute(new RunnableC3989f(this, str, i, i2, syncCallback));
            return;
        }
        throw new IllegalArgumentException("Wrong parameter {string: " + str + "} is null!");
    }

    public void playWaveform(long[] jArr, int i) {
        if (this.mVibrator == null) {
            Log.e("AACHapticUtils", "Please call the init method");
            return;
        }
        Player player = this.mPlayer;
        if (player != null) {
            player.mo53a();
        } else {
            Log.w("AACHapticUtils", "mPlayer == null");
        }
        if (Build.VERSION.SDK_INT >= 26) {
            this.mVibrator.vibrate(VibrationEffect.createWaveform(jArr, i));
        } else {
            this.mVibrator.vibrate(jArr, i);
        }
    }

    public void playWaveform(long[] jArr, int[] iArr, int i) {
        if (this.mVibrator == null) {
            Log.e("AACHapticUtils", "Please call the init method");
            return;
        }
        Player player = this.mPlayer;
        if (player != null) {
            player.mo53a();
        } else {
            Log.w("AACHapticUtils", "mPlayer == null");
        }
        if (Build.VERSION.SDK_INT >= 26) {
            this.mVibrator.vibrate(VibrationEffect.createWaveform(jArr, iArr, i));
        } else {
            this.mVibrator.vibrate(jArr, i);
        }
    }

    public void quit() {
        Vibrator vibrator = this.mVibrator;
        if (vibrator != null) {
            vibrator.cancel();
        }
        Player player = this.mPlayer;
        if (player != null) {
            player.mo53a();
            this.mPlayer.mo36b();
        }
        sInstance = null;
        this.mContext = null;
    }

    @Deprecated
    public void selectPlayer(int i) {
        Player c3994b;
        if (i == 2) {
            c3994b = new RichtapPlayer(this.mContext);
        } else if (i == 1) {
            c3994b = new TencentPlayer(this.mContext);
        } else if (i != 0) {
            return;
        } else {
            c3994b = new GooglePlayer(this.mContext);
        }
        this.mPlayer = c3994b;
    }

    public void sendLoopParameter(int i, int i2) {
        if (i < 1 || i > 255) {
            throw new IllegalArgumentException("Wrong parameter {amplitude: " + i + "}, which should be [1, 255]!");
        } else if (i2 >= 0 && i2 <= 1000) {
            mExcutor.execute(new RunnableC3990g(this, i, i2));
        } else {
            throw new IllegalArgumentException("Wrong parameter {interval: " + i2 + "}, which should be [0, 1000]!");
        }
    }

    public void sendLoopParameter(int i, int i2, int i3) {
        if (i < 1 || i > 255) {
            throw new IllegalArgumentException("Wrong parameter {amplitude: " + i + "}, which should be [1, 255]!");
        } else if (i2 >= 0 && i2 <= 1000) {
            mExcutor.execute(new RunnableC3991h(this, i, i2, i3));
        } else {
            throw new IllegalArgumentException("Wrong parameter {interval: " + i2 + "}, which should be [0, 1000]!");
        }
    }

    public void stop() {
        Player player = this.mPlayer;
        if (player != null) {
            player.mo53a();
        }
        Vibrator vibrator = this.mVibrator;
        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    public void swapLR(boolean z) {
        if (this.mContext == null) {
            Log.e("AACHapticUtils", "init() not called.");
        }
        try {
            Util.m107a(z, this.mContext);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    public void useNonRichTap(boolean z) {
        this.mPlayer = z ? new GooglePlayer(this.mContext) : getPlayer(this.mContext);
    }
}
