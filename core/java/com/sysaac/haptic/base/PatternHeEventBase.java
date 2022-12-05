package com.sysaac.haptic.base;

/* JADX INFO: Access modifiers changed from: package-private */
/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.base.j */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public abstract class PatternHeEventBase {

    /* renamed from: a */
    int f1562a;

    /* renamed from: b */
    int f1563b;

    /* renamed from: c */
    int f1564c;

    /* renamed from: d */
    int f1565d;

    /* renamed from: e */
    int f1566e;

    public PatternHeEventBase(int i, int i2, int i3) {
        this.f1562a = i;
        this.f1563b = i2;
        this.f1566e = i3;
    }

    /* renamed from: a */
    abstract int[] mo137a();

    public String toString() {
        return "PatternHeEventBase{mEventType=" + this.f1562a + ", mRelativeTime=" + this.f1563b + ", mDuration=" + this.f1566e + '}';
    }
}
