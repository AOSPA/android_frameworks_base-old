package com.sysaac.haptic.base;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

/* JADX INFO: Access modifiers changed from: package-private */
/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.base.p */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class HandlerC3982p extends Handler {

    /* renamed from: a */
    final /* synthetic */ RepeatExecutor f1586a;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public HandlerC3982p(RepeatExecutor repeatExecutor, Looper looper) {
        super(looper);
        this.f1586a = repeatExecutor;
    }

    @Override // android.p007os.Handler
    public void handleMessage(Message message) {
        AbstractC3983q abstractC3983q;
        Handler handler;
        int i;
        int i2;
        try {
            switch (message.what) {
                case 101:
                    abstractC3983q = this.f1586a.f1585h;
                    abstractC3983q.mo14a();
                    handler = this.f1586a.f1584g;
                    i = 102;
                    i2 = this.f1586a.f1582e;
                    break;
                case 102:
                    if (RepeatExecutor.m124d(this.f1586a) <= 0) {
                        return;
                    }
                    handler = this.f1586a.f1584g;
                    i = 101;
                    i2 = this.f1586a.f1578a;
                    break;
                default:
                    return;
            }
            handler.sendEmptyMessageDelayed(i, i2);
        } catch (Throwable th) {
            th.printStackTrace();
        }
    }
}
