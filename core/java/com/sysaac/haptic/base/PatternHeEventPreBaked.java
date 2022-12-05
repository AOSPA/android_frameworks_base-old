package com.sysaac.haptic.base;

import java.util.Arrays;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.base.k */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class PatternHeEventPreBaked extends PatternHeEventBase {
    /* JADX INFO: Access modifiers changed from: package-private */
    public PatternHeEventPreBaked(int i, int i2, int i3, int i4, int i5) {
        super(i, i2, i3);
        this.f1562a = i;
        this.f1563b = i2;
        this.f1566e = i3;
        this.f1564c = i4;
        this.f1565d = i5;
    }

    @Override // com.sysaac.haptic.base.PatternHeEventBase
    /* renamed from: a */
    public int[] mo137a() {
        int[] iArr = new int[17];
        Arrays.fill(iArr, 0);
        iArr[0] = this.f1562a;
        iArr[1] = this.f1563b;
        iArr[2] = this.f1564c;
        iArr[3] = this.f1565d;
        iArr[4] = this.f1566e;
        return iArr;
    }

    @Override // com.sysaac.haptic.base.PatternHeEventBase
    public String toString() {
        return "PatternHeEventPreBaked{mEventType=" + this.f1562a + ", mRelativeTime=" + this.f1563b + ", mDuration=" + this.f1566e + ", mIntensity=" + this.f1564c + ", mFrequency=" + this.f1565d + '}';
    }
}
