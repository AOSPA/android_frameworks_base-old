package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.player.Player;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.g */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class RunnableC3990g implements Runnable {

    /* renamed from: a */
    final /* synthetic */ int f1669a;

    /* renamed from: b */
    final /* synthetic */ int f1670b;

    /* renamed from: c */
    final /* synthetic */ AACHapticUtils f1671c;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3990g(AACHapticUtils aACHapticUtils, int i, int i2) {
        this.f1671c = aACHapticUtils;
        this.f1669a = i;
        this.f1670b = i2;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1671c.mPlayer;
        if (player == null) {
            Log.w("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1671c.mPlayer;
            player2.mo50a(this.f1669a, this.f1670b, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
