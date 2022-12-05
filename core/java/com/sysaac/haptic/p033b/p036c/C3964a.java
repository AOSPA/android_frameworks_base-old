package com.sysaac.haptic.p033b.p036c;

import com.sysaac.haptic.p033b.p034a.C3961e;
import com.sysaac.haptic.p033b.p034a.InterfaceC3959c;
import java.util.ArrayList;
import java.util.Iterator;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.b.c.a */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class C3964a implements InterfaceC3959c {

    /* renamed from: a */
    public C3965b f1511a;

    /* renamed from: b */
    public ArrayList<C3966c> f1512b;

    @Override // com.sysaac.haptic.p033b.p034a.InterfaceC3959c
    /* renamed from: a */
    public int mo183a() {
        return this.f1511a.f1513a;
    }

    @Override // com.sysaac.haptic.p033b.p034a.InterfaceC3959c
    /* renamed from: b */
    public int mo182b() {
        ArrayList<C3966c> arrayList = null;
        try {
            C3966c c3966c = this.f1512b.get(arrayList.size() - 1);
            Iterator<C3961e> it = c3966c.f1517b.iterator();
            int i = 0;
            while (it.hasNext()) {
                C3961e next = it.next();
                int i2 = next.f1505a.f1497a.equals("continuous") ? next.f1505a.f1498b + next.f1505a.f1499c : next.f1505a.f1498b + 48;
                if (i2 > i) {
                    i = i2;
                }
            }
            return i + c3966c.f1516a;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
