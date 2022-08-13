package android.os;

import android.annotation.SuppressLint;
import android.util.Log;

@SuppressLint({"NotCloseable"})
/* loaded from: /tmp/jadx-17486720808468492542.dex */
public class HapticPlayer {
    private HapticPlayer() {
    }

    public HapticPlayer(DynamicEffect dynamicEffect) {
        this();
        Log.d("HapticPlayer", "new player");
    }

    public static boolean isAvailable() {
        return false;
    }

    public void start(int i) {
        Log.e("HapticPlayer", "not support Haptic player api, start with loop");
    }

    public void start(int i, int i2, int i3, int i4) {
        Log.e("HapticPlayer", "not support Haptic player api, start with loop & interval & amplitude & freq");
    }

    public void stop() {
        Log.e("HapticPlayer", "not support Haptic player api, stop");
    }
}