package com.sysaac.haptic.base;

import java.util.Arrays;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.base.f */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class Continuous extends Event {

    /* renamed from: a */
    int f1545a;

    /* renamed from: b */
    C3978l[] f1546b;

    /* renamed from: c */
    final /* synthetic */ PatternHeImpl f1547c;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public Continuous(PatternHeImpl patternHeImpl) {
        super(patternHeImpl);
        this.f1547c = patternHeImpl;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.sysaac.haptic.base.Event
    /* renamed from: a */
    public int[] mo136a() {
        int i = 8;
        int[] iArr = new int[(this.f1545a * 3) + 8];
        Arrays.fill(iArr, 0);
        iArr[0] = this.f1548d;
        iArr[1] = ((this.f1545a * 3) + 8) - 2;
        iArr[2] = this.f1550f;
        iArr[3] = this.f1551g;
        iArr[4] = this.f1552h;
        iArr[5] = this.f1553i;
        iArr[6] = this.f1554j;
        iArr[7] = this.f1545a;
        for (int i2 = 0; i2 < this.f1545a; i2++) {
            iArr[i] = this.f1546b[i2].f1567a;
            int i3 = i + 1;
            iArr[i3] = this.f1546b[i2].f1568b;
            int i4 = i3 + 1;
            iArr[i4] = this.f1546b[i2].f1569c;
            i = i4 + 1;
        }
        return iArr;
    }

    @Override // com.sysaac.haptic.base.Event
    public String toString() {
        return "Continuous{mPointNum=" + this.f1545a + ", mPoint=" + Arrays.toString(this.f1546b) + '}';
    }
}
