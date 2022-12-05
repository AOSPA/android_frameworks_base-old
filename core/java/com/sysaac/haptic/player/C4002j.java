package com.sysaac.haptic.player;

import android.content.Context;
import com.sysaac.haptic.base.PatternHe;
import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

public class C4002j extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1732a;

    /* renamed from: b */
    final /* synthetic */ int f1733b;

    /* renamed from: c */
    final /* synthetic */ int f1734c;

    /* renamed from: d */
    final /* synthetic */ RichtapPlayer f1735d;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4002j(RichtapPlayer richtapPlayer, String str, int i, int i2) {
        this.f1735d = richtapPlayer;
        this.f1732a = str;
        this.f1733b = i;
        this.f1734c = i2;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.sysaac.haptic.base.AbstractC3983q
    /* renamed from: a */
    public void mo14a() {
        Context context;
        RepeatExecutor repeatExecutor;
        int i;
        RepeatExecutor repeatExecutor2;
        int i2;
        RepeatExecutor repeatExecutor3;
        RepeatExecutor repeatExecutor4;
        context = this.f1735d.f1715d;
        PatternHe m162a = PatternHe.m162a(context);
        String str = this.f1732a;
        repeatExecutor = this.f1735d.f1723l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1735d.f1723l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1735d.f1723l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1735d.f1723l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = this.f1733b;
        }
        m162a.mo148b(m100b, 1, 0, i2, this.f1734c);
    }
}
