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
import com.sysaac.haptic.base.NonRichTapLooperInfo;
import com.sysaac.haptic.base.NonRichTapThread;
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
import org.json.JSONObject;

public class GooglePlayer implements Player {

    /* renamed from: a */
    private static final String f1692a = "GooglePlayer";

    /* renamed from: c */
    private Vibrator f1694c;

    /* renamed from: d */
    private Context f1695d;

    /* renamed from: e */
    private NonRichTapThread f1696e;

    /* renamed from: f */
    public Handler f1697f;

    /* renamed from: g */
    public HandlerThread f1698g;

    /* renamed from: h */
    public C4014d f1699h;

    /* renamed from: i */
    public SyncCallback f1700i;

    /* renamed from: k */
    public PlayerEventCallback f1702k;

    /* renamed from: l */
    public RepeatExecutor f1703l;

    /* renamed from: b */
    private final boolean f1693b = false;

    /* renamed from: j */
    public CurrentPlayingInfo f1701j = new CurrentPlayingInfo();

    public GooglePlayer(Context context) {
        this.f1695d = context;
        this.f1694c = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        NonRichTapThread nonRichTapThread = new NonRichTapThread(this.f1695d);
        this.f1696e = nonRichTapThread;
        nonRichTapThread.start();
    }

    /* JADX INFO: Access modifiers changed from: private */
    /* renamed from: c */
    public void m74c(String str, int i, int i2, SyncCallback syncCallback) {
        C4014d c4014d;
        int i3;
        this.f1700i = syncCallback;
        HandlerThread handlerThread = new HandlerThread("Richtap-Sync");
        this.f1698g = handlerThread;
        handlerThread.start();
        GooglePlayerThread googlePlayerThread = new GooglePlayerThread(this, this.f1698g.getLooper(), i, i2);
        this.f1697f = googlePlayerThread;
        this.f1699h = new C4014d(googlePlayerThread, str, this.f1701j);
        if (this.f1701j.f1687h == null) {
            C4014d c4014d2 = this.f1699h;
            if (syncCallback == null) {
                c4014d2.m5a(0L);
                return;
            }
            c4014d2.m2b(syncCallback.getCurrentPosition());
            this.f1699h.m4a(syncCallback.getCurrentPosition(), 0L);
            return;
        }
        if (this.f1701j.f1687h.getCurrentPosition() < 0) {
            this.f1699h.m2b(this.f1701j.f1687h.getCurrentPosition());
            c4014d = this.f1699h;
            i3 = this.f1701j.f1687h.getCurrentPosition();
        } else {
            this.f1699h.m2b(this.f1701j.f1688i);
            c4014d = this.f1699h;
            i3 = this.f1701j.f1688i;
        }
        c4014d.m4a(i3, this.f1701j.f1688i);
    }

    /* renamed from: j */
    private void m69j() {
        NonRichTapThread nonRichTapThread = this.f1696e;
        if (nonRichTapThread != null) {
            nonRichTapThread.m167b();
        }
        this.f1694c.cancel();
    }

    /* renamed from: k */
    private void m68k() {
        HandlerThread handlerThread = this.f1698g;
        if (handlerThread != null) {
            handlerThread.quit();
            this.f1698g = null;
            this.f1697f = null;
            this.f1699h = null;
        }
        RepeatExecutor repeatExecutor = this.f1703l;
        if (repeatExecutor != null) {
            repeatExecutor.m126c();
            this.f1703l = null;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo53a() {
        this.f1701j.m81a();
        m68k();
        m69j();
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo51a(int i, int i2) {
        int i3 = (i2 * 255) / 100;
        this.f1701j.m81a();
        if (this.f1694c == null) {
            Log.e("GooglePlayer", "Please call the init method");
            return;
        }
        mo53a();
        if (Build.VERSION.SDK_INT >= 26) {
            this.f1694c.vibrate(VibrationEffect.createOneShot(65, Math.max(1, Math.min(i3, 255))));
        } else {
            this.f1694c.vibrate(65);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo50a(int i, int i2, int i3) {
        RepeatExecutor repeatExecutor = this.f1703l;
        if (repeatExecutor != null) {
            repeatExecutor.m131a(i2, i, i3);
        }
        NonRichTapLooperInfo nonRichTapLooperInfo = new NonRichTapLooperInfo(null, -1, i2, i, i3);
        NonRichTapThread nonRichTapThread = this.f1696e;
        if (nonRichTapThread != null) {
            nonRichTapThread.m166b(nonRichTapLooperInfo);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo49a(PlayerEventCallback playerEventCallback) {
        this.f1702k = playerEventCallback;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo43a(File file, int i, int i2, int i3, int i4) {
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = null;
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
                            mo40a(sb.toString(), i, i2, i3, i4);
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
                } catch (Exception ignored) {
                }
            } catch (Throwable ignored) {
            }
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        mo40a(sb.toString(), i, i2, i3, i4);
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo42a(File file, int i, int i2, SyncCallback syncCallback) {
        this.f1701j.m81a();
        this.f1701j.f1691l = file;
        this.f1701j.f1683d = i;
        this.f1701j.f1684e = i2;
        this.f1701j.f1687h = syncCallback;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo40a(String str, int i, int i2, int i3, int i4) {
        try {
            int i5 = new JSONObject(str).getJSONObject("Metadata").getInt("Version");
            if (i5 == 1) {
                m69j();
                this.f1696e.m168a(new NonRichTapLooperInfo(str, i, i2, i3, i4));
            } else if (i5 == 2) {
                m68k();
                String m85h = Util.m85h(Util.m90d(str));
                RepeatExecutor repeatExecutor = new RepeatExecutor();
                this.f1703l = repeatExecutor;
                repeatExecutor.m131a(i2, i3, i4);
                this.f1703l.m130a(i, i2, Util.m84i(m85h), new C3995c(this, m85h, i3, i4));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo39a(String str, int i, int i2, SyncCallback syncCallback) {
        m68k();
        if (2 == Util.m88e(str)) {
            str = Util.m85h(Util.m90d(str));
        }
        m74c(str, i, i2, syncCallback);
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo38a(boolean z) {
        CurrentPlayingInfo currentPlayingInfo;
        int i;
        if (z) {
            currentPlayingInfo = this.f1701j;
            i = Integer.MAX_VALUE;
        } else {
            currentPlayingInfo = this.f1701j;
            i = 0;
        }
        currentPlayingInfo.f1682c = i;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public void mo37a(int[] iArr, int[] iArr2, int[] iArr3, boolean z, int i) {
        int i2 = Arrays.copyOfRange(iArr, 0, 4)[3];
        this.f1701j.m81a();
        if (this.f1694c == null) {
            Log.e("GooglePlayer", "Please call the init method");
            return;
        }
        mo53a();
        int max = (int) ((Math.max(iArr2[1], iArr2[2]) / 100.0f) * (i / 255.0f) * 255.0f);
        if (Build.VERSION.SDK_INT >= 26) {
            this.f1694c.vibrate(VibrationEffect.createOneShot(i2, Math.max(1, Math.min(max, 255))));
        } else {
            this.f1694c.vibrate(i2);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: a */
    public boolean mo52a(int i) {
        int i2;
        int i3;
        SyncCallback syncCallback;
        if (!CurrentPlayingInfo.m80a(this.f1701j.f1686g)) {
            Log.e("GooglePlayer", "pause_start_seek seekTo() return - HE invalid or prepare() not be called.");
            return false;
        }
        if (i >= 0 && i <= this.f1701j.f1686g.mo182b()) {
            m68k();
            m69j();
            this.f1701j.f1688i = i;
            String m113a = Util.m113a(this.f1701j.f1680a, this.f1701j.f1688i);
            if (m113a == null || "".equals(m113a)) {
                if (this.f1701j.f1682c > 0) {
                    m113a = this.f1701j.f1680a;
                    this.f1701j.f1688i = 0;
                } else {
                    this.f1701j.f1690k = 9;
                    PlayerEventCallback playerEventCallback = this.f1702k;
                    if (playerEventCallback != null) {
                        playerEventCallback.onPlayerStateChanged(9);
                    }
                }
            }
            if (6 != this.f1701j.f1690k) {
                return true;
            }
            this.f1701j.f1681b = SystemClock.elapsedRealtime();
            if (this.f1701j.f1687h != null) {
                m113a = this.f1701j.f1680a;
                i2 = this.f1701j.f1683d;
                i3 = this.f1701j.f1684e;
                syncCallback = this.f1701j.f1687h;
            } else {
                i2 = this.f1701j.f1683d;
                i3 = this.f1701j.f1684e;
                syncCallback = null;
            }
            m74c(m113a, i2, i3, syncCallback);
            return true;
        }
        return false;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo36b() {
        this.f1701j.m81a();
        m68k();
        NonRichTapThread nonRichTapThread = this.f1696e;
        if (nonRichTapThread != null) {
            nonRichTapThread.m165c();
            this.f1696e = null;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo34b(int i, int i2) {
        int m163a = PatternHe.m163a(i, i2);
        int i3 = (i * 255) / 100;
        this.f1701j.m81a();
        if (this.f1694c == null) {
            Log.e("GooglePlayer", "Please call the init method");
            return;
        }
        mo53a();
        if (Build.VERSION.SDK_INT >= 26) {
            this.f1694c.vibrate(VibrationEffect.createOneShot(m163a, Math.max(1, Math.min(i3, 255))));
        } else {
            this.f1694c.vibrate(m163a);
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: b */
    public void mo32b(String str, int i, int i2, SyncCallback syncCallback) {
        this.f1701j.m81a();
        this.f1701j.f1680a = str;
        this.f1701j.f1683d = i;
        this.f1701j.f1684e = i2;
        this.f1701j.f1687h = syncCallback;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: c */
    public void mo31c() {
        if (6 != this.f1701j.f1690k) {
            return;
        }
        m68k();
        m69j();
        this.f1701j.f1690k = 7;
        if (!CurrentPlayingInfo.m80a(this.f1701j.f1686g)) {
            Log.e("GooglePlayer", "pause_start_seek pause() return - HE invalid or prepare() not be called");
            this.f1701j.f1688i = 0;
        } else if (this.f1701j.f1687h != null) {
            CurrentPlayingInfo currentPlayingInfo = this.f1701j;
            currentPlayingInfo.f1688i = currentPlayingInfo.f1687h.getCurrentPosition();
        } else {
            int elapsedRealtime = (int) (SystemClock.elapsedRealtime() - this.f1701j.f1681b);
            if (elapsedRealtime < 0) {
                this.f1701j.f1688i = 0;
                return;
            }
            this.f1701j.f1688i += elapsedRealtime;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: d */
    public boolean mo27d() {
        int i;
        int i2;
        SyncCallback syncCallback;
        if (6 == this.f1701j.f1690k) {
            return false;
        }
        if (!CurrentPlayingInfo.m80a(this.f1701j.f1686g)) {
            Log.e("GooglePlayer", "pause_start_seek start() return - HE invalid or prepare() not called.");
            return false;
        } else if (this.f1701j.f1688i < 0) {
            return false;
        } else {
            if (9 == this.f1701j.f1690k) {
                this.f1701j.f1688i = 0;
            }
            String m113a = Util.m113a(this.f1701j.f1680a, this.f1701j.f1688i);
            if (m113a == null || "".equals(m113a)) {
                this.f1701j.f1690k = 9;
                PlayerEventCallback playerEventCallback = this.f1702k;
                if (playerEventCallback != null) {
                    playerEventCallback.onPlayerStateChanged(9);
                }
                return false;
            }
            this.f1701j.f1681b = SystemClock.elapsedRealtime();
            this.f1701j.f1690k = 6;
            if (this.f1701j.f1687h != null) {
                m113a = this.f1701j.f1680a;
                i = this.f1701j.f1683d;
                i2 = this.f1701j.f1684e;
                syncCallback = this.f1701j.f1687h;
            } else {
                i = this.f1701j.f1683d;
                i2 = this.f1701j.f1684e;
                syncCallback = null;
            }
            m74c(m113a, i, i2, syncCallback);
            return true;
        }
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: e */
    public boolean mo24e() {
        if (this.f1701j.f1691l != null) {
            CurrentPlayingInfo currentPlayingInfo = this.f1701j;
            currentPlayingInfo.f1680a = Util.m102b(currentPlayingInfo.f1691l);
        }
        if (1 == Util.m88e(this.f1701j.f1680a)) {
            CurrentPlayingInfo currentPlayingInfo2 = this.f1701j;
            currentPlayingInfo2.f1680a = Util.m101b(currentPlayingInfo2.f1680a);
        }
        CurrentPlayingInfo currentPlayingInfo3 = this.f1701j;
        currentPlayingInfo3.f1680a = Util.m90d(currentPlayingInfo3.f1680a);
        CurrentPlayingInfo currentPlayingInfo4 = this.f1701j;
        currentPlayingInfo4.f1680a = Util.m85h(currentPlayingInfo4.f1680a);
        InterfaceC3959c m114a = Util.m114a(this.f1701j.f1680a);
        if (CurrentPlayingInfo.m80a(m114a)) {
            this.f1701j.f1686g = m114a;
            return true;
        }
        Log.e("GooglePlayer", "prepare error, invalid HE");
        this.f1701j.m81a();
        return false;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: f */
    public int mo22f() {
        if (this.f1701j.f1687h != null) {
            return this.f1701j.f1687h.getCurrentPosition();
        }
        switch (this.f1701j.f1690k) {
            case 6:
                return (int) ((SystemClock.elapsedRealtime() - this.f1701j.f1681b) + this.f1701j.f1688i);
            case 7:
                return this.f1701j.f1688i;
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
        if (this.f1701j.f1686g == null) {
            return 0;
        }
        return this.f1701j.f1686g.mo182b();
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: h */
    public boolean mo18h() {
        return 6 == this.f1701j.f1690k;
    }

    @Override // com.sysaac.haptic.player.Player
    /* renamed from: i */
    public void mo17i() {
        this.f1702k = null;
    }
}
