package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

public class C4008p extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1761a;

    /* renamed from: b */
    final /* synthetic */ TencentPlayer f1762b;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4008p(TencentPlayer tencentPlayer, String str) {
        this.f1762b = tencentPlayer;
        this.f1761a = str;
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
        TencentPlayer tencentPlayer = this.f1762b;
        String str = this.f1761a;
        repeatExecutor = tencentPlayer.f1751l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1762b.f1751l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1762b.f1751l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1762b.f1751l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = 255;
        }
        tencentPlayer.m28c(m100b, i2, 0, null);
    }
}
