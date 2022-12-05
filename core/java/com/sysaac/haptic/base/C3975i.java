package com.sysaac.haptic.base;

import java.util.Arrays;

public class C3975i {

    /* renamed from: a */
    int f1559a;

    /* renamed from: b */
    Event[] f1560b;

    /* renamed from: c */
    final /* synthetic */ PatternHeImpl f1561c;

    public C3975i(PatternHeImpl patternHeImpl) {
        this.f1561c = patternHeImpl;
    }

    /* renamed from: a */
    int m140a() {
        Event[] eventArr;
        int i = 0;
        for (Event event : this.f1560b) {
            if (event.f1548d == 4096) {
                i += (((Continuous) event).f1545a * 3) + 8;
            } else if (event.f1548d == 4097) {
                i += 7;
            }
        }
        return i;
    }

    /* renamed from: a */
    public int[] m139a(int i) {
        int[] iArr = new int[m138b()];
        Arrays.fill(iArr, 0);
        iArr[0] = i;
        iArr[1] = this.f1559a;
        Event[] eventArr = this.f1560b;
        iArr[2] = eventArr.length;
        int i2 = 3;
        for (Event event : eventArr) {
            int[] mo136a = event.mo136a();
            System.arraycopy(mo136a, 0, iArr, i2, mo136a.length);
            i2 += mo136a.length;
        }
        return iArr;
    }

    /* renamed from: b */
    public int m138b() {
        return m140a() + 3;
    }
}
