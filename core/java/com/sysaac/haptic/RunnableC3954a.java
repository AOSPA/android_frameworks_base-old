package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.base.PreBakedEffect;
import com.sysaac.haptic.player.Player;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.a */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class RunnableC3954a implements Runnable {

    /* renamed from: a */
    final /* synthetic */ int f1479a;

    /* renamed from: b */
    final /* synthetic */ int f1480b;

    /* renamed from: c */
    final /* synthetic */ AACHapticUtils f1481c;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3954a(AACHapticUtils aACHapticUtils, int i, int i2) {
        this.f1481c = aACHapticUtils;
        this.f1479a = i;
        this.f1480b = i2;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1481c.mPlayer;
        if (player == null) {
            Log.w("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1481c.mPlayer;
            player2.mo40a(PreBakedEffect.m134b(this.f1479a), 1, 0, this.f1480b, 0);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
