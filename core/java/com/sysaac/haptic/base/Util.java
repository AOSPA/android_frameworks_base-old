package com.sysaac.haptic.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;
import com.sysaac.haptic.p033b.p034a.C3957a;
import com.sysaac.haptic.p033b.p034a.C3958b;
import com.sysaac.haptic.p033b.p034a.C3960d;
import com.sysaac.haptic.p033b.p034a.C3961e;
import com.sysaac.haptic.p033b.p034a.InterfaceC3959c;
import com.sysaac.haptic.p033b.p035b.C3962a;
import com.sysaac.haptic.p033b.p035b.C3963b;
import com.sysaac.haptic.p033b.p036c.C3964a;
import com.sysaac.haptic.p033b.p036c.C3965b;
import com.sysaac.haptic.p033b.p036c.C3966c;
import com.sysaac.haptic.player.CurrentPlayingInfo;
import com.sysaac.haptic.sync.C4012b;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Locale;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONStringer;

public class Util {

    /* renamed from: A */
    public static final int f1587A = 0;

    /* renamed from: B */
    public static final int f1588B = 0;

    /* renamed from: C */
    public static final int f1589C = 1;

    /* renamed from: D */
    public static final int f1590D = 2;

    /* renamed from: E */
    public static final int f1591E = 3;

    /* renamed from: F */
    public static final int f1592F = 4;

    /* renamed from: G */
    public static final int f1593G = 5;

    /* renamed from: H */
    public static final int f1594H = 6;

    /* renamed from: I */
    public static final int f1595I = 17;

    /* renamed from: J */
    public static final int f1596J = 55;

    /* renamed from: K */
    public static final int f1597K = 26;

    /* renamed from: L */
    public static final int f1598L = 29;

    /* renamed from: M */
    public static final int f1599M = 65;

    /* renamed from: N */
    public static final int f1600N = 50;

    /* renamed from: O */
    public static final int f1601O = 2;

    /* renamed from: P */
    public static final int f1602P = 1;

    /* renamed from: Q */
    public static final int f1603Q = 255;

    /* renamed from: R */
    public static final int f1604R = 0;

    /* renamed from: S */
    public static final int f1605S = 100;

    /* renamed from: T */
    public static final int f1606T = 30000;

    /* renamed from: U */
    public static final int f1607U = 17;

    /* renamed from: V */
    public static final int f1608V = 1;

    /* renamed from: W */
    public static final int f1609W = 100;

    /* renamed from: X */
    public static final String f1610X = "Metadata";

    /* renamed from: Y */
    public static final String f1611Y = "Version";

    /* renamed from: Z */
    public static final int f1612Z = 2;

    /* renamed from: a */
    public static final String f1613a = "{\n    \"Metadata\": {\n        \"Created\": \"2020-08-10\",\n        \"Description\": \"Haptic editor design\",\n        \"Version\": 2\n    },\n    \"PatternList\": [\n       {\n        \"AbsoluteTime\": 0,\n          ReplaceMe\n       }\n    ]\n}";

    /* renamed from: aa */
    public static final int f1614aa = 16;

    /* renamed from: ab */
    private static final String f1615ab = "Util";

    /* renamed from: ac */
    private static final String f1616ac = "swap_left_right";

    /* renamed from: ad */
    private static int f1617ad = 0;

    /* renamed from: ae */
    private static int f1618ae = 0;

    /* renamed from: af */
    private static boolean f1619af = false;

    /* renamed from: ag */
    private static int f1620ag = 0;

    /* renamed from: b */
    public static final boolean f1621b = false;

    /* renamed from: c */
    public static final String f1622c = ".he";

    /* renamed from: d */
    public static final String f1623d = "Pattern";

    /* renamed from: e */
    public static final String f1624e = "PatternList";

    /* renamed from: f */
    public static final String f1625f = "PatternDesc";

    /* renamed from: g */
    public static final String f1626g = "AbsoluteTime";

    /* renamed from: h */
    public static final String f1627h = "Index";

    /* renamed from: i */
    public static final String f1628i = "continuous";

    /* renamed from: j */
    public static final String f1629j = "transient";

    /* renamed from: k */
    public static final String f1630k = "Event";

    /* renamed from: l */
    public static final String f1631l = "RelativeTime";

    /* renamed from: m */
    public static final String f1632m = "Duration";

    /* renamed from: n */
    public static final String f1633n = "Type";

    /* renamed from: o */
    public static final String f1634o = "Parameters";

    /* renamed from: p */
    public static final String f1635p = "Intensity";

    /* renamed from: q */
    public static final String f1636q = "Frequency";

    /* renamed from: r */
    public static final String f1637r = "Curve";

    /* renamed from: s */
    public static final String f1638s = "Time";

    /* renamed from: t */
    public static final String f1639t = "Created";

    /* renamed from: u */
    public static final String f1640u = "Description";

    /* renamed from: v */
    public static final String f1641v = "Version";

    /* renamed from: w */
    public static final int f1642w = 16;

    /* renamed from: x */
    public static final int f1643x = 4096;

    /* renamed from: y */
    public static final int f1644y = 4097;

    /* renamed from: z */
    public static final int f1645z = 400;

    /* renamed from: a */
    public static int m122a() {
        return f1617ad;
    }

    /* renamed from: a */
    public static int m121a(int i) {
        f1617ad = i;
        return i;
    }

    /* renamed from: a */
    public static long m106a(byte[] bArr) {
        int i;
        int length = bArr.length;
        if (length == 1) {
            i = bArr[0] & 255;
        } else if (length == 2) {
            i = (short) ((bArr[0] & 255) | ((bArr[1] & 255) << 8));
        } else if (length != 4) {
            if (length != 8) {
                return 0L;
            }
            long j = bArr[7] & 255;
            long j2 = bArr[6] & 255;
            long j3 = bArr[5] & 255;
            long j4 = bArr[4] & 255;
            return (bArr[0] & 255) | ((bArr[2] & 255) << 16) | ((bArr[3] & 255) << 24) | (j << 56) | (j2 << 48) | (j3 << 40) | (j4 << 32) | ((bArr[1] & 255) << 8);
        } else {
            i = (bArr[0] & 255) | ((bArr[3] & 255) << 24) | ((bArr[2] & 255) << 16) | ((bArr[1] & 255) << 8);
        }
        return i;
    }

    /* renamed from: a */
    public static InterfaceC3959c m114a(String str) {
        switch (m88e(str)) {
            case 1:
                try {
                    return m87f(str);
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            case 2:
                try {
                    return m86g(str);
                } catch (Exception e2) {
                    e2.printStackTrace();
                    return null;
                }
            default:
                return null;
        }
    }

    /* renamed from: a */
    public static String m120a(int i, int i2) {
        return "{ \"Metadata\":{\"Created\": \"2021-01-01\",\"Description\": \"Haptic editor design, for getting transient signal\",        \"Version\": 1},\"Pattern\":[{    \"Event\": {       \"Parameters\": {           \"Frequency\": " + i2 + ",\"Intensity\": " + i + "       },       \"Type\": \"transient\",       \"RelativeTime\": 0   }}]}";
    }

    /* renamed from: a */
    public static String m117a(C3962a c3962a) {
        try {
            JSONStringer jSONStringer = new JSONStringer();
            jSONStringer.object();
            jSONStringer.key("Metadata").object().key("Created").value(c3962a.f1506a.f1509b).key("Description").value(c3962a.f1506a.f1510c).key("Version").value(c3962a.f1506a.f1508a).endObject();
            jSONStringer.key("Pattern").array();
            Iterator<C3961e> it = c3962a.f1507b.iterator();
            while (it.hasNext()) {
                C3961e next = it.next();
                jSONStringer.object();
                jSONStringer.key("Event").object().key("Type").value(next.f1505a.f1497a).key("RelativeTime").value(next.f1505a.f1498b);
                if ("continuous".equals(next.f1505a.f1497a)) {
                    jSONStringer.key("Duration").value(next.f1505a.f1499c);
                }
                jSONStringer.key("Parameters").object().key("Frequency").value(next.f1505a.f1501e.f1503b).key("Intensity").value(next.f1505a.f1501e.f1502a);
                if ("continuous".equals(next.f1505a.f1497a)) {
                    jSONStringer.key("Curve").array();
                    Iterator<C3957a> it2 = next.f1505a.f1501e.f1504c.iterator();
                    while (it2.hasNext()) {
                        C3957a next2 = it2.next();
                        jSONStringer.object().key("Frequency").value(next2.f1496c).key("Intensity").value(next2.f1495b).key("Time").value(next2.f1494a).endObject();
                    }
                    jSONStringer.endArray();
                }
                jSONStringer.endObject().endObject().endObject();
            }
            jSONStringer.endArray().endObject();
            return jSONStringer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* renamed from: a */
    public static String m116a(C3964a c3964a) {
        try {
            JSONStringer jSONStringer = new JSONStringer();
            jSONStringer.object();
            jSONStringer.key("Metadata").object().key("Created").value(c3964a.f1511a.f1514b).key("Description").value(c3964a.f1511a.f1515c).key("Version").value(c3964a.f1511a.f1513a).endObject();
            jSONStringer.key("PatternList").array();
            Iterator<C3966c> it = c3964a.f1512b.iterator();
            while (it.hasNext()) {
                C3966c next = it.next();
                jSONStringer.object().key("AbsoluteTime").value(next.f1516a).key("Pattern").array();
                Iterator<C3961e> it2 = next.f1517b.iterator();
                while (it2.hasNext()) {
                    C3961e next2 = it2.next();
                    jSONStringer.object();
                    jSONStringer.key("Event").object().key("Index").value(next2.f1505a.f1500d).key("RelativeTime").value(next2.f1505a.f1498b).key("Type").value(next2.f1505a.f1497a);
                    if ("continuous".equals(next2.f1505a.f1497a)) {
                        jSONStringer.key("Duration").value(next2.f1505a.f1499c);
                    }
                    jSONStringer.key("Parameters").object().key("Frequency").value(next2.f1505a.f1501e.f1503b).key("Intensity").value(next2.f1505a.f1501e.f1502a);
                    if ("continuous".equals(next2.f1505a.f1497a)) {
                        jSONStringer.key("Curve").array();
                        Iterator<C3957a> it3 = next2.f1505a.f1501e.f1504c.iterator();
                        while (it3.hasNext()) {
                            C3957a next3 = it3.next();
                            jSONStringer.object().key("Frequency").value(next3.f1496c).key("Intensity").value(next3.f1495b).key("Time").value(next3.f1494a).endObject();
                        }
                        jSONStringer.endArray();
                    }
                    jSONStringer.endObject().endObject().endObject();
                }
                jSONStringer.endArray().endObject();
            }
            jSONStringer.endArray().endObject();
            return jSONStringer.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* renamed from: a */
    public static String m113a(String str, int i) {
        switch (m88e(str)) {
            case 1:
                return m92c(str, i);
            case 2:
                return m89d(str, i);
            default:
                return "";
        }
    }

    /* renamed from: a */
    private static ArrayList<C3957a> m110a(ArrayList<C3957a> arrayList) {
        if (arrayList == null || arrayList.size() == 0) {
            return null;
        }
        int size = arrayList.size();
        if (size > 0 && size <= 4) {
            return arrayList;
        }
        C3957a c3957a = new C3957a();
        int i = size - 2;
        int i2 = i / 2;
        for (int i3 = 1; i3 <= i2; i3++) {
            c3957a.f1494a += arrayList.get(i3).f1494a;
            c3957a.f1495b += arrayList.get(i3).f1495b;
            c3957a.f1496c += arrayList.get(i3).f1496c;
        }
        c3957a.f1494a /= i2;
        c3957a.f1495b /= i2;
        c3957a.f1495b = Math.round(c3957a.f1495b * 10.0d) / 10.0d;
        c3957a.f1496c /= i2;
        C3957a c3957a2 = new C3957a();
        for (int i4 = i2 + 1; i4 <= i; i4++) {
            c3957a2.f1494a += arrayList.get(i4).f1494a;
            c3957a2.f1495b += arrayList.get(i4).f1495b;
            c3957a2.f1496c += arrayList.get(i4).f1496c;
        }
        int i5 = i - i2;
        c3957a2.f1494a /= i5;
        c3957a2.f1495b /= i5;
        c3957a2.f1495b = Math.round(c3957a2.f1495b * 10.0d) / 10.0d;
        c3957a2.f1496c /= i5;
        arrayList.subList(1, size - 1).clear();
        arrayList.add(1, c3957a);
        arrayList.add(2, c3957a2);
        return arrayList;
    }

    /* renamed from: a */
    public static void m111a(String str, ArrayList<Long> arrayList, ArrayList<Integer> arrayList2) {
        String str2;
        String str3;
        int i;
        String str4;
        String str5;
        Iterator<C3966c> it2;
        if (str == null || str.length() == 0 || arrayList == null || arrayList2 == null) {
            Log.e("Util", "convertHeStringToWaveformParams(), invalid parameters.");
            return;
        }
        InterfaceC3959c m114a = m114a(str);
        if (!CurrentPlayingInfo.m80a(m114a)) {
            return;
        }
        arrayList.clear();
        arrayList2.clear();
        long j = 0;
        arrayList.add(new Long(0L));
        int i2 = 0;
        arrayList2.add(0);
        String str6 = "continuous";
        String str7 = "transient";
        switch (m114a.mo183a()) {
            case 1:
                String str8 = str6;
                String str9 = str7;
                Iterator<C3961e> it = ((C3962a) m114a).f1507b.iterator();
                while (it.hasNext()) {
                    C3961e next = it.next();
                    if (next == null || next.f1505a == null || next.f1505a.f1501e == null) {
                        str2 = str8;
                        str3 = str9;
                    } else {
                        str3 = str9;
                        if (str3.equals(next.f1505a.f1497a)) {
                            if (next.f1505a.f1498b > j) {
                                arrayList.add(Long.valueOf(new Long(next.f1505a.f1498b).longValue() - j));
                                arrayList2.add(0);
                                j += arrayList.get(arrayList.size() - 1).longValue();
                            }
                            arrayList.add(new Long(65L));
                            arrayList2.add(Integer.valueOf((int) (((next.f1505a.f1501e.f1502a * 1.0d) / 100.0d) * 255.0d)));
                            j += arrayList.get(arrayList.size() - 1).longValue();
                            str2 = str8;
                        } else {
                            str2 = str8;
                            if (str2.equals(next.f1505a.f1497a)) {
                                if (next.f1505a.f1498b > j) {
                                    arrayList.add(Long.valueOf(new Long(next.f1505a.f1498b).longValue() - j));
                                    i = 0;
                                    arrayList2.add(0);
                                    j += arrayList.get(arrayList.size() - 1).longValue();
                                } else {
                                    i = 0;
                                }
                                if (next.f1505a.f1501e.f1504c != null && 4 <= next.f1505a.f1501e.f1504c.size()) {
                                    int i3 = i;
                                    while (i3 < next.f1505a.f1501e.f1504c.size() - 2) {
                                        int i4 = i3 + 1;
                                        arrayList.add(new Long(next.f1505a.f1501e.f1504c.get(i4).f1494a - next.f1505a.f1501e.f1504c.get(i3).f1494a));
                                        arrayList2.add(Integer.valueOf((int) (((next.f1505a.f1501e.f1504c.get(i4).f1495b + next.f1505a.f1501e.f1504c.get(i3).f1495b) / 2.0d) * 255.0d)));
                                        j += arrayList.get(arrayList.size() - 1).longValue();
                                        i3 = i4;
                                    }
                                }
                                str9 = str3;
                                str8 = str2;
                            } else {
                                Log.e("Util", "unknown type!");
                            }
                        }
                    }
                    str9 = str3;
                    str8 = str2;
                }
                return;
            case 2:
                Iterator<C3966c> it4 = ((C3964a) m114a).f1512b.iterator();
                while (it4.hasNext()) {
                    C3966c next2 = it4.next();
                    Iterator<C3961e> it5 = next2.f1517b.iterator();
                    while (it5.hasNext()) {
                        C3961e next3 = it5.next();
                        if (next3 != null && next3.f1505a != null && next3.f1505a.f1501e != null) {
                            if (str7.equals(next3.f1505a.f1497a)) {
                                if (next3.f1505a.f1498b + next2.f1516a > j) {
                                    arrayList.add(Long.valueOf(new Long(next3.f1505a.f1498b + next2.f1516a).longValue() - j));
                                    arrayList2.add(Integer.valueOf(i2));
                                    j += arrayList.get(arrayList.size() - 1).longValue();
                                }
                                arrayList.add(new Long(65L));
                                arrayList2.add(Integer.valueOf((int) (((next3.f1505a.f1501e.f1502a * 255) * 1.0f) / 100.0f)));
                                j += arrayList.get(arrayList.size() - 1).longValue();
                            } else if (str6.equals(next3.f1505a.f1497a)) {
                                if (next3.f1505a.f1498b + next2.f1516a > j) {
                                    arrayList.add(Long.valueOf(new Long(next3.f1505a.f1498b).longValue() - j));
                                    arrayList2.add(Integer.valueOf(i2));
                                    j += arrayList.get(arrayList.size() - 1).longValue();
                                }
                                if (next3.f1505a.f1501e.f1504c != null && 4 <= next3.f1505a.f1501e.f1504c.size()) {
                                    int i5 = i2;
                                    while (i5 < next3.f1505a.f1501e.f1504c.size() - 2) {
                                        int i6 = i5 + 1;
                                        arrayList.add(new Long(next3.f1505a.f1501e.f1504c.get(i6).f1494a - next3.f1505a.f1501e.f1504c.get(i5).f1494a));
                                        arrayList2.add(Integer.valueOf((int) (((((next3.f1505a.f1501e.f1504c.get(i6).f1495b + next3.f1505a.f1501e.f1504c.get(i5).f1495b) * 0.5d) * next3.f1505a.f1501e.f1502a) / 100.0d) * 255.0d)));
                                        j += arrayList.get(arrayList.size() - 1).longValue();
                                        i5 = i6;
                                        it4 = it4;
                                        str6 = str6;
                                        str7 = str7;
                                    }
                                }
                            } else {
                                it2 = it4;
                                str5 = str6;
                                str4 = str7;
                                Log.e("Util", "unknown type!");
                                it4 = it2;
                                str6 = str5;
                                str7 = str4;
                                i2 = 0;
                            }
                        }
                        it2 = it4;
                        str5 = str6;
                        str4 = str7;
                        it4 = it2;
                        str6 = str5;
                        str7 = str4;
                        i2 = 0;
                    }
                }
                return;
            default:
                return;
        }
    }

    /* renamed from: a */
    public static void m108a(boolean z) {
        f1619af = z;
    }

    /* renamed from: a */
    public static void m107a(boolean z, Context context) {
        if (context == null) {
            Log.e("Util", "swapLR null==context");
            return;
        }
        SharedPreferences.Editor edit = context.getSharedPreferences("swap_left_right", 0).edit();
        edit.putBoolean("swap_left_right", z);
        edit.commit();
    }

    /* renamed from: a */
    public static boolean m118a(Context context) {
        if (context == null) {
            Log.e("Util", "isLRSwapped null==context");
            return false;
        }
        return context.getSharedPreferences("swap_left_right", 0).getBoolean("swap_left_right", false);
    }

    /* renamed from: a */
    public static boolean m115a(File file) {
        return m112a(file.getPath(), ".he");
    }

    /* renamed from: a */
    public static boolean m112a(String str, String str2) {
        if (!m99b(str, str2)) {
            return false;
        }
        return new File(str).exists();
    }

    /* renamed from: a */
    public static byte[] m119a(long j) {
        return new byte[]{(byte) (j & 255), (byte) ((j >> 8) & 255), (byte) ((j >> 16) & 255), (byte) ((j >> 24) & 255), (byte) ((j >> 32) & 255), (byte) ((j >> 40) & 255), (byte) ((j >> 48) & 255), (byte) ((j >> 56) & 255)};
    }

    /* renamed from: a */
    public static byte[] m109a(short s) {
        return new byte[]{(byte) (s & 255), (byte) ((s >> 8) & 255)};
    }

    /* renamed from: b */
    public static int m105b() {
        return f1618ae;
    }

    /* renamed from: b */
    public static int m104b(int i) {
        f1618ae = i;
        return i;
    }

    /* renamed from: b */
    public static C4012b m103b(C3964a c3964a) {
        if (!CurrentPlayingInfo.m80a(c3964a)) {
            return null;
        }
        C3964a c3964a2 = new C3964a();
        c3964a2.f1511a = new C3965b();
        c3964a2.f1512b = new ArrayList<>();
        c3964a2.f1512b.add(c3964a.f1512b.get(0));
        StringBuilder sb = new StringBuilder(m116a(c3964a2));
        return new C4012b(sb.substring(sb.indexOf("\"Pattern\""), sb.lastIndexOf("}", sb.lastIndexOf("}"))), 1, 0);
    }

    /* renamed from: b */
    public static String m102b(File file) {
        if (file == null || !file.exists()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = null;
        try {
            try {
                try {
                    BufferedReader bufferedReader2 = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    while (true) {
                        try {
                            String readLine = bufferedReader2.readLine();
                            if (readLine == null) {
                                break;
                            }
                            sb.append(readLine);
                        } catch (Exception e) {
                            e = e;
                            bufferedReader = bufferedReader2;
                            e.printStackTrace();
                            bufferedReader.close();
                            return sb.toString();
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader = bufferedReader2;
                            try {
                                bufferedReader.close();
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                            throw th;
                        }
                    }
                    bufferedReader2.close();
                } catch (Throwable th2) {
                }
            } catch (Exception e3) {
            }
        } catch (Exception e4) {
            e4.printStackTrace();
        }
        return sb.toString();
    }

    /* renamed from: b */
    public static String m101b(String str) {
        C3962a c3962a;
        try {
            c3962a = m87f(str);
        } catch (Exception e) {
            e.printStackTrace();
            c3962a = null;
        }
        if (c3962a == null || c3962a.f1507b == null || c3962a.f1507b.size() == 0) {
            Log.w("Util", "pause_start_seek, convertHe10ToHe20, invalid HE1.0 string!");
            return "";
        }
        C3964a c3964a = new C3964a();
        c3964a.f1511a = new C3965b();
        c3964a.f1512b = new ArrayList<>();
        C3966c c3966c = new C3966c();
        c3966c.f1517b = c3962a.f1507b;
        c3966c.f1516a = 0;
        c3964a.f1512b.add(c3966c);
        return m116a(c3964a);
    }

    /* renamed from: b */
    public static String m100b(String str, int i) {
        if (i == 0) {
            return str;
        }
        C3964a m86g = m86g(str);
        if (!CurrentPlayingInfo.m80a(m86g)) {
            return str;
        }
        Iterator<C3966c> it = m86g.f1512b.iterator();
        while (it.hasNext()) {
            Iterator<C3961e> it2 = it.next().f1517b.iterator();
            while (it2.hasNext()) {
                C3961e next = it2.next();
                next.f1505a.f1501e.f1503b += i;
                int i2 = 0;
                if ("transient".equals(next.f1505a.f1497a)) {
                    if (m122a() >= 24) {
                        i2 = 150;
                        if (next.f1505a.f1501e.f1503b <= 150) {
                            i2 = -50;
                            if (next.f1505a.f1501e.f1503b < -50) {
                            }
                        }
                        next.f1505a.f1501e.f1503b = i2;
                    } else if (next.f1505a.f1501e.f1503b > 100) {
                        next.f1505a.f1501e.f1503b = 100;
                    } else if (next.f1505a.f1501e.f1503b < 0) {
                        next.f1505a.f1501e.f1503b = i2;
                    }
                } else if ("continuous".equals(next.f1505a.f1497a)) {
                    if (next.f1505a.f1501e.f1503b > 100) {
                        next.f1505a.f1501e.f1503b = 100;
                    } else if (next.f1505a.f1501e.f1503b < 0) {
                        next.f1505a.f1501e.f1503b = i2;
                    }
                }
            }
        }
        return m116a(m86g);
    }

    /* renamed from: b */
    public static void m98b(String str, ArrayList<Long> arrayList, ArrayList<Integer> arrayList2) {
        int i = 0;
        int i2 = 0;
        if (str == null || str.length() == 0 || arrayList == null || arrayList2 == null) {
            Log.e("Util", "convertM2VHeStringToWaveformParams(), invalid parameters.");
            return;
        }
        InterfaceC3959c m114a = m114a(str);
        if (!CurrentPlayingInfo.m80a(m114a)) {
            return;
        }
        arrayList.clear();
        arrayList2.clear();
        long j = 0;
        arrayList.add(new Long(0L));
        arrayList2.add(0);
        switch (m114a.mo183a()) {
            case 1:
                Log.i("Util", "convertM2VHeStringToWaveformParams, HE VERSION == 1, NOT A M2V HE, do nothing!");
                break;
            case 2:
                Iterator<C3966c> it = ((C3964a) m114a).f1512b.iterator();
                while (it.hasNext()) {
                    C3966c next = it.next();
                    Iterator<C3961e> it2 = next.f1517b.iterator();
                    while (it2.hasNext()) {
                        C3961e next2 = it2.next();
                        if (next2 != null && next2.f1505a != null && next2.f1505a.f1501e != null && 2 != next2.f1505a.f1500d) {
                            if ("transient".equals(next2.f1505a.f1497a)) {
                                if (next2.f1505a.f1498b + next.f1516a > j) {
                                    arrayList.add(Long.valueOf(new Long(next2.f1505a.f1498b + next.f1516a).longValue() - j));
                                    arrayList2.add(0);
                                    j += arrayList.get(arrayList.size() - 1).longValue();
                                }
                                arrayList.add(100 == next2.f1505a.f1501e.f1502a ? new Long(75L) : new Long(30L));
                                i = 255;
                            } else if ("continuous".equals(next2.f1505a.f1497a)) {
                                if (next2.f1505a.f1498b + next.f1516a > j) {
                                    arrayList.add(Long.valueOf(new Long(next2.f1505a.f1498b + next.f1516a).longValue() - j));
                                    arrayList2.add(0);
                                    j += arrayList.get(arrayList.size() - 1).longValue();
                                }
                                if (next2.f1505a.f1501e.f1504c != null && 4 <= next2.f1505a.f1501e.f1504c.size()) {
                                    arrayList.add(new Long(next2.f1505a.f1499c));
                                    if (4 == next2.f1505a.f1501e.f1504c.size()) {
                                        i2 = 153;
                                    } else if (6 == next2.f1505a.f1501e.f1504c.size()) {
                                        i = 255;
                                    } else {
                                        i2 = 127;
                                    }
                                    i = Integer.valueOf(i2);
                                }
                            } else {
                                Log.e("Util", "unknown type!");
                            }
                            arrayList2.add(i);
                            j += arrayList.get(arrayList.size() - 1).longValue();
                        }
                    }
                }
                break;
        }
        arrayList.remove(0);
        arrayList2.remove(0);
    }

    /* renamed from: b */
    public static boolean m99b(String str, String str2) {
        if (TextUtils.isEmpty(str) || TextUtils.isEmpty(str.trim())) {
            return false;
        }
        return str.trim().toLowerCase(Locale.getDefault()).endsWith(str2);
    }

    /* renamed from: c */
    public static int m94c(C3964a c3964a) {
        if (CurrentPlayingInfo.m80a(c3964a)) {
            return c3964a.f1512b.size();
        }
        return -1;
    }

    /* renamed from: c */
    public static String m93c(String str) {
        C3964a c3964a;
        try {
            c3964a = m86g(str);
        } catch (Exception e) {
            e.printStackTrace();
            c3964a = null;
        }
        if (!CurrentPlayingInfo.m80a(c3964a)) {
            Log.w("Util", "pause_start_seek, cutUpLongPatternOfHe20String, invalid HE2.0 string!");
            return "";
        }
        C3964a c3964a2 = new C3964a();
        c3964a2.f1511a = c3964a.f1511a;
        c3964a2.f1512b = new ArrayList<>();
        Iterator<C3966c> it = c3964a.f1512b.iterator();
        while (it.hasNext()) {
            C3966c next = it.next();
            if (next.f1517b.size() != 0) {
                if (1 == next.f1517b.size()) {
                    c3964a2.f1512b.add(next);
                } else if (1 < next.f1517b.size()) {
                    Iterator<C3961e> it2 = next.f1517b.iterator();
                    while (it2.hasNext()) {
                        C3961e next2 = it2.next();
                        C3966c c3966c = new C3966c();
                        c3966c.f1516a = next.f1516a + next2.f1505a.f1498b;
                        c3966c.f1517b = new ArrayList<>();
                        c3966c.f1517b.add(next2);
                        c3966c.f1517b.get(0).f1505a.f1498b = 0;
                        c3964a2.f1512b.add(c3966c);
                    }
                }
            }
        }
        return m116a(c3964a2);
    }

    /* renamed from: c */
    private static String m92c(String str, int i) {
        C3962a c3962a;
        try {
            c3962a = m87f(str);
        } catch (Exception e) {
            e.printStackTrace();
            c3962a = null;
        }
        if (c3962a == null || c3962a.f1507b == null || c3962a.f1507b.size() == 0) {
            Log.w("Util", "pause_start_seek generatePartialHe10String, source HE invalid!");
            return "";
        }
        int i2 = -1;
        Iterator<C3961e> it = c3962a.f1507b.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            C3961e next = it.next();
            if (next.f1505a != null && next.f1505a.f1498b >= i) {
                i2 = c3962a.f1507b.indexOf(next);
                break;
            }
        }
        if (i2 < 0) {
            return "";
        }
        c3962a.f1507b.subList(0, i2).clear();
        Iterator<C3961e> it2 = c3962a.f1507b.iterator();
        while (it2.hasNext()) {
            C3961e next2 = it2.next();
            if (next2.f1505a != null) {
                next2.f1505a.f1498b -= i;
            }
        }
        return m117a(c3962a);
    }

    /* renamed from: c */
    public static void m91c(String str, String str2) {
        FileOutputStream fileOutputStream = null;
        Throwable th;
        Exception e;
        try {
            try {
                try {
                    fileOutputStream = new FileOutputStream(new File(str));
                    try {
                        fileOutputStream.write(str2.getBytes());
                        fileOutputStream.close();
                    } catch (Exception e2) {
                        e = e2;
                        Log.e("Util", e.toString());
                        fileOutputStream.close();
                    }
                } catch (Throwable th2) {
                    th = th2;
                    try {
                        fileOutputStream.close();
                    } catch (Exception e3) {
                        e3.printStackTrace();
                    }
                    throw th;
                }
            } catch (Exception e4) {
                fileOutputStream = null;
                e = e4;
            } catch (Throwable th3) {
                fileOutputStream = null;
                th = th3;
                fileOutputStream.close();
            }
        } catch (Exception e5) {
            e5.printStackTrace();
        }
    }

    /* renamed from: c */
    public static boolean m96c() {
        return f1619af;
    }

    /* renamed from: c */
    public static byte[] m95c(int i) {
        return new byte[]{(byte) (i & 255), (byte) ((i >> 8) & 255), (byte) ((i >> 16) & 255), (byte) ((i >> 24) & 255)};
    }

    /* renamed from: d */
    public static String m90d(String str) {
        C3964a c3964a;
        try {
            c3964a = m86g(str);
        } catch (Exception e) {
            e.printStackTrace();
            c3964a = null;
        }
        if (!CurrentPlayingInfo.m80a(c3964a)) {
            Log.w("Util", "pause_start_seek, trim16pTo4p, invalid HE2.0 string!");
            return "";
        }
        Iterator<C3966c> it = c3964a.f1512b.iterator();
        while (it.hasNext()) {
            Iterator<C3961e> it2 = it.next().f1517b.iterator();
            while (it2.hasNext()) {
                C3961e next = it2.next();
                next.f1505a.f1501e.f1504c = m110a(next.f1505a.f1501e.f1504c);
                if (next.f1505a.f1497a.equals("transient")) {
                    if (next.f1505a.f1501e.f1503b < 0) {
                        next.f1505a.f1501e.f1503b = 0;
                    } else if (next.f1505a.f1501e.f1503b > 100) {
                        next.f1505a.f1501e.f1503b = 100;
                    }
                }
            }
        }
        return m116a(c3964a);
    }

    /* renamed from: d */
    private static String m89d(String str, int i) {
        C3964a c3964a;
        int i2;
        int i3;
        try {
            c3964a = m86g(str);
        } catch (Exception e) {
            e.printStackTrace();
            c3964a = null;
        }
        if (c3964a == null || c3964a.f1512b == null || c3964a.f1512b.size() == 0) {
            Log.w("Util", "pause_start_seek generatePartialHe20String, source HE invalid!");
            return "";
        }
        Iterator<C3966c> it = c3964a.f1512b.iterator();
        loop0: while (true) {
            i2 = -1;
            if (!it.hasNext()) {
                i3 = -1;
                break;
            }
            C3966c next = it.next();
            if (next.f1517b != null) {
                Iterator<C3961e> it2 = next.f1517b.iterator();
                while (it2.hasNext()) {
                    C3961e next2 = it2.next();
                    if (next2.f1505a != null && next2.f1505a.f1498b + next.f1516a >= i) {
                        int indexOf = next.f1517b.indexOf(next2);
                        i2 = c3964a.f1512b.indexOf(next);
                        i3 = indexOf;
                        break loop0;
                    }
                }
                continue;
            }
        }
        if (i2 < 0 || i3 < 0) {
            return "";
        }
        c3964a.f1512b.subList(0, i2).clear();
        c3964a.f1512b.get(0).f1517b.subList(0, i3).clear();
        Iterator<C3966c> it3 = c3964a.f1512b.iterator();
        while (it3.hasNext()) {
            C3966c next3 = it3.next();
            if (next3.f1517b != null) {
                if (next3.f1516a < i) {
                    Iterator<C3961e> it4 = next3.f1517b.iterator();
                    while (it4.hasNext()) {
                        C3961e next4 = it4.next();
                        if (next4.f1505a != null) {
                            next4.f1505a.f1498b = (next4.f1505a.f1498b + next3.f1516a) - i;
                        }
                    }
                    next3.f1516a = 0;
                } else {
                    next3.f1516a -= i;
                }
            }
        }
        return m116a(c3964a);
    }

    /* renamed from: e */
    public static int m88e(String str) {
        try {
            return new JSONObject(str).getJSONObject("Metadata").getInt("Version");
        } catch (Exception e) {
            Log.e("Util", "getHeVersion ERROR, heString:" + str);
            e.printStackTrace();
            return 0;
        }
    }

    /* renamed from: f */
    public static C3962a m87f(String str) {
        if (1 != m88e(str)) {
            return null;
        }
        try {
            JSONObject jSONObject = new JSONObject(str);
            C3962a c3962a = new C3962a();
            c3962a.f1506a = new C3963b();
            c3962a.f1507b = new ArrayList<>();
            JSONArray jSONArray = jSONObject.getJSONArray("Pattern");
            for (int i = 0; i < jSONArray.length(); i++) {
                C3961e c3961e = new C3961e();
                c3961e.f1505a = new C3958b();
                JSONObject jSONObject2 = ((JSONObject) jSONArray.get(i)).getJSONObject("Event");
                c3961e.f1505a.f1497a = jSONObject2.getString("Type");
                if ("continuous".equals(c3961e.f1505a.f1497a)) {
                    c3961e.f1505a.f1499c = jSONObject2.getInt("Duration");
                }
                c3961e.f1505a.f1498b = jSONObject2.getInt("RelativeTime");
                JSONObject jSONObject3 = jSONObject2.getJSONObject("Parameters");
                c3961e.f1505a.f1501e = new C3960d();
                c3961e.f1505a.f1501e.f1503b = jSONObject3.getInt("Frequency");
                c3961e.f1505a.f1501e.f1502a = jSONObject3.getInt("Intensity");
                c3961e.f1505a.f1501e.f1504c = new ArrayList<>();
                if ("continuous".equals(c3961e.f1505a.f1497a)) {
                    JSONArray jSONArray2 = jSONObject3.getJSONArray("Curve");
                    for (int i2 = 0; i2 < jSONArray2.length(); i2++) {
                        JSONObject jSONObject4 = (JSONObject) jSONArray2.get(i2);
                        C3957a c3957a = new C3957a();
                        c3957a.f1496c = jSONObject4.getInt("Frequency");
                        c3957a.f1495b = jSONObject4.getDouble("Intensity");
                        c3957a.f1494a = jSONObject4.getInt("Time");
                        c3961e.f1505a.f1501e.f1504c.add(c3957a);
                    }
                }
                c3962a.f1507b.add(c3961e);
            }
            return c3962a;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* renamed from: g */
    public static C3964a m86g(String str) {
        if (2 != m88e(str)) {
            return null;
        }
        try {
            JSONObject jSONObject = new JSONObject(str);
            C3964a c3964a = new C3964a();
            c3964a.f1511a = new C3965b();
            c3964a.f1512b = new ArrayList<>();
            JSONArray jSONArray = jSONObject.getJSONArray("PatternList");
            int i = 0;
            while (i < jSONArray.length()) {
                JSONObject jSONObject2 = (JSONObject) jSONArray.get(i);
                C3966c c3966c = new C3966c();
                c3966c.f1516a = jSONObject2.getInt("AbsoluteTime");
                c3966c.f1517b = new ArrayList<>();
                JSONArray jSONArray2 = jSONObject2.getJSONArray("Pattern");
                int i2 = 0;
                while (i2 < jSONArray2.length()) {
                    C3961e c3961e = new C3961e();
                    c3961e.f1505a = new C3958b();
                    JSONObject jSONObject3 = ((JSONObject) jSONArray2.get(i2)).getJSONObject("Event");
                    c3961e.f1505a.f1497a = jSONObject3.getString("Type");
                    if ("continuous".equals(c3961e.f1505a.f1497a)) {
                        c3961e.f1505a.f1499c = jSONObject3.getInt("Duration");
                    }
                    c3961e.f1505a.f1498b = jSONObject3.getInt("RelativeTime");
                    c3961e.f1505a.f1500d = jSONObject3.getInt("Index");
                    JSONObject jSONObject4 = jSONObject3.getJSONObject("Parameters");
                    c3961e.f1505a.f1501e = new C3960d();
                    c3961e.f1505a.f1501e.f1503b = jSONObject4.getInt("Frequency");
                    c3961e.f1505a.f1501e.f1502a = jSONObject4.getInt("Intensity");
                    c3961e.f1505a.f1501e.f1504c = new ArrayList<>();
                    if ("continuous".equals(c3961e.f1505a.f1497a)) {
                        JSONArray jSONArray3 = jSONObject4.getJSONArray("Curve");
                        int i3 = 0;
                        while (i3 < jSONArray3.length()) {
                            JSONObject jSONObject5 = (JSONObject) jSONArray3.get(i3);
                            C3957a c3957a = new C3957a();
                            c3957a.f1496c = jSONObject5.getInt("Frequency");
                            c3957a.f1495b = jSONObject5.getDouble("Intensity");
                            c3957a.f1494a = jSONObject5.getInt("Time");
                            c3961e.f1505a.f1501e.f1504c.add(c3957a);
                            i3++;
                            c3964a = c3964a;
                        }
                    }
                    c3966c.f1517b.add(c3961e);
                    i2++;
                    c3964a = c3964a;
                }
                C3964a c3964a2 = c3964a;
                c3964a2.f1512b.add(c3966c);
                i++;
                c3964a = c3964a2;
            }
            return c3964a;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* renamed from: h */
    public static String m85h(String str) {
        int i;
        m88e(str);
        C3964a m86g = m86g(str);
        if (!CurrentPlayingInfo.m80a(m86g)) {
            return str;
        }
        try {
            Iterator<C3966c> it = m86g.f1512b.iterator();
            while (it.hasNext()) {
                C3966c next = it.next();
                if (next.f1517b != null) {
                    Iterator<C3961e> it2 = next.f1517b.iterator();
                    while (it2.hasNext()) {
                        if (2 == it2.next().f1505a.f1500d) {
                            it2.remove();
                        }
                    }
                    if (1 != next.f1517b.size()) {
                        Collections.sort(next.f1517b, new C3985s());
                        int i2 = 0;
                        int i3 = 0;
                        int i4 = 1;
                        for (int i5 = 1; i3 <= next.f1517b.size() - i5 && i4 <= next.f1517b.size() - i5; i5 = 1) {
                            C3958b c3958b = next.f1517b.get(i3).f1505a;
                            C3958b c3958b2 = next.f1517b.get(i4).f1505a;
                            int i6 = "transient".equals(c3958b.f1497a) ? 48 : c3958b.f1499c;
                            int i7 = "transient".equals(c3958b2.f1497a) ? 48 : c3958b2.f1499c;
                            if (c3958b2.f1498b < c3958b.f1498b + i6) {
                                if (!"continuous".equals(c3958b2.f1497a)) {
                                    i = i2;
                                    c3958b2.f1500d = -1;
                                } else if (c3958b2.f1498b + i7 <= c3958b.f1498b + i6) {
                                    c3958b2.f1500d = -1;
                                    i4++;
                                    i = i2;
                                } else {
                                    int i8 = (c3958b2.f1498b + i7) - (c3958b.f1498b + i6);
                                    if (i8 <= 48) {
                                        c3958b2.f1500d = -1;
                                        i = i2;
                                    } else {
                                        ArrayList<C3957a> arrayList = new ArrayList<>();
                                        C3957a c3957a = new C3957a();
                                        c3957a.f1494a = i2;
                                        c3957a.f1495b = 0.0d;
                                        c3957a.f1496c = i2;
                                        C3957a c3957a2 = new C3957a();
                                        c3957a2.f1494a = i8 / 3;
                                        c3957a2.f1495b = 1.0d;
                                        c3957a2.f1496c = i2;
                                        C3957a c3957a3 = new C3957a();
                                        c3957a3.f1494a = (i8 / 3) * 2;
                                        c3957a3.f1495b = 1.0d;
                                        i = 0;
                                        c3957a3.f1496c = 0;
                                        C3957a c3957a4 = new C3957a();
                                        c3957a4.f1494a = i8;
                                        c3957a4.f1495b = 0.0d;
                                        c3957a4.f1496c = 0;
                                        arrayList.add(c3957a);
                                        arrayList.add(c3957a2);
                                        arrayList.add(c3957a3);
                                        arrayList.add(c3957a4);
                                        c3958b2.f1499c = i8;
                                        c3958b2.f1498b = c3958b.f1498b + i6;
                                        c3958b2.f1501e.f1504c = arrayList;
                                        i3 = i4;
                                    }
                                }
                                i4++;
                            } else {
                                i = i2;
                                i3 = i4;
                                i4++;
                            }
                            i2 = i;
                        }
                        Iterator<C3961e> it3 = next.f1517b.iterator();
                        while (it3.hasNext()) {
                            if (it3.next().f1505a.f1500d < 0) {
                                it3.remove();
                            }
                        }
                    }
                }
            }
            return m116a(m86g);
        } catch (Throwable th) {
            Log.e("Util", "trimOverlapEvent " + th.toString());
            return str;
        }
    }

    /* renamed from: i */
    public static int m84i(String str) {
        C3964a m86g = m86g(str);
        if (m86g != null) {
            return m86g.mo182b();
        }
        return 0;
    }

    /* renamed from: j */
    public static int m83j(String str) {
        if (str == null || str.length() == 0) {
            return 0;
        }
        InterfaceC3959c m114a = m114a(str);
        if (CurrentPlayingInfo.m80a(m114a)) {
            return m114a.mo182b();
        }
        return 0;
    }

    /* renamed from: b */
    public int[] m97b(byte[] bArr) {
        int i = 0;
        int length = bArr.length;
        int[] iArr = new int[length % 4 == 0 ? length / 4 : (length / 4) + 1];
        int i2 = 4;
        while (i2 <= length) {
            iArr[(i2 / 4) - 1] = (bArr[i2 - 4] << 24) | ((bArr[i2 - 3] & 255) << 16) | ((bArr[i2 - 2] & 255) << 8) | (bArr[i2 - 1] & 255);
            i2 += 4;
        }
        int i3 = 0;
        while (true) {
            if (i3 < (length + 4) - i2) {
                int i4 = (i2 / 4) - 1;
                if (i3 == 0) {
                    iArr[i4] = (bArr[(i2 - 4) + i3] << (((i - i3) - 1) * 8)) | iArr[i4];
                } else {
                    iArr[i4] = ((bArr[(i2 - 4) + i3] & 255) << (((i - i3) - 1) * 8)) | iArr[i4];
                }
                i3++;
            } else {
                return iArr;
            }
        }
    }
}
