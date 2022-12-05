package com.sysaac.haptic.base;

import java.util.Arrays;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.base.m */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class C3979m extends Event {

    /* renamed from: a */
    final /* synthetic */ PatternHeImpl f1571a;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public C3979m(PatternHeImpl patternHeImpl) {
        super(patternHeImpl);
        this.f1571a = patternHeImpl;
        this.f1549e = 7;
    }

    /* JADX INFO: Access modifiers changed from: package-private */
    @Override // com.sysaac.haptic.base.Event
    /* renamed from: a */
    public int[] mo136a() {
        int[] iArr = new int[this.f1549e];
        Arrays.fill(iArr, 0);
        iArr[0] = this.f1548d;
        iArr[1] = this.f1549e - 2;
        iArr[2] = this.f1550f;
        iArr[3] = this.f1551g;
        iArr[4] = this.f1552h;
        iArr[5] = this.f1553i;
        iArr[6] = this.f1554j;
        return iArr;
    }
}
