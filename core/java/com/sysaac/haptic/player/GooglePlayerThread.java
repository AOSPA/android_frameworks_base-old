package com.sysaac.haptic.player;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.sync.C4012b;
import com.sysaac.haptic.sync.C4014d;
import com.sysaac.haptic.sync.SyncCallback;

public class GooglePlayerThread extends Handler {

    /* renamed from: a */
    int f1708a;

    /* renamed from: b */
    final /* synthetic */ int f1709b;

    /* renamed from: c */
    final /* synthetic */ int f1710c;

    /* renamed from: d */
    final /* synthetic */ GooglePlayer f1711d;

    /* JADX INFO: Access modifiers changed from: package-private */
    /* JADX WARN: 'super' call moved to the top of the method (can break code semantics) */
    public GooglePlayerThread(GooglePlayer googlePlayer, Looper looper, int i, int i2) {
        super(looper);
        this.f1711d = googlePlayer;
        this.f1709b = i;
        this.f1710c = i2;
        this.f1708a = 0;
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
        RepeatExecutor repeatExecutor;
        int i;
        RepeatExecutor repeatExecutor2;
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
            handler = this.f1711d.f1697f;
            if (handler != null) {
                handlerThread = this.f1711d.f1698g;
                if (handlerThread != null) {
                    c4014d = this.f1711d.f1699h;
                    if (c4014d != null) {
                        switch (message.what) {
                            case 100:
                                long longValue = ((Long) message.obj).longValue();
                                syncCallback = this.f1711d.f1700i;
                                if (syncCallback == null) {
                                    c4014d2 = this.f1711d.f1699h;
                                    c4014d2.m2b(longValue);
                                    c4014d3 = this.f1711d.f1699h;
                                    c4014d3.m4a(longValue, longValue);
                                    return;
                                }
                                syncCallback2 = this.f1711d.f1700i;
                                int currentPosition = syncCallback2.getCurrentPosition();
                                if (currentPosition <= 0 || currentPosition < this.f1708a) {
                                    if (currentPosition >= 0 && currentPosition < this.f1708a) {
                                        currentPlayingInfo = this.f1711d.f1701j;
                                        if (currentPlayingInfo.f1682c > 0) {
                                            this.f1711d.mo52a(currentPosition);
                                        }
                                    }
                                    handler2 = this.f1711d.f1697f;
                                    handler2.sendMessage(Message.obtain(message));
                                } else {
                                    c4014d4 = this.f1711d.f1699h;
                                    long j = currentPosition;
                                    c4014d4.m2b(j);
                                    c4014d5 = this.f1711d.f1699h;
                                    c4014d5.m4a(j, longValue);
                                }
                                this.f1708a = currentPosition;
                                return;
                            case 101:
                                if (!(message.obj instanceof Parcel)) {
                                    return;
                                }
                                Parcel parcel = (Parcel) message.obj;
                                syncCallback3 = this.f1711d.f1700i;
                                if (syncCallback3 != null) {
                                    syncCallback4 = this.f1711d.f1700i;
                                    syncCallback4.getCurrentPosition();
                                }
                                C4012b c4012b = new C4012b(parcel);
                                String str = "{\"Metadata\": {\"Version\": 1}," + c4012b.f1793a + "}";
                                GooglePlayer googlePlayer = this.f1711d;
                                repeatExecutor = googlePlayer.f1703l;
                                if (repeatExecutor != null) {
                                    repeatExecutor2 = this.f1711d.f1703l;
                                    i = repeatExecutor2.m133a();
                                } else {
                                    i = this.f1709b;
                                }
                                googlePlayer.mo40a(str, 1, 0, i, this.f1710c);
                                parcel.recycle();
                                return;
                            case 102:
                                currentPlayingInfo2 = this.f1711d.f1701j;
                                if (currentPlayingInfo2.f1682c <= 0) {
                                    currentPlayingInfo3 = this.f1711d.f1701j;
                                    currentPlayingInfo3.f1690k = 9;
                                    playerEventCallback = this.f1711d.f1702k;
                                    if (playerEventCallback == null) {
                                        return;
                                    }
                                    playerEventCallback2 = this.f1711d.f1702k;
                                    playerEventCallback2.onPlayerStateChanged(9);
                                    return;
                                }
                                syncCallback5 = this.f1711d.f1700i;
                                if (syncCallback5 != null) {
                                    syncCallback6 = this.f1711d.f1700i;
                                    int currentPosition2 = syncCallback6.getCurrentPosition();
                                    currentPlayingInfo4 = this.f1711d.f1701j;
                                    if (currentPosition2 > Util.m83j(currentPlayingInfo4.f1680a)) {
                                        handler3 = this.f1711d.f1697f;
                                        handler3.sendEmptyMessageDelayed(102, 10L);
                                        return;
                                    }
                                }
                                this.f1711d.mo52a(0);
                                return;
                            default:
                                return;
                        }
                    }
                }
            }
            Log.d("GooglePlayer", "after stopPatternListIfNeeded ...");
        } catch (Exception e) {
            Log.w("GooglePlayer", "ex in handleMessage:" + e.toString());
        }
    }
}
