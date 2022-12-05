package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

public class C4000h extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1724a;

    /* renamed from: b */
    final /* synthetic */ int f1725b;

    /* renamed from: c */
    final /* synthetic */ int f1726c;

    /* renamed from: d */
    final /* synthetic */ RichtapPlayer f1727d;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4000h(RichtapPlayer richtapPlayer, String str, int i, int i2) {
        this.f1727d = richtapPlayer;
        this.f1724a = str;
        this.f1725b = i;
        this.f1726c = i2;
    }

    /* JADX INFO: Access modifiers changed from: protected */
    @Override // com.sysaac.haptic.base.AbstractC3983q
    /* renamed from: a */
    public void mo14a() {
        RepeatExecutor repeatExecutor;
        int i;
        RepeatExecutor repeatExecutor2;
        int i2;
        RepeatExecutor repeatExecutor3;
        RepeatExecutor repeatExecutor4;
        RichtapPlayer richtapPlayer = this.f1727d;
        String str = this.f1724a;
        repeatExecutor = richtapPlayer.f1723l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1727d.f1723l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1727d.f1723l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1727d.f1723l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = this.f1725b;
        }
        richtapPlayer.m62c(m100b, i2, this.f1726c, null);
    }
}
