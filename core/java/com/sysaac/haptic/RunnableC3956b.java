package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.player.Player;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.b */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class RunnableC3956b implements Runnable {

    /* renamed from: a */
    final /* synthetic */ int f1491a;

    /* renamed from: b */
    final /* synthetic */ int f1492b;

    /* renamed from: c */
    final /* synthetic */ AACHapticUtils f1493c;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3956b(AACHapticUtils aACHapticUtils, int i, int i2) {
        this.f1493c = aACHapticUtils;
        this.f1491a = i;
        this.f1492b = i2;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1493c.mPlayer;
        if (player == null) {
            Log.w("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1493c.mPlayer;
            player2.mo34b(this.f1491a, this.f1492b);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
