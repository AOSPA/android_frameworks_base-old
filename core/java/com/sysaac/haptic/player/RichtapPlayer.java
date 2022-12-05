package com.sysaac.haptic.player;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import com.sysaac.haptic.base.PatternHe;
import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.p033b.p034a.InterfaceC3959c;
import com.sysaac.haptic.sync.C4014d;
import com.sysaac.haptic.sync.SyncCallback;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import org.json.JSONObject;

public class RichtapPlayer implements Player {

    /* renamed from: a */
    private static final String f1712a = "RichtapPlayer";

    /* renamed from: c */
    private Vibrator f1714c;

    /* renamed from: d */
    public Context f1715d;

    /* renamed from: e */
    private Class f1716e;

    /* renamed from: f */
    public Handler f1717f;

    /* renamed from: g */
    public HandlerThread f1718g;

    /* renamed from: h */
    public C4014d f1719h;

    /* renamed from: i */
    public SyncCallback f1720i;

    /* renamed from: k */
    public PlayerEventCallback f1722k;

    /* renamed from: l */
    public RepeatExecutor f1723l;

    /* renamed from: b */
    private final boolean f1713b = false;

    /* renamed from: j */
    public CurrentPlayingInfo f1721j = new CurrentPlayingInfo();

    public RichtapPlayer(Context context) {
        this.f1715d = context;
        this.f1714c = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        try {
            Class<?> cls = Class.forName("android.os.RichTapVibrationEffect");
            this.f1716e = cls;
            if (cls == null) {
                this.f1716e = Class.forName("android.os.VibrationEffect");
            }
            m55k();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: c */
    public void m62c(String str, int i, int i2, SyncCallback syncCallback) {
        C4014d c4014d;
        int i3;
        this.f1720i = syncCallback;
        HandlerThread handlerThread = new HandlerThread("Richtap-Sync");
        this.f1718g = handlerThread;
        handlerThread.start();
        RichtapPlayerThread richtapPlayerThread = new RichtapPlayerThread(this, this.f1718g.getLooper(), i, i2);
        this.f1717f = richtapPlayerThread;
        this.f1719h = new C4014d(richtapPlayerThread, str, this.f1721j);
        if (this.f1721j.f1687h != null) {
            if (this.f1721j.f1687h.getCurrentPosition() < 0) {
                this.f1719h.m2b(this.f1721j.f1687h.getCurrentPosition());
                c4014d = this.f1719h;
                i3 = this.f1721j.f1687h.getCurrentPosition();
            } else {
                this.f1719h.m2b(this.f1721j.f1688i);
                c4014d = this.f1719h;
                i3 = this.f1721j.f1688i;
            }
            c4014d.m4a(i3, this.f1721j.f1688i);
        } else if (syncCallback == null && Util.m122a() >= 24) {
            PatternHe.m162a(this.f1715d).mo148b(str, 1, 0, i, i2);
            this.f1717f.sendEmptyMessageDelayed(102, Util.m83j(str));
        } else {
            C4014d c4014d2 = this.f1719h;
            if (syncCallback == null) {
                c4014d2.m5a(0L);
                return;
            }
            c4014d2.m2b(syncCallback.getCurrentPosition());
            this.f1719h.m4a(syncCallback.getCurrentPosition(), 0L);
        }
    }

    /* renamed from: j */
    public static boolean m56j() {
        try {
            Class<?> cls = Class.forName("android.os.RichTapVibrationEffect");
            if (cls == null) {
                cls = Class.forName("android.os.VibrationEffect");
            }
            return 2 != ((Integer) cls.getMethod("checkIfRichTapSupport", new Class[0]).invoke(null, new Object[0])).intValue();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /* renamed from: k */
    private void m55k() {
        try {
            int intValue = ((Integer) this.f1716e.getMethod("checkIfRichTapSupport", new Class[0]).invoke(null, new Object[0])).intValue();
            if (1 == intValue) {
                Util.m108a(true);
            } else {
                int i = (65280 & intValue) >> 8;
                int i2 = (intValue & 255) >> 0;
                Util.m121a(i);
                Util.m104b(i2);
                Util.m108a(false);
                Log.d("RichtapPlayer", "clientCode:" + ((16711680 & intValue) >> 16) + " majorVersion:" + i + " minorVersion:" + i2);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /* renamed from: l */
    private void m54l() {
        HandlerThread handlerThread = this.f1718g;
        if (handlerThread != null) {
            handlerThread.quit();
            this.f1718g = null;
            this.f1717f = null;
            this.f1719h = null;
        }
        RepeatExecutor repeatExecutor = this.f1723l;
        if (repeatExecutor != null) {
            repeatExecutor.m126c();
            this.f1723l = null;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo53a() {
        this.f1721j.m81a();
        m54l();
        PatternHe.m162a(this.f1715d).mo161a();
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo51a(int i, int i2) {
        m54l();
        this.f1721j.m81a();
        try {
            Method method = this.f1716e.getMethod("createExtPreBaked", Integer.TYPE, Integer.TYPE);
            if (Build.VERSION.SDK_INT < 26) {
                return;
            }
            this.f1714c.vibrate((VibrationEffect) method.invoke(null, Integer.valueOf(i), Integer.valueOf(i2)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo50a(int i, int i2, int i3) {
        PatternHe.m162a(this.f1715d).mo159a(i2, i, i3);
        RepeatExecutor repeatExecutor = this.f1723l;
        if (repeatExecutor != null) {
            repeatExecutor.m131a(i2, i, i3);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo49a(PlayerEventCallback playerEventCallback) {
        this.f1722k = playerEventCallback;
    }

    /* JADX WARN: Removed duplicated region for block: B:26:0x0050  */
    /* JADX WARN: Removed duplicated region for block: B:30:0x0064  */
    /* JADX WARN: Removed duplicated region for block: B:49:0x006e A[EXC_TOP_SPLITTER, SYNTHETIC] */
    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public void mo43a(File file, int i, int i2, int i3, int i4) {
        BufferedReader bufferedReader;
        Exception e;
        String str = null;
        if (!Util.m112a(file.getPath(), ".he")) {
            throw new IllegalArgumentException("Wrong parameter {patternFile: " + file.getPath() + "} doesn't exist or has wrong file format!");
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader2 = null;
        try {
            try {
                bufferedReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                while (true) {
                    try {
                        try {
                            String readLine = bufferedReader.readLine();
                            if (readLine == null) {
                                break;
                            }
                            sb.append(readLine);
                        } catch (Exception e2) {
                            e = e2;
                            e.printStackTrace();
                            if (bufferedReader != null) {
                                bufferedReader.close();
                            }
                            if (Util.m99b(file.getPath(), ".he")) {
                            }
                            Log.e("RichtapPlayer", str);
                        }
                    } catch (Throwable th) {
                        th = th;
                        bufferedReader2 = bufferedReader;
                        if (bufferedReader2 != null) {
                            try {
                                bufferedReader2.close();
                            } catch (Exception e3) {
                                e3.printStackTrace();
                            }
                        }
                        throw th;
                    }
                }
                bufferedReader.close();
            } catch (Exception e4) {
                e4.printStackTrace();
            }
        } catch (Exception ignored) {
        } catch (Throwable th2) {
            if (bufferedReader2 != null) {
            }
        }
        if (Util.m99b(file.getPath(), ".he")) {
            str = "Wrong HE file extention!";
        } else if (this.f1715d != null) {
            mo40a(sb.toString(), i, i2, i3, i4);
            return;
        } else {
            str = "mContext is null!";
        }
        Log.e("RichtapPlayer", str);
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo42a(File file, int i, int i2, SyncCallback syncCallback) {
        this.f1721j.m81a();
        this.f1721j.f1691l = file;
        this.f1721j.f1683d = i;
        this.f1721j.f1684e = i2;
        this.f1721j.f1687h = syncCallback;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo40a(String str, int i, int i2, int i3, int i4) {
        RepeatExecutor repeatExecutor;
        int m84i;
        AbstractC3983q c4002j;
        try {
            m54l();
            if (Util.m96c()) {
                return;
            }
            int i5 = new JSONObject(str).getJSONObject("Metadata").getInt("Version");
            int m122a = Util.m122a();
            if (i5 == 1) {
                PatternHe.m162a(this.f1715d).mo155a(str, i, i2, i3, i4);
            } else if (i5 != 2) {
                Log.e("RichtapPlayer", "unsupport he version heVersion:" + i5);
            } else {
                if (m122a == 22) {
                    String m85h = Util.m85h(Util.m90d(str));
                    RepeatExecutor repeatExecutor2 = new RepeatExecutor();
                    this.f1723l = repeatExecutor2;
                    repeatExecutor2.m131a(i2, i3, i4);
                    repeatExecutor = this.f1723l;
                    m84i = Util.m84i(m85h);
                    c4002j = new C4000h(this, m85h, i3, i4);
                } else if (m122a == 23) {
                    String m85h2 = Util.m85h(Util.m90d(str));
                    RepeatExecutor repeatExecutor3 = new RepeatExecutor();
                    this.f1723l = repeatExecutor3;
                    repeatExecutor3.m131a(i2, i3, i4);
                    repeatExecutor = this.f1723l;
                    m84i = Util.m84i(m85h2);
                    c4002j = new C4001i(this, m85h2, i3, i4);
                } else if (m122a < 24) {
                    return;
                } else {
                    RepeatExecutor repeatExecutor4 = new RepeatExecutor();
                    this.f1723l = repeatExecutor4;
                    repeatExecutor4.m131a(i2, i3, i4);
                    repeatExecutor = this.f1723l;
                    m84i = Util.m84i(str);
                    c4002j = new C4002j(this, str, i3, i4);
                }
                repeatExecutor.m130a(i, i2, m84i, c4002j);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo39a(String str, int i, int i2, SyncCallback syncCallback) {
        m54l();
        if (24 > Util.m122a() && 2 == Util.m88e(str)) {
            str = Util.m85h(Util.m90d(str));
        }
        m62c(str, i, i2, syncCallback);
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo38a(boolean z) {
        CurrentPlayingInfo currentPlayingInfo;
        int i;
        if (z) {
            currentPlayingInfo = this.f1721j;
            i = Integer.MAX_VALUE;
        } else {
            currentPlayingInfo = this.f1721j;
            i = 0;
        }
        currentPlayingInfo.f1682c = i;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo37a(int[] iArr, int[] iArr2, int[] iArr3, boolean z, int i) {
        m54l();
        try {
            this.f1721j.m81a();
            Method method = this.f1716e.getMethod("createEnvelope", int[].class, int[].class, int[].class, Boolean.TYPE, Integer.TYPE);
            if (Build.VERSION.SDK_INT < 26) {
                return;
            }
            this.f1714c.vibrate((VibrationEffect) method.invoke(null, iArr, iArr2, iArr3, Boolean.valueOf(z), Integer.valueOf(i)));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public boolean mo52a(int i) {
        int i2;
        int i3;
        SyncCallback syncCallback;
        if (!CurrentPlayingInfo.m80a(this.f1721j.f1686g)) {
            Log.e("RichtapPlayer", "pause_start_seek seekTo() return - HE invalid or prepare() not be called.");
            return false;
        }
        if (i >= 0 && i <= this.f1721j.f1686g.mo182b()) {
            m54l();
            PatternHe.m162a(this.f1715d).mo161a();
            this.f1721j.f1688i = i;
            String m113a = Util.m113a(this.f1721j.f1680a, this.f1721j.f1688i);
            if (m113a == null || "".equals(m113a)) {
                if (this.f1721j.f1682c > 0) {
                    m113a = this.f1721j.f1680a;
                    this.f1721j.f1688i = 0;
                } else {
                    this.f1721j.f1690k = 9;
                    PlayerEventCallback playerEventCallback = this.f1722k;
                    if (playerEventCallback != null) {
                        playerEventCallback.onPlayerStateChanged(9);
                    }
                }
            }
            if (6 != this.f1721j.f1690k) {
                return true;
            }
            this.f1721j.f1681b = SystemClock.elapsedRealtime();
            if (this.f1721j.f1687h != null) {
                m113a = this.f1721j.f1680a;
                i2 = this.f1721j.f1683d;
                i3 = this.f1721j.f1684e;
                syncCallback = this.f1721j.f1687h;
            } else {
                i2 = this.f1721j.f1683d;
                i3 = this.f1721j.f1684e;
                syncCallback = null;
            }
            m62c(m113a, i2, i3, syncCallback);
            return true;
        }
        return false;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo36b() {
        this.f1721j.m81a();
        m54l();
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo34b(int i, int i2) {
        m54l();
        this.f1721j.m81a();
        Context context = this.f1715d;
        if (context != null) {
            PatternHe.m162a(context).mo152b(i, i2);
        } else {
            Log.e("RichtapPlayer", "mContext is null!");
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo32b(String str, int i, int i2, SyncCallback syncCallback) {
        this.f1721j.m81a();
        this.f1721j.f1680a = str;
        this.f1721j.f1683d = i;
        this.f1721j.f1684e = i2;
        this.f1721j.f1687h = syncCallback;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: c */
    public void mo31c() {
        if (6 != this.f1721j.f1690k) {
            return;
        }
        m54l();
        PatternHe.m162a(this.f1715d).mo161a();
        this.f1721j.f1690k = 7;
        if (!CurrentPlayingInfo.m80a(this.f1721j.f1686g)) {
            Log.e("RichtapPlayer", "pause_start_seek pause() return - HE invalid or prepare() not be called");
            this.f1721j.f1688i = 0;
        } else if (this.f1721j.f1687h != null) {
            CurrentPlayingInfo currentPlayingInfo = this.f1721j;
            currentPlayingInfo.f1688i = currentPlayingInfo.f1687h.getCurrentPosition();
        } else {
            int elapsedRealtime = (int) (SystemClock.elapsedRealtime() - this.f1721j.f1681b);
            if (elapsedRealtime < 0) {
                this.f1721j.f1688i = 0;
                return;
            }
            this.f1721j.f1688i += elapsedRealtime;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: d */
    public boolean mo27d() {
        int i;
        int i2;
        SyncCallback syncCallback;
        if (6 == this.f1721j.f1690k) {
            return false;
        }
        if (!CurrentPlayingInfo.m80a(this.f1721j.f1686g)) {
            Log.e("RichtapPlayer", "pause_start_seek start() return - HE invalid or prepare() not called.");
            return false;
        } else if (this.f1721j.f1688i < 0) {
            return false;
        } else {
            if (9 == this.f1721j.f1690k) {
                this.f1721j.f1688i = 0;
            }
            String m113a = Util.m113a(this.f1721j.f1680a, this.f1721j.f1688i);
            if (m113a == null || "".equals(m113a)) {
                this.f1721j.f1690k = 9;
                PlayerEventCallback playerEventCallback = this.f1722k;
                if (playerEventCallback != null) {
                    playerEventCallback.onPlayerStateChanged(9);
                }
                return false;
            }
            this.f1721j.f1681b = SystemClock.elapsedRealtime();
            this.f1721j.f1690k = 6;
            if (this.f1721j.f1687h != null) {
                m113a = this.f1721j.f1680a;
                i = this.f1721j.f1683d;
                i2 = this.f1721j.f1684e;
                syncCallback = this.f1721j.f1687h;
            } else {
                i = this.f1721j.f1683d;
                i2 = this.f1721j.f1684e;
                syncCallback = null;
            }
            m62c(m113a, i, i2, syncCallback);
            return true;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: e */
    public boolean mo24e() {
        if (this.f1721j.f1691l != null) {
            CurrentPlayingInfo currentPlayingInfo = this.f1721j;
            currentPlayingInfo.f1680a = Util.m102b(currentPlayingInfo.f1691l);
        }
        if (1 == Util.m88e(this.f1721j.f1680a)) {
            CurrentPlayingInfo currentPlayingInfo2 = this.f1721j;
            currentPlayingInfo2.f1680a = Util.m101b(currentPlayingInfo2.f1680a);
        }
        if (24 > Util.m122a()) {
            CurrentPlayingInfo currentPlayingInfo3 = this.f1721j;
            currentPlayingInfo3.f1680a = Util.m90d(currentPlayingInfo3.f1680a);
            CurrentPlayingInfo currentPlayingInfo4 = this.f1721j;
            currentPlayingInfo4.f1680a = Util.m85h(currentPlayingInfo4.f1680a);
        }
        InterfaceC3959c m114a = Util.m114a(this.f1721j.f1680a);
        if (CurrentPlayingInfo.m80a(m114a)) {
            this.f1721j.f1686g = m114a;
            return true;
        }
        Log.e("RichtapPlayer", "prepare error, invalid HE");
        this.f1721j.m81a();
        return false;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: f */
    public int mo22f() {
        if (this.f1721j.f1687h != null) {
            return this.f1721j.f1687h.getCurrentPosition();
        }
        switch (this.f1721j.f1690k) {
            case 6:
                return (int) ((SystemClock.elapsedRealtime() - this.f1721j.f1681b) + this.f1721j.f1688i);
            case 7:
                return this.f1721j.f1688i;
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
        if (this.f1721j.f1686g == null) {
            return 0;
        }
        return this.f1721j.f1686g.mo182b();
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: h */
    public boolean mo18h() {
        return 6 == this.f1721j.f1690k;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: i */
    public void mo17i() {
        this.f1722k = null;
    }
}
