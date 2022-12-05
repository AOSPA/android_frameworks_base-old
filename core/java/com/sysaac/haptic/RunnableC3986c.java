package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.player.Player;

/* JADX INFO: Access modifiers changed from: package-private */
/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.c */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class RunnableC3986c implements Runnable {

    /* renamed from: a */
    final /* synthetic */ int[] f1646a;

    /* renamed from: b */
    final /* synthetic */ int[] f1647b;

    /* renamed from: c */
    final /* synthetic */ int[] f1648c;

    /* renamed from: d */
    final /* synthetic */ boolean f1649d;

    /* renamed from: e */
    final /* synthetic */ int f1650e;

    /* renamed from: f */
    final /* synthetic */ AACHapticUtils f1651f;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3986c(AACHapticUtils aACHapticUtils, int[] iArr, int[] iArr2, int[] iArr3, boolean z, int i) {
        this.f1651f = aACHapticUtils;
        this.f1646a = iArr;
        this.f1647b = iArr2;
        this.f1648c = iArr3;
        this.f1649d = z;
        this.f1650e = i;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1651f.mPlayer;
        if (player == null) {
            Log.w("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1651f.mPlayer;
            player2.mo37a(this.f1646a, this.f1647b, this.f1648c, this.f1649d, this.f1650e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
