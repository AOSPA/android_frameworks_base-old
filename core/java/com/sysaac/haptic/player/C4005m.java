package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

class C4005m extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1752a;

    /* renamed from: b */
    final /* synthetic */ TencentPlayer f1753b;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4005m(TencentPlayer tencentPlayer, String str) {
        this.f1753b = tencentPlayer;
        this.f1752a = str;
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
        TencentPlayer tencentPlayer = this.f1753b;
        String str = this.f1752a;
        repeatExecutor = tencentPlayer.f1751l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1753b.f1751l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1753b.f1751l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1753b.f1751l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = 255;
        }
        tencentPlayer.m28c(m100b, i2, 0, null);
    }
}
