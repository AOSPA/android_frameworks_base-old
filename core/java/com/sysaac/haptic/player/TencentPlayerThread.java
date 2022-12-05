package com.sysaac.haptic.player;

import android.os.DynamicEffect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.HapticPlayer;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.util.Log;
import com.sysaac.haptic.base.RepeatExecutor;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.sync.C4012b;
import com.sysaac.haptic.sync.C4014d;
import com.sysaac.haptic.sync.SyncCallback;

public class TencentPlayerThread extends Handler {

    /* renamed from: a */
    int f1767a;

    /* renamed from: b */
    final /* synthetic */ int f1768b;

    /* renamed from: c */
    final /* synthetic */ int f1769c;

    /* renamed from: d */
    final /* synthetic */ TencentPlayer f1770d;

    public TencentPlayerThread(TencentPlayer tencentPlayer, Looper looper, int i, int i2) {
        super(looper);
        this.f1770d = tencentPlayer;
        this.f1768b = i;
        this.f1769c = i2;
        this.f1767a = 0;
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
            handler = this.f1770d.f1745f;
            if (handler != null) {
                handlerThread = this.f1770d.f1746g;
                if (handlerThread != null) {
                    c4014d = this.f1770d.f1747h;
                    if (c4014d != null) {
                        switch (message.what) {
                            case 100:
                                long longValue = ((Long) message.obj).longValue();
                                syncCallback = this.f1770d.f1748i;
                                if (syncCallback == null) {
                                    c4014d2 = this.f1770d.f1747h;
                                    c4014d2.m2b(longValue);
                                    c4014d3 = this.f1770d.f1747h;
                                    c4014d3.m4a(longValue, longValue);
                                    return;
                                }
                                syncCallback2 = this.f1770d.f1748i;
                                int currentPosition = syncCallback2.getCurrentPosition();
                                if (currentPosition <= 0 || currentPosition < this.f1767a) {
                                    if (currentPosition >= 0 && currentPosition < this.f1767a) {
                                        currentPlayingInfo = this.f1770d.f1749j;
                                        if (currentPlayingInfo.f1682c > 0) {
                                            this.f1770d.mo52a(currentPosition);
                                        }
                                    }
                                    handler2 = this.f1770d.f1745f;
                                    handler2.sendMessage(Message.obtain(message));
                                } else {
                                    c4014d4 = this.f1770d.f1747h;
                                    long j = currentPosition;
                                    c4014d4.m2b(j);
                                    c4014d5 = this.f1770d.f1747h;
                                    c4014d5.m4a(j, longValue);
                                }
                                this.f1767a = currentPosition;
                                return;
                            case 101:
                                if (!(message.obj instanceof Parcel)) {
                                    return;
                                }
                                Parcel parcel = (Parcel) message.obj;
                                syncCallback3 = this.f1770d.f1748i;
                                if (syncCallback3 != null) {
                                    syncCallback4 = this.f1770d.f1748i;
                                    syncCallback4.getCurrentPosition();
                                }
                                C4012b c4012b = new C4012b(parcel);
                                String str = "{\"Metadata\": {\"Version\": 1}," + c4012b.f1793a + "}";
                                if (HapticPlayer.isAvailable()) {
                                    this.f1770d.f1741a = new HapticPlayer(DynamicEffect.create(str));
                                    try {
                                        HapticPlayer hapticPlayer = this.f1770d.f1741a;
                                        repeatExecutor = this.f1770d.f1751l;
                                        if (repeatExecutor != null) {
                                            repeatExecutor2 = this.f1770d.f1751l;
                                            i = repeatExecutor2.m133a();
                                        } else {
                                            i = this.f1768b;
                                        }
                                        hapticPlayer.start(1, 0, i, this.f1769c);
                                    } catch (NoSuchMethodError e) {
                                        Log.w("TencentPlayer", "[patternString, looper,interval,amplitude,freq],haven't integrate Haptic player 1.1 !!!!!!!now we use Haptic player 1.0 to start vibrate");
                                        this.f1770d.f1741a.start(1);
                                    }
                                } else {
                                    Log.e("TencentPlayer", "The system does not support HapticsPlayer");
                                }
                                parcel.recycle();
                                return;
                            case 102:
                                currentPlayingInfo2 = this.f1770d.f1749j;
                                if (currentPlayingInfo2.f1682c <= 0) {
                                    currentPlayingInfo3 = this.f1770d.f1749j;
                                    currentPlayingInfo3.f1690k = 9;
                                    playerEventCallback = this.f1770d.f1750k;
                                    if (playerEventCallback == null) {
                                        return;
                                    }
                                    playerEventCallback2 = this.f1770d.f1750k;
                                    playerEventCallback2.onPlayerStateChanged(9);
                                    return;
                                }
                                syncCallback5 = this.f1770d.f1748i;
                                if (syncCallback5 != null) {
                                    syncCallback6 = this.f1770d.f1748i;
                                    int currentPosition2 = syncCallback6.getCurrentPosition();
                                    currentPlayingInfo4 = this.f1770d.f1749j;
                                    if (currentPosition2 > Util.m83j(currentPlayingInfo4.f1680a)) {
                                        handler3 = this.f1770d.f1745f;
                                        handler3.sendEmptyMessageDelayed(102, 10L);
                                        return;
                                    }
                                }
                                this.f1770d.mo52a(0);
                                return;
                            default:
                                return;
                        }
                    }
                }
            }
            Log.d("TencentPlayer", "after stopPatternListIfNeeded ...");
        } catch (Exception e2) {
            Log.w("TencentPlayer", "ex in handleMessage:" + e2.toString());
        }
    }
}
