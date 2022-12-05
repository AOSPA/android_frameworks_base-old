package com.sysaac.haptic.player;

import android.content.Context;
import android.os.Build;
import android.os.DynamicEffect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HapticPlayer;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.p033b.p034a.InterfaceC3959c;
import com.sysaac.haptic.sync.C4014d;
import com.sysaac.haptic.sync.SyncCallback;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Arrays;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.player.l */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class TencentPlayer implements Player {

    /* renamed from: b */
    private static final String f1740b = "TencentPlayer";

    /* renamed from: a */
    HapticPlayer f1741a;

    /* renamed from: d */
    private Vibrator f1743d;

    /* renamed from: e */
    public Context f1744e;

    /* renamed from: f */
    public Handler f1745f;

    /* renamed from: g */
    public HandlerThread f1746g;

    /* renamed from: h */
    public C4014d f1747h;

    /* renamed from: i */
    public SyncCallback f1748i;

    /* renamed from: k */
    public PlayerEventCallback f1750k;

    /* renamed from: l */
    public RepeatExecutor f1751l;

    /* renamed from: c */
    private final boolean f1742c = false;

    /* renamed from: j */
    public CurrentPlayingInfo f1749j = new CurrentPlayingInfo();

    public TencentPlayer(Context context) {
        Log.i("TencentPlayer", "TencentPlayer initialized!");
        this.f1744e = context;
        this.f1743d = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
    }

    /* renamed from: a */
    private String m46a(File file) {
        BufferedReader bufferedReader = null;
        if (!Util.m112a(file.getPath(), ".he")) {
            Log.d("TencentPlayer", "Wrong parameter {patternFile: " + file.getPath() + "} doesn't exist or has wrong file format!");
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try {
            try {
                try {
                    BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    while (true) {
                        try {
                            String readLine = bufferedReader2.readLine();
                            if (readLine == null) {
                                break;
                            }
                            sb.append(readLine);
                        } catch (Exception e) {
                            e = e;
                            bufferedReader = bufferedReader2;
                            e.printStackTrace();
                            if (bufferedReader != null) {
                                bufferedReader.close();
                            }
                            return sb.toString();
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader = bufferedReader2;
                            if (bufferedReader != null) {
                                try {
                                    bufferedReader.close();
                                } catch (Exception e2) {
                                    e2.printStackTrace();
                                }
                            }
                            throw th;
                        }
                    }
                    bufferedReader2.close();
                } catch (Throwable ignored) {
                }
            } catch (Exception ignored) {
            }
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        return sb.toString();
    }

    /* renamed from: b */
    private void m35b(int i) {
        String str;
        try {
            if (HapticPlayer.isAvailable()) {
                HapticPlayer hapticPlayer = this.f1741a;
                if (hapticPlayer == null) {
                    Log.e("TencentPlayer", "HapticsPlayer is null");
                    return;
                }
                try {
                    hapticPlayer.updateInterval(i);
                    return;
                } catch (NoSuchMethodError e) {
                    str = "[interval,amplitude,freq],haven't integrate Haptic player 1.1";
                }
            } else {
                str = "The system does not support HapticsPlayer";
            }
            Log.e("TencentPlayer", str);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /* renamed from: c */
    private void m30c(int i) {
        String str;
        try {
            if (HapticPlayer.isAvailable()) {
                HapticPlayer hapticPlayer = this.f1741a;
                if (hapticPlayer == null) {
                    Log.e("TencentPlayer", "HapticsPlayer is null");
                    return;
                }
                try {
                    hapticPlayer.updateAmplitude(i);
                    return;
                } catch (NoSuchMethodError e) {
                    str = "[interval,amplitude,freq],haven't integrate Haptic player 1.1";
                }
            } else {
                str = "The system does not support HapticsPlayer";
            }
            Log.e("TencentPlayer", str);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: c */
    public void m28c(String str, int i, int i2, SyncCallback syncCallback) {
        C4014d c4014d;
        int i3;
        this.f1748i = syncCallback;
        HandlerThread handlerThread = new HandlerThread("Tencent-Sync");
        this.f1746g = handlerThread;
        handlerThread.start();
        TencentPlayerThread tencentPlayerThread = new TencentPlayerThread(this, this.f1746g.getLooper(), i, i2);
        this.f1745f = tencentPlayerThread;
        this.f1747h = new C4014d(tencentPlayerThread, str, this.f1749j);
        if (this.f1749j.f1687h == null) {
            C4014d c4014d2 = this.f1747h;
            if (syncCallback == null) {
                c4014d2.m5a(0L);
                return;
            }
            c4014d2.m2b(syncCallback.getCurrentPosition());
            this.f1747h.m4a(syncCallback.getCurrentPosition(), 0L);
            return;
        }
        if (this.f1749j.f1687h.getCurrentPosition() < 0) {
            this.f1747h.m2b(this.f1749j.f1687h.getCurrentPosition());
            c4014d = this.f1747h;
            i3 = this.f1749j.f1687h.getCurrentPosition();
        } else {
            this.f1747h.m2b(this.f1749j.f1688i);
            c4014d = this.f1747h;
            i3 = this.f1749j.f1688i;
        }
        c4014d.m4a(i3, this.f1749j.f1688i);
    }

    /* renamed from: d */
    private void m26d(int i) {
        String str;
        try {
            if (HapticPlayer.isAvailable()) {
                HapticPlayer hapticPlayer = this.f1741a;
                if (hapticPlayer == null) {
                    Log.e("TencentPlayer", "HapticsPlayer is null");
                    return;
                }
                try {
                    hapticPlayer.updateFrequency(i);
                    return;
                } catch (NoSuchMethodError e) {
                    str = "[interval,amplitude,freq],haven't integrate Haptic player 1.1";
                }
            } else {
                str = "The system does not support HapticsPlayer";
            }
            Log.e("TencentPlayer", str);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /* renamed from: j */
    public static boolean m16j() {
        try {
            return HapticPlayer.isAvailable();
        } catch (Throwable th) {
            th.printStackTrace();
            return false;
        }
    }

    /* renamed from: k */
    private void m15k() {
        HandlerThread handlerThread = this.f1746g;
        if (handlerThread != null) {
            handlerThread.quit();
            this.f1746g = null;
            this.f1745f = null;
            this.f1747h = null;
        }
        RepeatExecutor repeatExecutor = this.f1751l;
        if (repeatExecutor != null) {
            repeatExecutor.m126c();
            this.f1751l = null;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo53a() {
        try {
            this.f1749j.m81a();
            if (this.f1741a == null) {
                Log.e("TencentPlayer", "HapticsPlayer is null");
                return;
            }
            m15k();
            this.f1741a.stop();
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo51a(int i, int i2) {
        int i3 = (i2 * 255) / 100;
        m15k();
        this.f1749j.m81a();
        if (this.f1743d == null) {
            Log.e("TencentPlayer", "Please call the init method");
            return;
        }
        mo53a();
        if (Build.VERSION.SDK_INT >= 26) {
            this.f1743d.vibrate(VibrationEffect.createOneShot(65, Math.max(1, Math.min(i3, 255))));
        } else {
            this.f1743d.vibrate(65);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo50a(int i, int i2, int i3) {
        try {
            if (!HapticPlayer.isAvailable()) {
                Log.e("TencentPlayer", "The system does not support HapticsPlayer");
                return;
            }
            HapticPlayer hapticPlayer = this.f1741a;
            if (hapticPlayer == null) {
                Log.e("TencentPlayer", "HapticsPlayer is null");
                return;
            }
            try {
                hapticPlayer.updateParameter(i2, i, i3);
            } catch (NoSuchMethodError e) {
                Log.e("TencentPlayer", "[interval,amplitude,freq],haven't integrate Haptic player 1.1");
            }
            RepeatExecutor repeatExecutor = this.f1751l;
            if (repeatExecutor == null) {
                return;
            }
            repeatExecutor.m131a(i2, i, i3);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo49a(PlayerEventCallback playerEventCallback) {
        this.f1750k = playerEventCallback;
    }

    /* renamed from: a */
    public void m45a(File file, int i) {
        try {
            m15k();
            if (!HapticPlayer.isAvailable()) {
                Log.e("TencentPlayer", "The system does not support HapticsPlayer");
                return;
            }
            String m46a = m46a(file);
            if (TextUtils.isEmpty(m46a)) {
                Log.e("TencentPlayer", "empty pattern,do nothing");
            } else if (2 != Util.m88e(m46a)) {
                HapticPlayer hapticPlayer = new HapticPlayer(DynamicEffect.create(m46a));
                this.f1741a = hapticPlayer;
                hapticPlayer.start(i);
            } else {
                String m85h = Util.m85h(Util.m90d(m46a));
                RepeatExecutor repeatExecutor = new RepeatExecutor();
                this.f1751l = repeatExecutor;
                repeatExecutor.m131a(0, 255, 0);
                this.f1751l.m130a(i, 0, Util.m84i(m85h), new C4005m(this, m85h));
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /* renamed from: a */
    public void m44a(File file, int i, int i2, int i3) {
        try {
            m15k();
            if (!HapticPlayer.isAvailable()) {
                Log.e("TencentPlayer", "[looper, interval, amplitude],The system does not support HapticsPlayer");
                return;
            }
            String m46a = m46a(file);
            if (TextUtils.isEmpty(m46a)) {
                Log.e("TencentPlayer", "empty pattern,do nothing");
            } else if (2 == Util.m88e(m46a)) {
                String m85h = Util.m85h(Util.m90d(m46a));
                RepeatExecutor repeatExecutor = new RepeatExecutor();
                this.f1751l = repeatExecutor;
                repeatExecutor.m131a(i2, i3, 0);
                this.f1751l.m130a(i, i2, Util.m84i(m85h), new C4006n(this, m85h, i3));
            } else {
                HapticPlayer hapticPlayer = new HapticPlayer(DynamicEffect.create(m46a));
                this.f1741a = hapticPlayer;
                try {
                    hapticPlayer.start(i, i2, i3);
                } catch (NoSuchMethodError e) {
                    Log.w("TencentPlayer", "haven't integrate Haptic player 1.1 !!!!!!! now we use Haptic player 1.0 to start vibrate");
                    this.f1741a.start(i);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo43a(File file, int i, int i2, int i3, int i4) {
        try {
            m15k();
            if (!HapticPlayer.isAvailable()) {
                Log.e("TencentPlayer", "The system does not support HapticsPlayer");
                return;
            }
            String m46a = m46a(file);
            if (TextUtils.isEmpty(m46a)) {
                Log.e("TencentPlayer", "empty pattern,do nothing");
            } else if (2 == Util.m88e(m46a)) {
                String m85h = Util.m85h(Util.m90d(m46a));
                RepeatExecutor repeatExecutor = new RepeatExecutor();
                this.f1751l = repeatExecutor;
                repeatExecutor.m131a(i2, i3, i4);
                this.f1751l.m130a(i, i2, Util.m84i(m85h), new C4007o(this, m85h, i3, i4));
            } else {
                HapticPlayer hapticPlayer = new HapticPlayer(DynamicEffect.create(m46a));
                this.f1741a = hapticPlayer;
                try {
                    hapticPlayer.start(i, i2, i3, i4);
                } catch (NoSuchMethodError e) {
                    Log.w("TencentPlayer", "[file, looper,interval,amplitude,freq],haven't integrate Haptic player 1.1 !!!!!!!now we use Haptic player 1.0 to start vibrate");
                    this.f1741a.start(i);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo42a(File file, int i, int i2, SyncCallback syncCallback) {
        this.f1749j.m81a();
        this.f1749j.f1691l = file;
        this.f1749j.f1683d = i;
        this.f1749j.f1684e = i2;
        this.f1749j.f1687h = syncCallback;
    }

    /* renamed from: a */
    public void m41a(String str, int i) {
        try {
            m15k();
            if (2 == Util.m88e(str)) {
                String m85h = Util.m85h(Util.m90d(str));
                RepeatExecutor repeatExecutor = new RepeatExecutor();
                this.f1751l = repeatExecutor;
                repeatExecutor.m131a(0, 255, 0);
                this.f1751l.m130a(i, 0, Util.m84i(m85h), new C4008p(this, m85h));
            } else if (!HapticPlayer.isAvailable()) {
                Log.e("TencentPlayer", "The system does not support HapticsPlayer");
            } else {
                HapticPlayer hapticPlayer = new HapticPlayer(DynamicEffect.create(str));
                this.f1741a = hapticPlayer;
                hapticPlayer.start(i);
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo40a(String str, int i, int i2, int i3, int i4) {
        try {
            m15k();
            if (2 == Util.m88e(str)) {
                String m85h = Util.m85h(Util.m90d(str));
                RepeatExecutor repeatExecutor = new RepeatExecutor();
                this.f1751l = repeatExecutor;
                repeatExecutor.m131a(i2, i3, i4);
                this.f1751l.m130a(i, i2, Util.m84i(m85h), new C4009q(this, m85h, i3, i4));
            } else if (!HapticPlayer.isAvailable()) {
                Log.e("TencentPlayer", "The system does not support HapticsPlayer");
            } else {
                HapticPlayer hapticPlayer = new HapticPlayer(DynamicEffect.create(str));
                this.f1741a = hapticPlayer;
                try {
                    hapticPlayer.start(i, i2, i3, i4);
                } catch (NoSuchMethodError e) {
                    Log.w("TencentPlayer", "[patternString, looper,interval,amplitude,freq],haven't integrate Haptic player 1.1 !!!!!!!now we use Haptic player 1.0 to start vibrate");
                    this.f1741a.start(i);
                }
            }
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo39a(String str, int i, int i2, SyncCallback syncCallback) {
        m15k();
        if (2 == Util.m88e(str)) {
            str = Util.m85h(Util.m90d(str));
        }
        m28c(str, i, i2, syncCallback);
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo38a(boolean z) {
        CurrentPlayingInfo currentPlayingInfo;
        int i;
        if (z) {
            currentPlayingInfo = this.f1749j;
            i = Integer.MAX_VALUE;
        } else {
            currentPlayingInfo = this.f1749j;
            i = 0;
        }
        currentPlayingInfo.f1682c = i;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo37a(int[] iArr, int[] iArr2, int[] iArr3, boolean z, int i) {
        int i2 = Arrays.copyOfRange(iArr, 0, 4)[3];
        mo53a();
        this.f1749j.m81a();
        int max = (int) ((Math.max(iArr2[1], iArr2[2]) / 100.0f) * (i / 255.0f) * 255.0f);
        if (Build.VERSION.SDK_INT >= 26) {
            this.f1743d.vibrate(VibrationEffect.createOneShot(i2, Math.max(1, Math.min(max, 255))));
        } else {
            this.f1743d.vibrate(i2);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public boolean mo52a(int i) {
        int i2;
        int i3;
        SyncCallback syncCallback;
        if (!CurrentPlayingInfo.m80a(this.f1749j.f1686g)) {
            Log.e("TencentPlayer", "pause_start_seek seekTo() return - HE invalid or prepare() not be called.");
            return false;
        }
        if (i >= 0 && i <= this.f1749j.f1686g.mo182b()) {
            HapticPlayer hapticPlayer = this.f1741a;
            if (hapticPlayer != null) {
                hapticPlayer.stop();
            }
            m15k();
            this.f1749j.f1688i = i;
            String m113a = Util.m113a(this.f1749j.f1680a, this.f1749j.f1688i);
            if (m113a == null || "".equals(m113a)) {
                if (this.f1749j.f1682c > 0) {
                    m113a = this.f1749j.f1680a;
                    this.f1749j.f1688i = 0;
                } else {
                    this.f1749j.f1690k = 9;
                    PlayerEventCallback playerEventCallback = this.f1750k;
                    if (playerEventCallback != null) {
                        playerEventCallback.onPlayerStateChanged(9);
                    }
                }
            }
            if (6 != this.f1749j.f1690k) {
                return true;
            }
            this.f1749j.f1681b = SystemClock.elapsedRealtime();
            if (this.f1749j.f1687h != null) {
                m113a = this.f1749j.f1680a;
                i2 = this.f1749j.f1683d;
                i3 = this.f1749j.f1684e;
                syncCallback = this.f1749j.f1687h;
            } else {
                i2 = this.f1749j.f1683d;
                i3 = this.f1749j.f1684e;
                syncCallback = null;
            }
            m28c(m113a, i2, i3, syncCallback);
            return true;
        }
        return false;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo36b() {
        Log.i("TencentPlayer", "TencentPlayer releaseed!");
        this.f1749j.m81a();
        m15k();
        HapticPlayer hapticPlayer = this.f1741a;
        if (hapticPlayer != null) {
            hapticPlayer.stop();
        }
        this.f1741a = null;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo34b(int i, int i2) {
        this.f1749j.m81a();
        if (this.f1743d == null) {
            Log.e("TencentPlayer", "Please call the init method");
        } else {
            m41a(Util.m120a(i, i2), 1);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo32b(String str, int i, int i2, SyncCallback syncCallback) {
        this.f1749j.m81a();
        this.f1749j.f1680a = str;
        this.f1749j.f1683d = i;
        this.f1749j.f1684e = i2;
        this.f1749j.f1687h = syncCallback;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: c */
    public void mo31c() {
        if (6 != this.f1749j.f1690k) {
            return;
        }
        HapticPlayer hapticPlayer = this.f1741a;
        if (hapticPlayer != null) {
            hapticPlayer.stop();
        }
        m15k();
        this.f1749j.f1690k = 7;
        if (!CurrentPlayingInfo.m80a(this.f1749j.f1686g)) {
            Log.e("TencentPlayer", "pause_start_seek pause() return -  HE invalid or prepare() not be called");
            this.f1749j.f1688i = 0;
        } else if (this.f1749j.f1687h != null) {
            CurrentPlayingInfo currentPlayingInfo = this.f1749j;
            currentPlayingInfo.f1688i = currentPlayingInfo.f1687h.getCurrentPosition();
        } else {
            int elapsedRealtime = (int) (SystemClock.elapsedRealtime() - this.f1749j.f1681b);
            if (elapsedRealtime < 0) {
                this.f1749j.f1688i = 0;
                return;
            }
            this.f1749j.f1688i += elapsedRealtime;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: d */
    public boolean mo27d() {
        int i;
        int i2;
        SyncCallback syncCallback;
        if (6 == this.f1749j.f1690k) {
            return false;
        }
        if (!CurrentPlayingInfo.m80a(this.f1749j.f1686g)) {
            Log.e("TencentPlayer", "pause_start_seek start() return - HE invalid or prepare() not called.");
            return false;
        } else if (this.f1749j.f1688i < 0) {
            return false;
        } else {
            if (9 == this.f1749j.f1690k) {
                this.f1749j.f1688i = 0;
            }
            String m113a = Util.m113a(this.f1749j.f1680a, this.f1749j.f1688i);
            if (m113a == null || "".equals(m113a)) {
                this.f1749j.f1690k = 9;
                PlayerEventCallback playerEventCallback = this.f1750k;
                if (playerEventCallback != null) {
                    playerEventCallback.onPlayerStateChanged(9);
                }
                return false;
            }
            this.f1749j.f1681b = SystemClock.elapsedRealtime();
            this.f1749j.f1690k = 6;
            if (this.f1749j.f1687h != null) {
                m113a = this.f1749j.f1680a;
                i = this.f1749j.f1683d;
                i2 = this.f1749j.f1684e;
                syncCallback = this.f1749j.f1687h;
            } else {
                i = this.f1749j.f1683d;
                i2 = this.f1749j.f1684e;
                syncCallback = null;
            }
            m28c(m113a, i, i2, syncCallback);
            return true;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: e */
    public boolean mo24e() {
        if (this.f1749j.f1691l != null) {
            CurrentPlayingInfo currentPlayingInfo = this.f1749j;
            currentPlayingInfo.f1680a = Util.m102b(currentPlayingInfo.f1691l);
        }
        if (1 == Util.m88e(this.f1749j.f1680a)) {
            CurrentPlayingInfo currentPlayingInfo2 = this.f1749j;
            currentPlayingInfo2.f1680a = Util.m101b(currentPlayingInfo2.f1680a);
        }
        CurrentPlayingInfo currentPlayingInfo3 = this.f1749j;
        currentPlayingInfo3.f1680a = Util.m90d(currentPlayingInfo3.f1680a);
        CurrentPlayingInfo currentPlayingInfo4 = this.f1749j;
        currentPlayingInfo4.f1680a = Util.m85h(currentPlayingInfo4.f1680a);
        InterfaceC3959c m114a = Util.m114a(this.f1749j.f1680a);
        if (CurrentPlayingInfo.m80a(m114a)) {
            this.f1749j.f1686g = m114a;
            return true;
        }
        Log.e("TencentPlayer", "prepare error, invalid HE");
        this.f1749j.m81a();
        return false;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: f */
    public int mo22f() {
        if (this.f1749j.f1687h != null) {
            return this.f1749j.f1687h.getCurrentPosition();
        }
        switch (this.f1749j.f1690k) {
            case 6:
                return (int) ((SystemClock.elapsedRealtime() - this.f1749j.f1681b) + this.f1749j.f1688i);
            case 7:
                return this.f1749j.f1688i;
            case 8:
            default:
                return 0;
            case 9:
                return mo20g();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: g */
    public int mo20g() {
        if (this.f1749j.f1686g == null) {
            return 0;
        }
        return this.f1749j.f1686g.mo182b();
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: h */
    public boolean mo18h() {
        return 6 == this.f1749j.f1690k;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: i */
    public void mo17i() {
        this.f1750k = null;
    }
}
