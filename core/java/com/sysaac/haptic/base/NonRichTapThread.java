package com.sysaac.haptic.base;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;
import java.util.ArrayList;
import java.util.List;

public class NonRichTapThread extends Thread {

    /* renamed from: c */
    final Context f1534c;

    /* renamed from: f */
    private final String f1537f = "NonRichTapThread";

    /* renamed from: a */
    final Object f1532a = new Object();

    /* renamed from: b */
    final Object f1533b = new Object();

    /* renamed from: d */
    volatile boolean f1535d = false;

    /* renamed from: e */
    List<NonRichTapLooperInfo> f1536e = new ArrayList();

    public NonRichTapThread(Context context) {
        this.f1534c = context;
    }

    /* renamed from: a */
    long m170a() {
        return SystemClock.elapsedRealtime();
    }

    /* renamed from: a */
    public void m169a(int i) {
        NonRichTapLooperInfo nonRichTapLooperInfo = null;
        long m170a = 0;
        synchronized (this.f1532a) {
            synchronized (this.f1533b) {
                int size = this.f1536e.size();
                if (size > 1) {
                    Log.d("NonRichTapThread", "vibrating ,so interrupt it,size > 1,remove one");
                    this.f1536e.get(1).f1529g = false;
                    nonRichTapLooperInfo = this.f1536e.get(0);
                    m170a = m170a();
                } else if (size > 0) {
                    Log.d("NonRichTapThread", "vibrating ,so interrupt it,size == 1,just set next time play");
                    nonRichTapLooperInfo = this.f1536e.get(0);
                    m170a = m170a();
                }
                nonRichTapLooperInfo.f1528f = m170a + i + 5;
            }
            try {
                this.f1532a.notify();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* renamed from: a */
    public void m168a(NonRichTapLooperInfo nonRichTapLooperInfo) {
        synchronized (this.f1532a) {
            nonRichTapLooperInfo.m179a(nonRichTapLooperInfo.m174d() + PatternHe.m162a(this.f1534c).mo156a(nonRichTapLooperInfo.m178b()));
            nonRichTapLooperInfo.f1528f = 0L;
            synchronized (this.f1533b) {
                if (this.f1536e.size() > 0) {
                    Log.d("NonRichTapThread", "vibrating ,interrupt it");
                    this.f1536e.get(0).f1529g = false;
                }
                this.f1536e.add(0, nonRichTapLooperInfo);
            }
            try {
                this.f1532a.notify();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* renamed from: b */
    public void m167b() {
        synchronized (this.f1532a) {
            try {
            } catch (Exception e) {
                e.printStackTrace();
            }
            synchronized (this.f1533b) {
                if (this.f1536e.isEmpty()) {
                    return;
                }
                NonRichTapLooperInfo nonRichTapLooperInfo = this.f1536e.get(0);
                if (nonRichTapLooperInfo.f1529g) {
                    nonRichTapLooperInfo.f1529g = false;
                }
                this.f1532a.notify();
            }
        }
    }

    /* renamed from: b */
    public void m166b(NonRichTapLooperInfo nonRichTapLooperInfo) {
        synchronized (this.f1532a) {
            synchronized (this.f1533b) {
                if (this.f1536e.isEmpty()) {
                    Log.d("NonRichTapThread", "vib list is empty,do nothing!!");
                    return;
                }
                int m172e = nonRichTapLooperInfo.m172e();
                NonRichTapLooperInfo nonRichTapLooperInfo2 = this.f1536e.get(0);
                if (!nonRichTapLooperInfo2.f1529g) {
                    return;
                }
                if (m172e != -1) {
                    nonRichTapLooperInfo2.m175c(m172e);
                }
                int m174d = nonRichTapLooperInfo.m174d();
                if (m174d != -1) {
                    int m174d2 = m174d - nonRichTapLooperInfo2.m174d();
                    int m180a = nonRichTapLooperInfo2.m180a() + m174d2;
                    Log.d("NonRichTapThread", "updateParam interval:" + m174d + " pre interval:" + nonRichTapLooperInfo2.m174d() + " delta:" + m174d2 + " duration:" + m180a);
                    nonRichTapLooperInfo2.m177b(m174d);
                    nonRichTapLooperInfo2.m179a(m180a);
                }
                int m171f = nonRichTapLooperInfo.m171f();
                if (m171f != -1) {
                    nonRichTapLooperInfo2.m173d(m171f);
                }
                try {
                    this.f1532a.notify();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /* renamed from: c */
    public void m165c() {
        this.f1535d = true;
        synchronized (this.f1532a) {
            try {
                synchronized (this.f1533b) {
                    this.f1536e.clear();
                    this.f1536e = null;
                }
                this.f1532a.notify();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /* renamed from: d */
    boolean m164d() {
        synchronized (this.f1533b) {
            for (NonRichTapLooperInfo nonRichTapLooperInfo : this.f1536e) {
                if (nonRichTapLooperInfo.f1529g) {
                    return true;
                }
            }
            return false;
        }
    }

    @Override // java.lang.Thread, java.lang.Runnable
    public void run() {
        NonRichTapLooperInfo nonRichTapLooperInfo;
        String str = null;
        String str2 = null;
        String str3 = "NonRichTapThread";
        String str4 = "non richTap thread start!!";
        loop0: while (true) {
            Log.d(str3, str4);
            while (!this.f1535d) {
                List<NonRichTapLooperInfo> list = this.f1536e;
                if (list != null) {
                    if (list.isEmpty() || !m164d()) {
                        synchronized (this.f1532a) {
                            try {
                                synchronized (this.f1533b) {
                                    this.f1536e.clear();
                                }
                                Log.d("NonRichTapThread", "nothing is in list,just wait!!");
                                this.f1532a.wait();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    } else {
                        long m170a = m170a();
                        nonRichTapLooperInfo = this.f1536e.get(0);
                        if (nonRichTapLooperInfo.f1529g) {
                            if (nonRichTapLooperInfo.f1528f > m170a) {
                                long j = nonRichTapLooperInfo.f1528f - m170a;
                                synchronized (this.f1532a) {
                                    try {
                                        Log.d("NonRichTapThread", "go to sleep :" + j);
                                        this.f1532a.wait(j);
                                    } catch (Exception e2) {
                                        e2.printStackTrace();
                                    }
                                }
                                if (nonRichTapLooperInfo.f1531i > nonRichTapLooperInfo.m176c()) {
                                    str = "NonRichTapThread";
                                    str2 = " looper finished,remove it!!";
                                }
                            } else {
                                PatternHe.m162a(this.f1534c).mo143c(nonRichTapLooperInfo.m178b(), nonRichTapLooperInfo.m176c(), nonRichTapLooperInfo.m174d(), nonRichTapLooperInfo.m172e(), nonRichTapLooperInfo.m171f());
                                nonRichTapLooperInfo.f1531i++;
                                Log.d("NonRichTapThread", " vib mHasVibNum:" + nonRichTapLooperInfo.f1531i);
                                if (nonRichTapLooperInfo.f1531i >= nonRichTapLooperInfo.m176c()) {
                                    str = "NonRichTapThread";
                                    str2 = " wake up vib looper is end ,remove it!!";
                                }
                            }
                            Log.d(str, str2);
                            nonRichTapLooperInfo.f1529g = false;
                        } else {
                            continue;
                        }
                    }
                }
            }
            Log.d("NonRichTapThread", "non richTap thread quit!");
            return;
        }
    }
}
