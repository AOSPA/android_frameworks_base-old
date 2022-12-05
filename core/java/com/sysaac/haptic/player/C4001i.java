package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

public class C4001i extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1728a;

    /* renamed from: b */
    final /* synthetic */ int f1729b;

    /* renamed from: c */
    final /* synthetic */ int f1730c;

    /* renamed from: d */
    final /* synthetic */ RichtapPlayer f1731d;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4001i(RichtapPlayer richtapPlayer, String str, int i, int i2) {
        this.f1731d = richtapPlayer;
        this.f1728a = str;
        this.f1729b = i;
        this.f1730c = i2;
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
        RichtapPlayer richtapPlayer = this.f1731d;
        String str = this.f1728a;
        repeatExecutor = richtapPlayer.f1723l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1731d.f1723l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1731d.f1723l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1731d.f1723l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = this.f1729b;
        }
        richtapPlayer.m62c(m100b, i2, this.f1730c, null);
    }
}
