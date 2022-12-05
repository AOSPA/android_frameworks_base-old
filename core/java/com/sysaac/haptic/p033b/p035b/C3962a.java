package com.sysaac.haptic.p033b.p035b;

import com.sysaac.haptic.p033b.p034a.C3958b;
import com.sysaac.haptic.p033b.p034a.C3961e;
import com.sysaac.haptic.p033b.p034a.InterfaceC3959c;
import java.util.ArrayList;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.b.b.a */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class C3962a implements InterfaceC3959c {

    /* renamed from: a */
    public C3963b f1506a;

    /* renamed from: b */
    public ArrayList<C3961e> f1507b;

    @Override // com.sysaac.haptic.p033b.p034a.InterfaceC3959c
    /* renamed from: a */
    public int mo183a() {
        return this.f1506a.f1508a;
    }

    @Override // com.sysaac.haptic.p033b.p034a.InterfaceC3959c
    /* renamed from: b */
    public int mo182b() {
        try {
            ArrayList<C3961e> arrayList = this.f1507b;
            C3958b c3958b = arrayList.get(arrayList.size() - 1).f1505a;
            return "continuous".equals(c3958b.f1497a) ? c3958b.f1498b + c3958b.f1499c : c3958b.f1498b + 48;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }
}
