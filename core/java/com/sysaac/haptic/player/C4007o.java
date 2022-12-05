package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

class C4007o extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1757a;

    /* renamed from: b */
    final /* synthetic */ int f1758b;

    /* renamed from: c */
    final /* synthetic */ int f1759c;

    /* renamed from: d */
    final /* synthetic */ TencentPlayer f1760d;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C4007o(TencentPlayer tencentPlayer, String str, int i, int i2) {
        this.f1760d = tencentPlayer;
        this.f1757a = str;
        this.f1758b = i;
        this.f1759c = i2;
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
        TencentPlayer tencentPlayer = this.f1760d;
        String str = this.f1757a;
        repeatExecutor = tencentPlayer.f1751l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1760d.f1751l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(str, i);
        repeatExecutor2 = this.f1760d.f1751l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1760d.f1751l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = this.f1758b;
        }
        tencentPlayer.m28c(m100b, i2, this.f1759c, null);
    }
}
