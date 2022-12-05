package com.sysaac.haptic;

import android.util.Log;
import com.sysaac.haptic.player.Player;

import java.io.File;

/* JADX INFO: Access modifiers changed from: package-private */
/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.d */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class RunnableC3987d implements Runnable {

    /* renamed from: a */
    final /* synthetic */ File f1652a;

    /* renamed from: b */
    final /* synthetic */ int f1653b;

    /* renamed from: c */
    final /* synthetic */ int f1654c;

    /* renamed from: d */
    final /* synthetic */ int f1655d;

    /* renamed from: e */
    final /* synthetic */ int f1656e;

    /* renamed from: f */
    final /* synthetic */ AACHapticUtils f1657f;

    /* JADX INFO: Access modifiers changed from: package-private */
    public RunnableC3987d(AACHapticUtils aACHapticUtils, File file, int i, int i2, int i3, int i4) {
        this.f1657f = aACHapticUtils;
        this.f1652a = file;
        this.f1653b = i;
        this.f1654c = i2;
        this.f1655d = i3;
        this.f1656e = i4;
    }

    @Override // java.lang.Runnable
    public void run() {
        Player player;
        Player player2;
        player = this.f1657f.mPlayer;
        if (player == null) {
            Log.w("AACHapticUtils", "mPlayer == null");
            return;
        }
        try {
            player2 = this.f1657f.mPlayer;
            player2.mo43a(this.f1652a, this.f1653b, this.f1654c, this.f1655d, this.f1656e);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
