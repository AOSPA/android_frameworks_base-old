package com.sysaac.haptic.p031a.p032a;

import android.util.Log;
import com.sysaac.haptic.base.Util;
import com.sysaac.haptic.p033b.p034a.C3961e;
import com.sysaac.haptic.p033b.p035b.C3962a;
import java.io.File;
import java.lang.reflect.Array;
import java.util.Iterator;

/* JADX WARN: Classes with same name are omitted:
  classes4.dex
 */
/* renamed from: com.sysaac.haptic.a.a.a */
/* loaded from: C:\Users\chris\AppData\Local\Temp\jadx-17855786731205153992\classes4.dex */
public class C3955a {

    /* renamed from: a */
    public static final String f1482a = "NordicUtils";

    /* renamed from: b */
    public static final byte f1483b = 16;

    /* renamed from: c */
    public static final byte f1484c = 17;

    /* renamed from: d */
    public static final int f1485d = 1;

    /* renamed from: e */
    public static final int f1486e = 5;

    /* renamed from: f */
    public static final int f1487f = 15;

    /* renamed from: g */
    public static final int f1488g = 241;

    /* renamed from: h */
    public static final int f1489h = 4;

    /* renamed from: i */
    public static final int f1490i = 16;

    /* renamed from: a */
    private static byte m190a(int i) {
        if (i > 127) {
            i = 127;
        } else if (i < -128) {
            i = -128;
        }
        return (byte) i;
    }

    /* renamed from: a */
    private static int m184a(byte[] bArr, int i, int i2) {
        int i3 = i + i2;
        int i4 = 0;
        while (i < i3) {
            i2--;
            i4 += (bArr[i] & 255) << (i2 * 8);
            i++;
        }
        return i4;
    }

    /* renamed from: a */
    public static String m185a(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int i = 0;
        byte b = bArr[0];
        sb.append("\n He10Bytes length:" + bArr.length + ",event count:" + ((int) b));
        int i2 = 1;
        while (i < b) {
            byte b2 = bArr[i2];
            StringBuilder sb2 = new StringBuilder();
            sb2.append("\n ");
            i++;
            sb2.append(i);
            sb2.append(":\t Type:");
            sb2.append((int) b2);
            sb.append(sb2.toString());
            if (16 == b2) {
                sb.append("\t RelativeTime:" + m184a(bArr, i2 + 1, 2) + "\t Frequency:" + ((int) bArr[i2 + 3]) + "\t Intensity:" + ((int) bArr[i2 + 4]));
                i2 += 5;
            } else if (17 == b2) {
                sb.append("\t RelativeTime:" + m184a(bArr, i2 + 1, 2) + "\t Duration:" + m184a(bArr, i2 + 3, 2) + "\t Frequency0:" + ((int) bArr[i2 + 5]) + "\t Time1:" + m184a(bArr, i2 + 6, 2) + "\t Frequency1:" + ((int) bArr[i2 + 8]) + "\t Intensity1:" + ((int) bArr[i2 + 9]) + "\t Time2:" + m184a(bArr, i2 + 10, 2) + "\t Frequency2:" + ((int) bArr[i2 + 12]) + "\t Intensity2:" + ((int) bArr[i2 + 13]) + "\t Frequency3:" + ((int) bArr[i2 + 14]));
                i2 += 15;
            }
        }
        return sb.toString();
    }

    /* renamed from: a */
    private static byte[] m189a(int i, int i2) {
        byte[] bArr = new byte[i2];
        for (int i3 = 0; i3 < i2; i3++) {
            bArr[(i2 - i3) - 1] = (byte) ((i >> (i3 * 8)) & 255);
        }
        return bArr;
    }

    /* renamed from: a */
    private static byte[] m188a(C3962a c3962a) {
        String str = null;
        if (c3962a == null || c3962a.f1507b == null || c3962a.f1507b.size() < 1 || c3962a.f1507b.size() > 16) {
            return null;
        }
        byte[] bArr = new byte[241];
        bArr[0] = (byte) c3962a.f1507b.size();
        Iterator<C3961e> it = c3962a.f1507b.iterator();
        int i = 1;
        while (it.hasNext()) {
            C3961e next = it.next();
            if (next.f1505a == null || next.f1505a.f1501e == null) {
                str = "null == patternItem.Event or null == patternItem.Event.Parameters";
            } else if ("transient".equals(next.f1505a.f1497a)) {
                bArr[i] = 16;
                System.arraycopy(m189a(next.f1505a.f1498b, 2), 0, bArr, i + 1, 2);
                bArr[i + 3] = (byte) next.f1505a.f1501e.f1503b;
                bArr[i + 4] = (byte) next.f1505a.f1501e.f1502a;
                i += 5;
            } else if ("continuous".equals(next.f1505a.f1497a)) {
                bArr[i] = 17;
                System.arraycopy(m189a(next.f1505a.f1498b, 2), 0, bArr, i + 1, 2);
                System.arraycopy(m189a(next.f1505a.f1499c, 2), 0, bArr, i + 3, 2);
                if (next.f1505a.f1501e.f1504c == null || 4 != next.f1505a.f1501e.f1504c.size()) {
                    str = "null == patternItem.Event.Parameters.Curve or POINT_COUNT != patternItem.Event.Parameters.Curve.size()";
                } else {
                    bArr[i + 5] = m190a(next.f1505a.f1501e.f1504c.get(0).f1496c + next.f1505a.f1501e.f1503b);
                    System.arraycopy(m189a(next.f1505a.f1501e.f1504c.get(1).f1494a, 2), 0, bArr, i + 6, 2);
                    bArr[i + 8] = m190a(next.f1505a.f1501e.f1504c.get(1).f1496c + next.f1505a.f1501e.f1503b);
                    bArr[i + 9] = (byte) (next.f1505a.f1501e.f1504c.get(1).f1495b * next.f1505a.f1501e.f1502a);
                    System.arraycopy(m189a(next.f1505a.f1501e.f1504c.get(2).f1494a, 2), 0, bArr, i + 10, 2);
                    bArr[i + 12] = m190a(next.f1505a.f1501e.f1504c.get(2).f1496c + next.f1505a.f1501e.f1503b);
                    bArr[i + 13] = (byte) (next.f1505a.f1501e.f1504c.get(2).f1495b * next.f1505a.f1501e.f1502a);
                    bArr[i + 14] = m190a(next.f1505a.f1501e.f1504c.get(3).f1496c + next.f1505a.f1501e.f1503b);
                    i += 15;
                }
            } else {
                str = "unknown event type.";
            }
            Log.w("NordicUtils", str);
        }
        byte[] bArr2 = (byte[]) Array.newInstance(Byte.TYPE, i);
        System.arraycopy(bArr, 0, bArr2, 0, i);
        return bArr2;
    }

    /* renamed from: a */
    public static byte[] m187a(File file) {
        return m186a(Util.m102b(file));
    }

    /* renamed from: a */
    public static byte[] m186a(String str) {
        if (1 != Util.m88e(str)) {
            return null;
        }
        return m188a(Util.m87f(str));
    }
}
