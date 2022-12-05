package com.sysaac.haptic.base;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.base.b */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class NonRichTapLooperInfo {

    /* renamed from: a */
    String f1523a;

    /* renamed from: b */
    int f1524b;

    /* renamed from: c */
    int f1525c;

    /* renamed from: d */
    int f1526d;

    /* renamed from: e */
    int f1527e;

    /* renamed from: f */
    public long f1528f;

    /* renamed from: h */
    int f1530h;

    /* renamed from: g */
    public boolean f1529g = true;

    /* renamed from: i */
    public int f1531i = 0;

    public NonRichTapLooperInfo(String str, int i, int i2, int i3, int i4) {
        this.f1523a = str;
        this.f1524b = i;
        this.f1525c = i2;
        this.f1526d = i3;
        this.f1527e = i4;
    }

    /* renamed from: a */
    public int m180a() {
        return this.f1530h;
    }

    /* renamed from: a */
    public void m179a(int i) {
        this.f1530h = i;
    }

    /* renamed from: b */
    public String m178b() {
        return this.f1523a;
    }

    /* renamed from: b */
    public void m177b(int i) {
        this.f1525c = i;
    }

    /* renamed from: c */
    public int m176c() {
        return this.f1524b;
    }

    /* renamed from: c */
    public void m175c(int i) {
        this.f1526d = i;
    }

    /* renamed from: d */
    public int m174d() {
        return this.f1525c;
    }

    /* renamed from: d */
    public void m173d(int i) {
        this.f1527e = i;
    }

    /* renamed from: e */
    public int m172e() {
        return this.f1526d;
    }

    /* renamed from: f */
    public int m171f() {
        return this.f1527e;
    }

    public String toString() {
        return "NonRichTapLooperInfo{mPattern='" + this.f1523a + "', mLooper=" + this.f1524b + ", mInterval=" + this.f1525c + ", mAmplitude=" + this.f1526d + ", mFreq=" + this.f1527e + ", mWhen=" + this.f1528f + ", mValid=" + this.f1529g + ", mPatternLastTime=" + this.f1530h + ", mHasVibNum=" + this.f1531i + '}';
    }
}
