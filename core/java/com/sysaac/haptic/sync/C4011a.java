package com.sysaac.haptic.sync;

import android.util.Log;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.p033b.p034a.C3961e;
import com.sysaac.haptic.p033b.p035b.C3962a;
import com.sysaac.haptic.p033b.p036c.C3964a;
import com.sysaac.haptic.player.CurrentPlayingInfo;
import java.io.ByteArrayOutputStream;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.sync.a */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class C4011a {

    /* renamed from: b */
    public static final long f1771b = -1;

    /* renamed from: c */
    public static final String f1772c = "PatternList";

    /* renamed from: d */
    public static final String f1773d = "PatternDesc";

    /* renamed from: e */
    public static final String f1774e = "Pattern";

    /* renamed from: f */
    public static final String f1775f = "AbsoluteTime";

    /* renamed from: g */
    public static final String f1776g = "Loop";

    /* renamed from: h */
    public static final String f1777h = "Interval";

    /* renamed from: i */
    public static final String f1778i = "Event";

    /* renamed from: j */
    public static final String f1779j = "Type";

    /* renamed from: k */
    public static final String f1780k = "Duration";

    /* renamed from: l */
    public static final String f1781l = "RelativeTime";

    /* renamed from: m */
    public static final String f1782m = "continuous";

    /* renamed from: n */
    public static final String f1783n = "transient";

    /* renamed from: o */
    public static final int f1784o = 48;

    /* renamed from: p */
    private static final String f1785p = "VibrationParser";

    /* renamed from: a */
    public boolean f1786a = false;

    /* renamed from: q */
    private long f1787q = -1;

    /* renamed from: r */
    private JSONArray f1788r;

    /* renamed from: s */
    private JSONObject f1789s;

    /* renamed from: t */
    private String f1790t;

    /* renamed from: u */
    private CurrentPlayingInfo f1791u;

    /* renamed from: v */
    private C3964a f1792v;

    public C4011a(FileDescriptor fileDescriptor) {
        this.f1788r = null;
        this.f1789s = null;
        try {
            String m11a = m11a(fileDescriptor);
            this.f1790t = m11a;
            if (this.f1786a) {
                Log.i("VibrationParser", "configured HE: " + m11a);
            }
            JSONObject jSONObject = new JSONObject(m11a);
            try {
                this.f1789s = jSONObject.getJSONObject("Pattern");
            } catch (JSONException e) {
                this.f1788r = jSONObject.getJSONArray("PatternList");
            }
        } catch (Exception e2) {
            Log.e("VibrationParser", e2.getMessage(), e2);
        }
    }

    public C4011a(String str) {
        this.f1788r = null;
        this.f1789s = null;
        try {
            this.f1790t = str;
            JSONObject jSONObject = new JSONObject(str);
            try {
                this.f1789s = jSONObject.getJSONObject("Pattern");
            } catch (JSONException e) {
                this.f1788r = jSONObject.getJSONArray("PatternList");
            }
        } catch (Exception e2) {
            Log.e("VibrationParser", e2.getMessage(), e2);
        }
    }

    public C4011a(String str, CurrentPlayingInfo currentPlayingInfo) {
        this.f1788r = null;
        this.f1789s = null;
        try {
            this.f1790t = str;
            JSONObject jSONObject = new JSONObject(str);
            try {
                this.f1789s = jSONObject.getJSONObject("Pattern");
            } catch (JSONException e) {
                this.f1788r = jSONObject.getJSONArray("PatternList");
            }
        } catch (Exception e2) {
            Log.e("VibrationParser", e2.getMessage(), e2);
        }
        this.f1791u = currentPlayingInfo;
        if (currentPlayingInfo.f1688i <= 0 || this.f1791u.f1687h == null) {
            return;
        }
        String m113a = Util.m113a(this.f1791u.f1680a, this.f1791u.f1688i);
        if (m113a != null && m113a.length() > 0) {
            this.f1792v = Util.m86g(Util.m113a(this.f1791u.f1680a, this.f1791u.f1688i));
        }
        if (!CurrentPlayingInfo.m80a(this.f1792v)) {
            return;
        }
        this.f1792v.f1512b.get(0).f1516a = this.f1791u.f1688i;
    }

    /* renamed from: a */
    private long m10a(JSONArray jSONArray) {
        long j = 0;
        JSONObject jSONObject = null;
        try {
            jSONObject = jSONArray.getJSONObject(jSONArray.length() - 1).getJSONObject("Event");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        String string = null;
        try {
            string = jSONObject.getString("Type");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        long j2 = 0;
        try {
            j2 = jSONObject.getInt("RelativeTime") + 0;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        if ("continuous".equals(string)) {
            try {
                j = jSONObject.getInt("Duration");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (!"transient".equals(string)) {
            return j2;
        } else {
            j = 48;
        }
        return j2 + j;
    }

    /* renamed from: a */
    private String m11a(FileDescriptor fileDescriptor) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        FileInputStream fileInputStream = new FileInputStream(fileDescriptor);
        byte[] bArr = new byte[4096];
        while (true) {
            int read = 0;
            try {
                read = fileInputStream.read(bArr);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (read == -1) {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return byteArrayOutputStream.toString();
            }
            byteArrayOutputStream.write(bArr, 0, read);
        }
    }

    /* renamed from: a */
    public long m13a() {
        String message;
        Exception exc;
        JSONObject jSONObject = null;
        long j;
        if (this.f1789s != null) {
            return -1L;
        }
        try {
            JSONArray jSONArray = this.f1788r;
            if (jSONArray != null) {
                int length = jSONArray.length();
                for (int i = 0; i < length; i++) {
                    try {
                        j = this.f1788r.getJSONObject(i).getLong("AbsoluteTime");
                    } catch (JSONException e) {
                        j = jSONObject.getJSONObject("PatternDesc").getLong("AbsoluteTime");
                    }
                    if (this.f1787q < j) {
                        return j;
                    }
                }
            }
        } catch (Exception e2) {
            message = e2.getMessage();
            exc = e2;
            Log.e("VibrationParser", message, exc);
            return -1L;
        }
        return -1L;
    }

    /* renamed from: a */
    public C4012b m12a(long j) {
        long j2 = 0;
        if (j < 0) {
            Log.i("VibrationParser", "timeUs shouldn't be less than 0, which means no media played!");
            return null;
        }
        this.f1787q = j;
        if (this.f1789s != null && this.f1788r == null) {
            return new C4012b("\"Pattern\":" + this.f1789s.toString(), 1, 0);
        }
        if (this.f1788r != null) {
            CurrentPlayingInfo currentPlayingInfo = this.f1791u;
            if (currentPlayingInfo != null && currentPlayingInfo.f1688i > 0 && CurrentPlayingInfo.m80a(this.f1792v) && this.f1792v.f1512b.get(0).f1516a >= j) {
                Log.d("VibrationParser", "use paused pattern!");
                return Util.m103b(this.f1792v);
            }
            int length = this.f1788r.length();
            int i = 0;
            while (i < length) {
                JSONObject jSONObject = null;
                try {
                    jSONObject = this.f1788r.getJSONObject(i);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                try {
                    j2 = jSONObject.getLong("AbsoluteTime");
                } catch (JSONException e) {
                    try {
                        j2 = jSONObject.getJSONObject("PatternDesc").getLong("AbsoluteTime");
                    } catch (JSONException ex) {
                        ex.printStackTrace();
                    }
                }
                if (j < j2) {
                    break;
                }
                i++;
            }
            if (i >= 1) {
                JSONArray jSONArray = null;
                try {
                    jSONArray = this.f1788r.getJSONObject(i - 1).getJSONArray("Pattern");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                return new C4012b("\"Pattern\":" + jSONArray.toString(), 1, 0);
            }
        }
        return null;
    }

    /* renamed from: b */
    public int m9b() {
        C3964a m86g;
        boolean z = -1 == m13a() && CurrentPlayingInfo.m80a(this.f1792v) && 1 == Util.m94c(this.f1792v);
        int m88e = Util.m88e(this.f1790t);
        if (1 == m88e) {
            C3962a m87f = Util.m87f(this.f1790t);
            if (!CurrentPlayingInfo.m80a(m87f)) {
                return 0;
            }
            return m87f.mo182b();
        } else if (2 != m88e) {
            return 0;
        } else {
            if (z) {
                Log.d("VibrationParser", "Utils.getHe20PatternCount(mRemainderHe20):" + Util.m94c(this.f1792v) + "\n getNextScheduledTimeMs():" + m13a() + "\n mRemainderHe20:" + Util.m116a(this.f1792v));
                m86g = this.f1792v;
            } else {
                m86g = Util.m86g(this.f1790t);
            }
            if (!CurrentPlayingInfo.m80a(m86g)) {
                return 0;
            }
            try {
                Iterator<C3961e> it = m86g.f1512b.get(m86g.f1512b.size() - 1).f1517b.iterator();
                int i = 0;
                while (it.hasNext()) {
                    C3961e next = it.next();
                    int i2 = next.f1505a.f1497a.equals("continuous") ? next.f1505a.f1498b + next.f1505a.f1499c : next.f1505a.f1498b + 48;
                    if (i2 > i) {
                        i = i2;
                    }
                }
                return i;
            } catch (Exception e) {
                e.printStackTrace();
                return 0;
            }
        }
    }
}
