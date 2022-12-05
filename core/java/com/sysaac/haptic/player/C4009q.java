package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

class C4009q extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1763a;

    /* renamed from: b */
    final /* synthetic */ int f1764b;

    /* renamed from: c */
    final /* synthetic */ int f1765c;

    /* renamed from: d */
    final /* synthetic */ TencentPlayer f1766d;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4009q(TencentPlayer tencentPlayer, String str, int i, int i2) {
        this.f1766d = tencentPlayer;
        this.f1763a = str;
        this.f1764b = i;
        this.f1765c = i2;
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
        TencentPlayer tencentPlayer = this.f1766d;
        String str = this.f1763a;
        repeatExecutor = tencentPlayer.f1751l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1766d.f1751l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1766d.f1751l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1766d.f1751l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = this.f1764b;
        }
        tencentPlayer.m28c(m100b, i2, this.f1765c, null);
    }
}
