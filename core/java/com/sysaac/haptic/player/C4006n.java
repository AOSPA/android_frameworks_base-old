package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

class C4006n extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1754a;

    /* renamed from: b */
    final /* synthetic */ int f1755b;

    /* renamed from: c */
    final /* synthetic */ TencentPlayer f1756c;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4006n(TencentPlayer tencentPlayer, String str, int i) {
        this.f1756c = tencentPlayer;
        this.f1754a = str;
        this.f1755b = i;
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
        TencentPlayer tencentPlayer = this.f1756c;
        String str = this.f1754a;
        repeatExecutor = tencentPlayer.f1751l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1756c.f1751l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1756c.f1751l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1756c.f1751l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = this.f1755b;
        }
        tencentPlayer.m28c(m100b, i2, 0, null);
    }
}
