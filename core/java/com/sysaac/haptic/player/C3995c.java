package com.sysaac.haptic.player;

import com.sysaac.haptic.base.AbstractC3983q;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;

public class C3995c extends AbstractC3983q {

    /* renamed from: a */
    final /* synthetic */ String f1704a;

    /* renamed from: b */
    final /* synthetic */ int f1705b;

    /* renamed from: c */
    final /* synthetic */ int f1706c;

    /* renamed from: d */
    final /* synthetic */ GooglePlayer f1707d;

    /* JADX INFO: Access modifiers changed from: package-private */
    public C3995c(GooglePlayer googlePlayer, String str, int i, int i2) {
        this.f1707d = googlePlayer;
        this.f1704a = str;
        this.f1705b = i;
        this.f1706c = i2;
    }

    @Override
    public void mo14a() {
        RepeatExecutor repeatExecutor;
        int i;
        RepeatExecutor repeatExecutor2;
        int i2;
        RepeatExecutor repeatExecutor3;
        RepeatExecutor repeatExecutor4;
        GooglePlayer googlePlayer = this.f1707d;
        repeatExecutor = googlePlayer.f1703l;
        if (repeatExecutor != null) {
            repeatExecutor4 = this.f1707d.f1703l;
            i = repeatExecutor4.m128b();
        } else {
            i = -1;
        }
        String m100b = Util.m100b(this.f1704a, i);
        repeatExecutor2 = this.f1707d.f1703l;
        if (repeatExecutor2 != null) {
            repeatExecutor3 = this.f1707d.f1703l;
            i2 = repeatExecutor3.m133a();
        } else {
            i2 = this.f1705b;
        }
        googlePlayer.m74c(m100b, i2, this.f1706c, null);
    }
}
