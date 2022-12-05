package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.player.Player;
import com.sysaac.haptic.sync.SyncCallback;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.f */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class RunnableC3989f implements Runnable {

    /* renamed from: a */
    final /* synthetic */ String f1664a;

    /* renamed from: b */
    final /* synthetic */ int f1665b;

    /* renamed from: c */
    final /* synthetic */ int f1666c;

    /* renamed from: d */
    final /* synthetic */ SyncCallback f1667d;

    /* renamed from: e */
    final /* synthetic */ AACHapticUtils f1668e;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3989f(AACHapticUtils aACHapticUtils, String str, int i, int i2, SyncCallback syncCallback) {
        this.f1668e = aACHapticUtils;
        this.f1664a = str;
        this.f1665b = i;
        this.f1666c = i2;
        this.f1667d = syncCallback;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1668e.mPlayer;
        if (player == null) {
            Log.d("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1668e.mPlayer;
            player2.mo39a(this.f1664a, this.f1665b, this.f1666c, this.f1667d);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
