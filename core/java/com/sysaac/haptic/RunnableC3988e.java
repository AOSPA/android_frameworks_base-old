package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.player.Player;

/* JADX INFO: Access modifiers changed from: package-private */
/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.e */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class RunnableC3988e implements Runnable {

    /* renamed from: a */
    final /* synthetic */ String f1658a;

    /* renamed from: b */
    final /* synthetic */ int f1659b;

    /* renamed from: c */
    final /* synthetic */ int f1660c;

    /* renamed from: d */
    final /* synthetic */ int f1661d;

    /* renamed from: e */
    final /* synthetic */ int f1662e;

    /* renamed from: f */
    final /* synthetic */ AACHapticUtils f1663f;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3988e(AACHapticUtils aACHapticUtils, String str, int i, int i2, int i3, int i4) {
        this.f1663f = aACHapticUtils;
        this.f1658a = str;
        this.f1659b = i;
        this.f1660c = i2;
        this.f1661d = i3;
        this.f1662e = i4;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1663f.mPlayer;
        if (player == null) {
            Log.w("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1663f.mPlayer;
            player2.mo40a(this.f1658a, this.f1659b, this.f1660c, this.f1661d, this.f1662e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
