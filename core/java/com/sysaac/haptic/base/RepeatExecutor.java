package com.sysaac.haptic.base;

import android.os.Handler;
import android.os.HandlerThread;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.base.o */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class RepeatExecutor {

    /* renamed from: i */
    private static final int f1575i = 101;

    /* renamed from: j */
    private static final int f1576j = 102;

    /* renamed from: k */
    private static final String f1577k = "RepeatExecutor";

    /* renamed from: a */
    public int f1578a;

    /* renamed from: b */
    private int f1579b;

    /* renamed from: c */
    private int f1580c = 255;

    /* renamed from: d */
    private int f1581d = 0;

    /* renamed from: e */
    public int f1582e;

    /* renamed from: f */
    private HandlerThread f1583f;

    /* renamed from: g */
    public Handler f1584g;

    /* renamed from: h */
    public AbstractC3983q f1585h;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* renamed from: d */
    public static /* synthetic */ int m124d(RepeatExecutor repeatExecutor) {
        int i = repeatExecutor.f1579b - 1;
        repeatExecutor.f1579b = i;
        return i;
    }

    /* renamed from: a */
    public synchronized int m133a() {
        return this.f1580c;
    }

    /* renamed from: a */
    public synchronized void m132a(int i) {
        this.f1578a = i;
    }

    /* renamed from: a */
    public synchronized void m131a(int i, int i2, int i3) {
        if (i >= 0) {
            try {
                this.f1578a = i;
            } catch (Throwable th) {
                throw th;
            }
        }
        if (i2 >= 0 && i2 <= 255) {
            this.f1580c = i2;
        }
        this.f1581d = i3;
    }

    /* renamed from: a */
    public synchronized void m130a(int i, int i2, int i3, AbstractC3983q abstractC3983q) {
        if (i < 1 || i3 < 0 || abstractC3983q == null) {
            return;
        }
        try {
            this.f1579b = i;
            this.f1578a = i2;
            this.f1582e = i3;
            this.f1585h = abstractC3983q;
            HandlerThread handlerThread = new HandlerThread("RepeatExecutor");
            this.f1583f = handlerThread;
            handlerThread.start();
            HandlerC3982p handlerC3982p = new HandlerC3982p(this, this.f1583f.getLooper());
            this.f1584g = handlerC3982p;
            handlerC3982p.sendEmptyMessage(101);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }

    /* renamed from: b */
    public synchronized int m128b() {
        return this.f1581d;
    }

    /* renamed from: c */
    public synchronized void m126c() {
        try {
            HandlerThread handlerThread = this.f1583f;
            if (handlerThread != null) {
                handlerThread.quit();
                this.f1583f = null;
            }
            this.f1584g = null;
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
