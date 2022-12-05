package com.sysaac.haptic.sync;

import android.os.Handler;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import com.sysaac.haptic.player.CurrentPlayingInfo;
import java.io.FileDescriptor;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.sync.d */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class C4014d {

    /* renamed from: b */
    public static final int f1796b = 100;

    /* renamed from: c */
    public static final int f1797c = 101;

    /* renamed from: d */
    public static final int f1798d = 102;

    /* renamed from: e */
    public static final long f1799e = 10;

    /* renamed from: f */
    private static final String f1800f = "VibrationTrack";

    /* renamed from: g */
    private static final int f1801g = 20;

    /* renamed from: i */
    private Handler f1804i;

    /* renamed from: j */
    private FileDescriptor f1805j;

    /* renamed from: k */
    private C4011a f1806k;

    /* renamed from: m */
    private long f1808m;

    /* renamed from: a */
    public boolean f1802a = false;

    /* renamed from: l */
    private long f1807l = -1;

    /* renamed from: h */
    private long f1803h = -1;

    public C4014d(Handler handler, String str) {
        this.f1804i = handler;
        this.f1806k = new C4011a(str);
    }

    public C4014d(Handler handler, String str, CurrentPlayingInfo currentPlayingInfo) {
        this.f1804i = handler;
        this.f1806k = new C4011a(str, currentPlayingInfo);
    }

    /* renamed from: a */
    public void m6a() {
        synchronized (this) {
            if (this.f1802a) {
                Log.d("VibrationTrack", "onStop");
            }
            this.f1803h = -1L;
            this.f1805j = null;
            this.f1804i.removeMessages(100);
            this.f1804i.removeMessages(101);
        }
        this.f1807l = -1L;
    }

    /* renamed from: a */
    public void m5a(long j) {
        if (this.f1802a) {
            Log.d("VibrationTrack", "onSeek " + j);
        }
        synchronized (this) {
            m1b(j, j);
        }
        m0c();
    }

    /* renamed from: a */
    public void m4a(long j, long j2) {
        if (this.f1802a) {
            Log.d("VibrationTrack", "onTimedEvent " + j2);
        }
        synchronized (this) {
            m1b(j, j2);
        }
        m0c();
    }

    /* renamed from: b */
    public void m3b() {
        synchronized (this) {
            if (this.f1802a) {
                Log.d("VibrationTrack", "onPause");
            }
            this.f1804i.removeMessages(100);
            this.f1804i.removeMessages(101);
        }
        this.f1807l = -1L;
    }

    /* renamed from: b */
    public void m2b(long j) {
        this.f1803h = j;
    }

    /* renamed from: b */
    protected void m1b(long j, long j2) {
        try {
            C4012b m12a = this.f1806k.m12a(j2);
            if (this.f1802a) {
                Log.d("VibrationTrack", "synchronize curPos:" + j + ",timeUs:" + j2 + " with " + m12a);
            }
            if (m12a == null || m12a.f1793a.isEmpty()) {
                return;
            }
            Parcel obtain = Parcel.obtain();
            m12a.writeToParcel(obtain, 0);
            obtain.setDataPosition(0);
            Message obtainMessage = this.f1804i.obtainMessage(101, 0, 0, obtain);
            long j3 = 0;
            if (j2 <= j) {
                this.f1804i.sendMessage(obtainMessage);
                this.f1808m = 0L;
                return;
            }
            long j4 = j2 - j;
            if (j4 > 20) {
                j3 = j4 - 20;
            }
            if (this.f1802a) {
                Log.d("VibrationTrack", "synchronize vib scheduleTime:" + j3);
            }
            this.f1808m = j3;
            this.f1804i.sendMessageDelayed(obtainMessage, j3);
        } catch (Exception e) {
            Log.e("VibrationTrack", e.getMessage(), e);
        }
    }

    /* renamed from: c */
    protected void m0c() {
        try {
            this.f1807l = this.f1806k.m13a();
            if (this.f1802a) {
                Log.d("VibrationTrack", "scheduleTimedEvents @" + this.f1807l + " after " + this.f1803h);
            }
            long j = this.f1807l;
            if (j != -1) {
                long j2 = (j - this.f1803h) - 20;
                Message obtainMessage = this.f1804i.obtainMessage(100, 0, 0, Long.valueOf(j));
                if (this.f1802a) {
                    Log.d("VibrationTrack", "scheduleTimedEvents scheduleTime:" + j2);
                }
                this.f1804i.sendMessageDelayed(obtainMessage, j2);
                return;
            }
            if (this.f1802a) {
                Log.d("VibrationTrack", "scheduleTimedEvents @ completed- tail pattern duration:" + this.f1806k.m9b() + ",mLastScheduledTime:" + this.f1808m);
            }
            this.f1804i.sendEmptyMessageDelayed(102, this.f1808m + this.f1806k.m9b());
        } catch (Exception e) {
            Log.w("VibrationTrack", "ex in scheduleTimedEvents");
        }
    }
}
