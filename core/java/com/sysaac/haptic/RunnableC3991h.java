package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.player.Player;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.h */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
class RunnableC3991h implements Runnable {

    /* renamed from: a */
    final /* synthetic */ int f1672a;

    /* renamed from: b */
    final /* synthetic */ int f1673b;

    /* renamed from: c */
    final /* synthetic */ int f1674c;

    /* renamed from: d */
    final /* synthetic */ AACHapticUtils f1675d;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3991h(AACHapticUtils aACHapticUtils, int i, int i2, int i3) {
        this.f1675d = aACHapticUtils;
        this.f1672a = i;
        this.f1673b = i2;
        this.f1674c = i3;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1675d.mPlayer;
        if (player == null) {
            Log.d("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1675d.mPlayer;
            player2.mo50a(this.f1672a, this.f1673b, this.f1674c);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
