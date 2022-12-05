package com.sysaac.haptic.player;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import com.sysaac.haptic.base.PatternHe;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.sync.C4012b;
import com.sysaac.haptic.sync.C4014d;
import com.sysaac.haptic.sync.SyncCallback;

public class RichtapPlayerThread extends Handler {

    /* renamed from: a */
    int f1736a;

    /* renamed from: b */
    final /* synthetic */ int f1737b;

    /* renamed from: c */
    final /* synthetic */ int f1738c;

    /* renamed from: d */
    final /* synthetic */ RichtapPlayer f1739d;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public RichtapPlayerThread(RichtapPlayer richtapPlayer, Looper looper, int i, int i2) {
        super(looper);
        this.f1739d = richtapPlayer;
        this.f1737b = i;
        this.f1738c = i2;
        this.f1736a = 0;
    }

    @Override // android.p007os.Handler
    public void handleMessage(Message message) {
        Handler handler;
        HandlerThread handlerThread;
        C4014d c4014d;
        SyncCallback syncCallback;
        C4014d c4014d2;
        C4014d c4014d3;
        SyncCallback syncCallback2;
        Handler handler2;
        CurrentPlayingInfo currentPlayingInfo;
        C4014d c4014d4;
        C4014d c4014d5;
        SyncCallback syncCallback3;
        Context context;
        RepeatExecutor repeatExecutor;
        int i;
        RepeatExecutor repeatExecutor2;
        Context context2;
        RepeatExecutor repeatExecutor3;
        int i2;
        RepeatExecutor repeatExecutor4;
        SyncCallback syncCallback4;
        CurrentPlayingInfo currentPlayingInfo2;
        CurrentPlayingInfo currentPlayingInfo3;
        PlayerEventCallback playerEventCallback;
        PlayerEventCallback playerEventCallback2;
        SyncCallback syncCallback5;
        SyncCallback syncCallback6;
        CurrentPlayingInfo currentPlayingInfo4;
        Handler handler3;
        try {
            handler = this.f1739d.f1717f;
            if (handler != null) {
                handlerThread = this.f1739d.f1718g;
                if (handlerThread != null) {
                    c4014d = this.f1739d.f1719h;
                    if (c4014d != null) {
                        switch (message.what) {
                            case 100:
                                long longValue = ((Long) message.obj).longValue();
                                syncCallback = this.f1739d.f1720i;
                                if (syncCallback == null) {
                                    c4014d2 = this.f1739d.f1719h;
                                    c4014d2.m2b(longValue);
                                    c4014d3 = this.f1739d.f1719h;
                                    c4014d3.m4a(longValue, longValue);
                                    return;
                                }
                                syncCallback2 = this.f1739d.f1720i;
                                int currentPosition = syncCallback2.getCurrentPosition();
                                if (currentPosition <= 0 || currentPosition < this.f1736a) {
                                    if (currentPosition >= 0 && currentPosition < this.f1736a) {
                                        currentPlayingInfo = this.f1739d.f1721j;
                                        if (currentPlayingInfo.f1682c > 0) {
                                            this.f1739d.mo52a(currentPosition);
                                        }
                                    }
                                    handler2 = this.f1739d.f1717f;
                                    handler2.sendMessage(Message.obtain(message));
                                } else {
                                    c4014d4 = this.f1739d.f1719h;
                                    long j = currentPosition;
                                    c4014d4.m2b(j);
                                    c4014d5 = this.f1739d.f1719h;
                                    c4014d5.m4a(j, longValue);
                                }
                                this.f1736a = currentPosition;
                                return;
                            case 101:
                                if (!(message.obj instanceof Parcel)) {
                                    return;
                                }
                                Parcel parcel = (Parcel) message.obj;
                                syncCallback3 = this.f1739d.f1720i;
                                if (syncCallback3 != null) {
                                    syncCallback4 = this.f1739d.f1720i;
                                    syncCallback4.getCurrentPosition();
                                }
                                C4012b c4012b = new C4012b(parcel);
                                Log.d("RichtapPlayer", "current pattern:" + c4012b.f1793a);
                                if (Util.m122a() >= 24) {
                                    String replace = "{\n    \"Metadata\": {\n        \"Created\": \"2020-08-10\",\n        \"Description\": \"Haptic editor design\",\n        \"Version\": 2\n    },\n    \"PatternList\": [\n       {\n        \"AbsoluteTime\": 0,\n          ReplaceMe\n       }\n    ]\n}".replace("ReplaceMe", c4012b.f1793a);
                                    context2 = this.f1739d.f1715d;
                                    PatternHe m162a = PatternHe.m162a(context2);
                                    repeatExecutor3 = this.f1739d.f1723l;
                                    if (repeatExecutor3 != null) {
                                        repeatExecutor4 = this.f1739d.f1723l;
                                        i2 = repeatExecutor4.m133a();
                                    } else {
                                        i2 = this.f1737b;
                                    }
                                    m162a.mo148b(replace, 1, 0, i2, this.f1738c);
                                } else {
                                    String str = "{\"Metadata\": {\"Version\": 1}," + c4012b.f1793a + "}";
                                    context = this.f1739d.f1715d;
                                    PatternHe m162a2 = PatternHe.m162a(context);
                                    repeatExecutor = this.f1739d.f1723l;
                                    if (repeatExecutor != null) {
                                        repeatExecutor2 = this.f1739d.f1723l;
                                        i = repeatExecutor2.m133a();
                                    } else {
                                        i = this.f1737b;
                                    }
                                    m162a2.mo155a(str, 1, 0, i, this.f1738c);
                                }
                                parcel.recycle();
                                return;
                            case 102:
                                currentPlayingInfo2 = this.f1739d.f1721j;
                                if (currentPlayingInfo2.f1682c <= 0) {
                                    currentPlayingInfo3 = this.f1739d.f1721j;
                                    currentPlayingInfo3.f1690k = 9;
                                    playerEventCallback = this.f1739d.f1722k;
                                    if (playerEventCallback == null) {
                                        return;
                                    }
                                    playerEventCallback2 = this.f1739d.f1722k;
                                    playerEventCallback2.onPlayerStateChanged(9);
                                    return;
                                }
                                syncCallback5 = this.f1739d.f1720i;
                                if (syncCallback5 != null) {
                                    syncCallback6 = this.f1739d.f1720i;
                                    int currentPosition2 = syncCallback6.getCurrentPosition();
                                    currentPlayingInfo4 = this.f1739d.f1721j;
                                    if (currentPosition2 > Util.m83j(currentPlayingInfo4.f1680a)) {
                                        handler3 = this.f1739d.f1717f;
                                        handler3.sendEmptyMessageDelayed(102, 10L);
                                        return;
                                    }
                                }
                                this.f1739d.mo52a(0);
                                return;
                            default:
                                return;
                        }
                    }
                }
            }
            Log.d("RichtapPlayer", "after stopPatternListIfNeeded ...");
        } catch (Exception e) {
            Log.w("RichtapPlayer", "ex in handleMessage:" + e.toString());
        }
    }
}
