package com.sysaac.haptic.base;

/* JADX INFO: Access modifiers changed from: package-private */
/* renamed from: com.sysaac.haptic.base.g */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public abstract class Event {

    /* renamed from: d */
    int f1548d;

    /* renamed from: e */
    int f1549e;

    /* renamed from: f */
    int f1550f;

    /* renamed from: g */
    int f1551g;

    /* renamed from: h */
    int f1552h;

    /* renamed from: i */
    int f1553i;

    /* renamed from: j */
    int f1554j;

    /* renamed from: k */
    final /* synthetic */ PatternHeImpl f1555k;

    public Event(PatternHeImpl patternHeImpl) {
        this.f1555k = patternHeImpl;
    }

    /* renamed from: a */
    public abstract int[] mo136a();

    public String toString() {
        return "Event{mType=" + this.f1548d + ", mVibId=" + this.f1550f + ", mRelativeTime=" + this.f1551g + ", mIntensity=" + this.f1552h + ", mFreq=" + this.f1553i + ", mDuration=" + this.f1554j + '}';
    }
}
