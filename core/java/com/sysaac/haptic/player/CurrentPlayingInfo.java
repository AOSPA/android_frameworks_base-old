package com.sysaac.haptic.player;

import com.sysaac.haptic.p033b.p034a.InterfaceC3959c;
import com.sysaac.haptic.p033b.p035b.C3962a;
import com.sysaac.haptic.p033b.p036c.C3964a;
import com.sysaac.haptic.p033b.p036c.C3966c;
import com.sysaac.haptic.sync.SyncCallback;
import java.io.File;
import java.util.Iterator;

public class CurrentPlayingInfo {

    /* renamed from: m */
    private static final String f1679m = "CurrentPlayingInfo";

    /* renamed from: a */
    public String f1680a;

    /* renamed from: b */
    public long f1681b;

    /* renamed from: c */
    public int f1682c;

    /* renamed from: d */
    public int f1683d;

    /* renamed from: e */
    public int f1684e;

    /* renamed from: f */
    public int f1685f;

    /* renamed from: g */
    public InterfaceC3959c f1686g;

    /* renamed from: h */
    public SyncCallback f1687h;

    /* renamed from: i */
    public int f1688i;

    /* renamed from: j */
    public int f1689j;

    /* renamed from: k */
    public int f1690k;

    /* renamed from: l */
    public File f1691l;

    /* renamed from: a */
    public static boolean m80a(InterfaceC3959c interfaceC3959c) {
        C3964a c3964a;
        if (interfaceC3959c == null) {
            return false;
        }
        if (1 != interfaceC3959c.mo183a()) {
            return (2 != interfaceC3959c.mo183a() || (c3964a = (C3964a) interfaceC3959c) == null || c3964a.f1512b == null || c3964a.f1512b.size() < 1 || c3964a.f1512b.get(0).f1517b == null || c3964a.f1512b.get(0).f1517b.size() < 1 || c3964a.f1512b.get(0).f1517b.get(0).f1505a == null) ? false : true;
        }
        C3962a c3962a = (C3962a) interfaceC3959c;
        return (c3962a == null || c3962a.f1507b == null || c3962a.f1507b.size() < 1 || c3962a.f1507b.get(0).f1505a == null) ? false : true;
    }

    /* renamed from: a */
    public void m81a() {
        this.f1680a = null;
        this.f1681b = 0L;
        this.f1682c = 0;
        this.f1683d = 0;
        this.f1684e = 0;
        this.f1685f = 0;
        this.f1686g = null;
        this.f1687h = null;
        this.f1688i = 0;
        this.f1690k = 0;
    }

    /* renamed from: b */
    public int m79b() {
        InterfaceC3959c interfaceC3959c = this.f1686g;
        if (interfaceC3959c == null) {
            return -1;
        }
        if (2 == interfaceC3959c.mo183a()) {
            Iterator<C3966c> it = ((C3964a) this.f1686g).f1512b.iterator();
            int i = 0;
            while (it.hasNext()) {
                C3966c next = it.next();
                i += next.f1517b == null ? 0 : next.f1517b.size();
            }
            return i;
        } else if (1 != this.f1686g.mo183a()) {
            return -1;
        } else {
            C3962a c3962a = (C3962a) this.f1686g;
            if (c3962a.f1507b != null) {
                return c3962a.f1507b.size();
            }
            return 0;
        }
    }

    public String toString() {
        return "CurrentPlayingHeInfo{mHeString='" + this.f1680a + "', mStartTime=" + this.f1681b + ", mLoop=" + this.f1682c + ", mAmplitude=" + this.f1683d + ", mFreq=" + this.f1684e + ", mHeRoot=" + this.f1686g + ", mSyncCallback=" + this.f1687h + ", mStartPosition=" + this.f1688i + ", mStatus:" + this.f1690k + '}';
    }
}
