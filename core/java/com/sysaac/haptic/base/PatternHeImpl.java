package com.sysaac.haptic.base;

import android.content.Context;
import android.os.Build;
import android.os.Process;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextUtils;
import android.util.Log;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONObject;

public class PatternHeImpl extends PatternHe {

    /* renamed from: a */
    public static AtomicInteger f1539a = new AtomicInteger();

    /* renamed from: b */
    private static final String f1540b = "PatternHeImpl";

    /* renamed from: c */
    private final Vibrator f1541c;

    /* renamed from: d */
    private final boolean f1542d = false;

    /* renamed from: e */
    private Class<?> f1543e;

    /* renamed from: f */
    private Context f1544f;

    public PatternHeImpl(Context context) {
        this.f1544f = context;
        this.f1541c = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        try {
            this.f1543e = Class.forName("android.os.RichTapVibrationEffect");
        } catch (ClassNotFoundException e) {
            Log.i("PatternHeImpl", "failed to reflect class: \"android.os.RichTapVibrationEffect\"!");
        }
        if (this.f1543e == null) {
            try {
                this.f1543e = Class.forName("android.os.VibrationEffect");
            } catch (ClassNotFoundException e2) {
                Log.i("PatternHeImpl", "failed to reflect class: \"android.os.VibrationEffect\"!");
            }
        }
    }

    /* renamed from: a */
    private int[] m154a(JSONArray jSONArray) {
        int[] iArr;
        String str;
        int i;
        if (jSONArray == null) {
            return null;
        }
        int[] iArr2 = new int[12];
        try {
            int length = jSONArray.length();
            int i2 = 5000;
            double d = 100.0d;
            String str2 = "Intensity";
            try {
                if (length == 4) {
                    int i3 = 0;
                    while (i3 < length) {
                        JSONObject jSONObject = jSONArray.getJSONObject(i3);
                        int i4 = jSONObject.getInt("Time");
                        int[] iArr3 = iArr2;
                        int i5 = (int) (jSONObject.getDouble(str2) * 100.0d);
                        int i6 = jSONObject.getInt("Frequency");
                        if (m145c(i4, 0, i2) && m145c(i5, 0, 100) && m145c(i6, -100, 100)) {
                            int i7 = i3 * 3;
                            iArr3[i7] = i4;
                            iArr3[i7 + 1] = i5;
                            iArr3[i7 + 2] = i6;
                            i3++;
                            iArr2 = iArr3;
                            i2 = 5000;
                        }
                        Log.e("PatternHeImpl", "point's time must be less than 5000, intensity must between 0~1, frequency must between -100 and 100");
                        return null;
                    }
                    return iArr2;
                } else if (length <= 4 || length > 16) {
                    return null;
                } else {
                    int i8 = 0;
                    while (i8 < length) {
                        JSONObject jSONObject2 = jSONArray.getJSONObject(i8);
                        int i9 = jSONObject2.getInt("Time");
                        int i10 = (int) (jSONObject2.getDouble(str2) * d);
                        int i11 = jSONObject2.getInt("Frequency");
                        if (m145c(i9, 0, 5000) && m145c(i10, 0, 100) && m145c(i11, -100, 100)) {
                            if (i8 == 0) {
                                int i12 = i8 * 3;
                                iArr2[i12] = i9;
                                iArr2[i12 + 1] = i10;
                                iArr2[i12 + 2] = i11;
                                str = str2;
                            } else {
                                int i13 = length - 1;
                                if (i8 < i13) {
                                    str = str2;
                                    double d2 = length / 2.0d;
                                    int i14 = 1;
                                    if (i8 < Math.ceil(d2)) {
                                        i = (int) (Math.ceil(d2) - 1.0d);
                                    } else {
                                        i = (length / 2) - 1;
                                        i14 = 2;
                                    }
                                    int i15 = i14 * 3;
                                    iArr2[i15] = iArr2[i15] + (i9 / i);
                                    int i16 = i15 + 1;
                                    iArr2[i16] = iArr2[i16] + (i10 / i);
                                    int i17 = i15 + 2;
                                    iArr2[i17] = iArr2[i17] + (i11 / i);
                                } else {
                                    str = str2;
                                    if (i8 == i13) {
                                        iArr2[9] = i9;
                                        iArr2[10] = i10;
                                        iArr2[11] = i11;
                                    }
                                }
                            }
                            i8++;
                            str2 = str;
                            d = 100.0d;
                        }
                        Log.e("PatternHeImpl", "point's time must be less than 5000, intensity must between 0~1, frequency must between -100 and 100");
                        return null;
                    }
                    return iArr2;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    /* renamed from: b */
    private boolean m151b(int i, int i2, int i3) {
        StringBuilder sb;
        String format;
        if (i >= 22) {
            if (i == 22) {
                if (i3 != 1) {
                    sb = new StringBuilder();
                    sb.append("RichTap version is ");
                    format = String.format("%x", Integer.valueOf(i));
                    sb.append(format);
                    sb.append(" can not support he version: ");
                    sb.append(i3);
                }
                return true;
            }
            if (i == 23) {
                if (i3 != 1) {
                    sb = new StringBuilder();
                    sb.append("RichTap version is ");
                    format = String.format("%x", Integer.valueOf(i));
                    sb.append(format);
                    sb.append(" can not support he version: ");
                    sb.append(i3);
                }
            } else if (i == 24 && i3 != 1 && i3 != 2) {
                return false;
            }
            return true;
        }
        sb = new StringBuilder();
        sb.append("can not support he in richtap version:");
        sb.append(String.format("%x02", Integer.valueOf(i)));
        Log.e("PatternHeImpl", sb.toString());
        return false;
    }

    private int[] m149b(String str) {
        int[] iArr = new int[0];
        boolean z;
        String str2;
        int i;
        int i2;
        String str3;
        String str4 = "RelativeTime";
        try {
            JSONArray jSONArray = new JSONObject(str).getJSONArray("Pattern");
            int min = Math.min(jSONArray.length(), 16);
            int[] iArr2 = new int[min * 17];
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            while (i3 < min) {
                try {
                    JSONObject jSONObject = jSONArray.getJSONObject(i3).getJSONObject("Event");
                    String string = jSONObject.getString("Type");
                    if (!TextUtils.equals("continuous", string)) {
                        if (!TextUtils.equals("transient", string)) {
                            Log.e("PatternHeImpl", "haven't get type value");
                            z = false;
                            break;
                        }
                        i = 4097;
                    } else {
                        i = 4096;
                    }
                    if (!jSONObject.has(str4)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("event:");
                        sb.append(i3);
                        sb.append(" don't have relativeTime parameters,set default:");
                        i2 = i3 * 400;
                        sb.append(i2);
                        Log.e("PatternHeImpl", sb.toString());
                    } else {
                        i2 = jSONObject.getInt(str4);
                    }
                    if (i2 >= 0) {
                        JSONObject jSONObject2 = jSONObject.getJSONObject("Parameters");
                        int i6 = jSONObject2.getInt("Intensity");
                        int i7 = jSONObject2.getInt("Frequency");
                        String str5 = str4;
                        JSONArray jSONArray2 = jSONArray;
                        if (i == 4096) {
                            if (!m145c(i6, 0, 100) || !m145c(i7, 0, 100)) {
                                str3 = "intensity or frequency is out of [0,100] for continuous event!";
                            }
                            int i8 = i3 * 17;
                            iArr2[i8 + 0] = i;
                            iArr2[i8 + 1] = i2;
                            iArr2[i8 + 2] = i6;
                            iArr2[i8 + 3] = i7;
                            if (4096 == i) {
                                if (!jSONObject.has("Duration")) {
                                    Log.e("PatternHeImpl", "event:" + i3 + " don't have duration parameters,set default:0");
                                    i5 = 0;
                                } else {
                                    i5 = jSONObject.getInt("Duration");
                                }
                                if (!m145c(i5, 0, 5000)) {
                                    str3 = "duration must be less than 5000";
                                } else {
                                    iArr2[i8 + 4] = i5;
                                    int[] m154a = m154a(jSONObject2.getJSONArray("Curve"));
                                    if (m154a != null) {
                                        System.arraycopy(m154a, 0, iArr2, i8 + 5, 12);
                                    }
                                }
                            }
                            i3++;
                            i4 = i2;
                            str4 = str5;
                            jSONArray = jSONArray2;
                        } else {
                            if (i == 4097 && (!m145c(i6, 0, 100) || !m145c(i7, -50, 150))) {
                                str3 = "intensity out of [0, 100] or frequency out of [-50, 150] for transient event!";
                            }
                            int i82 = i3 * 17;
                            iArr2[i82 + 0] = i;
                            iArr2[i82 + 1] = i2;
                            iArr2[i82 + 2] = i6;
                            iArr2[i82 + 3] = i7;
                            if (4096 == i) {
                            }
                            i3++;
                            i4 = i2;
                            str4 = str5;
                            jSONArray = jSONArray2;
                        }
                        i4 = i2;
                        z = false;
                        break;
                    }
                    str3 = "relativeTime:" + i2;
                    Log.e("PatternHeImpl", str3);
                    i4 = i2;
                    z = false;
                    break;
                } catch (Exception e) {
                    e = e;
                    iArr = iArr2;
                    e.printStackTrace();
                    return iArr;
                }
            }
            z = true;
            if (!z) {
                Log.e("PatternHeImpl", "current he file data, isn't compliance!!!!!!!");
                return null;
            }
            if (4096 == iArr2[((min - 1) * 17) + 0]) {
                str2 = "last event type is continuous, totalDuration:" + (i4 + i5);
            } else {
                str2 = "last event type is transient, totalDuration:" + (i4 + 48);
            }
            Log.d("PatternHeImpl", str2);
            return iArr2;
        } catch (Exception ignored) {
        }
        return iArr;
    }

    /* renamed from: b */
    private int[] m147b(JSONArray jSONArray) {
        if (jSONArray == null) {
            return null;
        }
        int[] iArr = new int[48];
        for (int i = 0; i < 48; i++) {
            iArr[i] = -1;
        }
        try {
            int length = jSONArray.length();
            if (length < 4) {
                return null;
            }
            int min = Math.min(length, 16);
            for (int i2 = 0; i2 < min; i2++) {
                JSONObject jSONObject = jSONArray.getJSONObject(i2);
                int i3 = jSONObject.getInt("Time");
                int i4 = (int) (jSONObject.getDouble("Intensity") * 100.0d);
                int i5 = jSONObject.getInt("Frequency");
                if (m145c(i3, 0, 5000) && m145c(i4, 0, 100) && m145c(i5, -100, 100)) {
                    int i6 = i2 * 3;
                    iArr[i6 + 0] = i3;
                    iArr[i6 + 1] = i4;
                    iArr[i6 + 2] = i5;
                }
                Log.e("PatternHeImpl", "point's time must be less than 5000, intensity must between 0~1, frequency must between -100 and 100");
                return null;
            }
            return iArr;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /* renamed from: c */
    private boolean m145c(int i, int i2, int i3) {
        return i >= i2 && i <= i3;
    }

    /* JADX WARN: Removed duplicated region for block: B:38:0x00ee A[Catch: Exception -> 0x0153, TryCatch #1 {Exception -> 0x0153, blocks: (B:7:0x0028, B:13:0x004d, B:16:0x0055, B:19:0x0078, B:20:0x0089, B:55:0x0159, B:57:0x0160, B:59:0x016c, B:60:0x017e, B:61:0x0182, B:22:0x008f, B:24:0x00ab, B:26:0x00b2, B:36:0x00d3, B:38:0x00ee, B:40:0x00f4, B:42:0x0116, B:45:0x0123, B:47:0x013b, B:41:0x0111, B:48:0x0143, B:31:0x00bf, B:33:0x00c6, B:17:0x0072, B:10:0x0043, B:49:0x014c), top: B:69:0x0028 }] */
    /* JADX WARN: Removed duplicated region for block: B:80:0x0143 A[SYNTHETIC] */
    /* renamed from: c */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private int[] m144c(String str) {
        int[] iArr = new int[0];
        boolean z;
        String str2;
        int i;
        int i2;
        String str3;
        String str4 = "RelativeTime";
        try {
            JSONArray jSONArray = new JSONObject(str).getJSONArray("Pattern");
            int min = Math.min(jSONArray.length(), 16);
            int[] iArr2 = new int[min * 55];
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            while (i3 < min) {
                try {
                    JSONObject jSONObject = jSONArray.getJSONObject(i3).getJSONObject("Event");
                    String string = jSONObject.getString("Type");
                    if (!TextUtils.equals("continuous", string)) {
                        if (!TextUtils.equals("transient", string)) {
                            Log.e("PatternHeImpl", "haven't get type value");
                            z = false;
                            break;
                        }
                        i = 4097;
                    } else {
                        i = 4096;
                    }
                    if (!jSONObject.has(str4)) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("event:");
                        sb.append(i3);
                        sb.append(" don't have relativeTime parameters,set default:");
                        i2 = i3 * 400;
                        sb.append(i2);
                        Log.e("PatternHeImpl", sb.toString());
                    } else {
                        i2 = jSONObject.getInt(str4);
                    }
                    if (i2 >= 0) {
                        JSONObject jSONObject2 = jSONObject.getJSONObject("Parameters");
                        int i6 = jSONObject2.getInt("Intensity");
                        int i7 = jSONObject2.getInt("Frequency");
                        String str5 = str4;
                        JSONArray jSONArray2 = jSONArray;
                        if (i == 4096) {
                            if (!m145c(i6, 0, 100) || !m145c(i7, 0, 100)) {
                                str3 = "intensity or frequency is out of [0,100] for continuous event!";
                            }
                            int i8 = i3 * 55;
                            iArr2[i8 + 0] = i;
                            iArr2[i8 + 1] = i2;
                            iArr2[i8 + 2] = i6;
                            iArr2[i8 + 3] = i7;
                            iArr2[i8 + 5] = 0;
                            if (4096 == i) {
                                if (!jSONObject.has("Duration")) {
                                    Log.e("PatternHeImpl", "event:" + i3 + " don't have duration parameters,set default:0");
                                    i5 = 0;
                                } else {
                                    i5 = jSONObject.getInt("Duration");
                                }
                                if (!m145c(i5, 0, 5000)) {
                                    str3 = "duration must be less than 5000";
                                } else {
                                    iArr2[i8 + 4] = i5;
                                    JSONArray jSONArray3 = jSONObject2.getJSONArray("Curve");
                                    iArr2[i8 + 6] = jSONArray3.length();
                                    int[] m147b = m147b(jSONArray3);
                                    if (m147b != null) {
                                        System.arraycopy(m147b, 0, iArr2, i8 + 7, 48);
                                    }
                                }
                            }
                            i3++;
                            i4 = i2;
                            str4 = str5;
                            jSONArray = jSONArray2;
                        } else {
                            if (i == 4097 && (!m145c(i6, 0, 100) || !m145c(i7, -50, 150))) {
                                str3 = "intensity out of [0, 100] or frequency out of [-50, 150] for transient event!";
                            }
                            int i82 = i3 * 55;
                            iArr2[i82 + 0] = i;
                            iArr2[i82 + 1] = i2;
                            iArr2[i82 + 2] = i6;
                            iArr2[i82 + 3] = i7;
                            iArr2[i82 + 5] = 0;
                            if (4096 == i) {
                            }
                            i3++;
                            i4 = i2;
                            str4 = str5;
                            jSONArray = jSONArray2;
                        }
                        i4 = i2;
                        z = false;
                        break;
                    }
                    str3 = "relativeTime :" + i2;
                    Log.e("PatternHeImpl", str3);
                    i4 = i2;
                    z = false;
                    break;
                } catch (Exception e) {
                    e = e;
                    iArr = iArr2;
                    e.printStackTrace();
                    return iArr;
                }
            }
            z = true;
            if (!z) {
                Log.e("PatternHeImpl", "current he file data, isn't compliance!!!!!!!");
                return null;
            }
            if (4096 == iArr2[((min - 1) * 55) + 0]) {
                str2 = "last event type is continuous, totalDuration:" + (i4 + i5);
            } else {
                str2 = "last event type is transient, totalDuration:" + (i4 + 80);
            }
            Log.d("PatternHeImpl", str2);
            return iArr2;
        } catch (Exception ignored) {
        }
        return iArr;
    }

    /* JADX WARN: Code restructure failed: missing block: B:59:0x019e, code lost:
        r2 = "intensity out of [0, 100] or frequency out of [-50, 150] for transient event!";
     */
    /* JADX WARN: Removed duplicated region for block: B:135:0x02b8 A[SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:62:0x01bd A[Catch: Exception -> 0x030e, TryCatch #1 {Exception -> 0x030e, blocks: (B:34:0x00e1, B:37:0x00f0, B:39:0x0109, B:42:0x0111, B:44:0x0134, B:102:0x02f1, B:104:0x02f8, B:45:0x0153, B:47:0x016d, B:49:0x0174, B:60:0x01a1, B:62:0x01bd, B:64:0x01c3, B:66:0x01dc, B:68:0x01e9, B:69:0x01f9, B:70:0x021d, B:72:0x0223, B:75:0x024b, B:79:0x0256, B:81:0x025d, B:84:0x0279, B:87:0x0294, B:91:0x02ae, B:97:0x02ba, B:88:0x029d, B:52:0x017c, B:55:0x018d, B:57:0x0194, B:98:0x02ce), top: B:118:0x00e1 }] */
    /* JADX WARN: Removed duplicated region for block: B:89:0x02a4  */
    /* JADX WARN: Removed duplicated region for block: B:93:0x02b3  */
    /* JADX WARN: Removed duplicated region for block: B:94:0x02b5  */
    /* JADX WARN: Removed duplicated region for block: B:97:0x02ba A[Catch: Exception -> 0x030e, LOOP:1: B:16:0x0085->B:97:0x02ba, LOOP_END, TryCatch #1 {Exception -> 0x030e, blocks: (B:34:0x00e1, B:37:0x00f0, B:39:0x0109, B:42:0x0111, B:44:0x0134, B:102:0x02f1, B:104:0x02f8, B:45:0x0153, B:47:0x016d, B:49:0x0174, B:60:0x01a1, B:62:0x01bd, B:64:0x01c3, B:66:0x01dc, B:68:0x01e9, B:69:0x01f9, B:70:0x021d, B:72:0x0223, B:75:0x024b, B:79:0x0256, B:81:0x025d, B:84:0x0279, B:87:0x0294, B:91:0x02ae, B:97:0x02ba, B:88:0x029d, B:52:0x017c, B:55:0x018d, B:57:0x0194, B:98:0x02ce), top: B:118:0x00e1 }] */
    /* renamed from: d */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    private C3975i[] m142d(String str) {
        C3975i[] c3975iArr = new C3975i[0];
        JSONArray jSONArray = null;
        int length = 0;
        C3975i[] c3975iArr2 = new C3975i[0];
        int i = 0;
        int i2 = 0;
        int i3 = 0;
        boolean z = false;
        C3975i[] c3975iArr3;
        String str2;
        String str3;
        String str4;
        int i4;
        String str5;
        int i5 = 0;
        int i6 = 0;
        String str6 = null;
        int i7 = 0;
        String str7 = "Duration";
        String str8 = "Frequency";
        String str9 = "Intensity";
        String str10 = "RelativeTime";
        try {
            jSONArray = new JSONObject(str).getJSONArray("PatternList");
            length = jSONArray.length();
            byte[] bArr = new byte[length * 64];
            c3975iArr2 = new C3975i[length];
            i = 0;
            i2 = 0;
            i3 = 0;
            z = true;
        } catch (Exception e) {
            e = e;
            c3975iArr = null;
        }
        while (i < length) {
            try {
                C3975i c3975i = new C3975i(this);
                JSONObject jSONObject = jSONArray.getJSONObject(i);
                int i8 = jSONObject.getInt("AbsoluteTime");
                c3975i.f1559a = i8;
                int i9 = i2 + i3;
                JSONArray jSONArray2 = jSONArray;
                if (i > 0 && i8 < i9) {
                    Log.e("PatternHeImpl", "Bad pattern relative time in int:" + i + ",last patternDuration:" + i2 + ", absTimeLast:" + i3);
                    return null;
                }
                JSONArray jSONArray3 = jSONObject.getJSONArray("Pattern");
                int min = Math.min(16, jSONArray3.length());
                c3975i.f1560b = new Event[min];
                int i10 = length;
                int i11 = 0;
                int i12 = -1;
                int i13 = 0;
                while (true) {
                    if (i13 >= min) {
                        str2 = str7;
                        str3 = str9;
                        str4 = str10;
                        c3975iArr3 = c3975iArr2;
                        i4 = i8;
                        str5 = str8;
                        i2 = i11;
                        break;
                    }
                    i4 = i8;
                    JSONArray jSONArray4 = jSONArray3;
                    JSONObject jSONObject2 = jSONArray3.getJSONObject(i13).getJSONObject("Event");
                    String string = jSONObject2.getString("Type");
                    int i14 = min;
                    boolean z2 = z;
                    if (!TextUtils.equals("continuous", string)) {
                        if (!TextUtils.equals("transient", string)) {
                            str2 = str7;
                            str3 = str9;
                            str4 = str10;
                            c3975iArr3 = c3975iArr2;
                            str5 = str8;
                            Log.e("PatternHeImpl", "haven't get type value");
                            i2 = i11;
                            break;
                        }
                        c3975i.f1560b[i13] = new C3979m(this);
                        i5 = 4097;
                    } else {
                        try {
                            c3975i.f1560b[i13] = new Continuous(this);
                            i5 = 4096;
                        } catch (Exception e2) {
                            c3975iArr = c3975iArr2;
                        }
                    }
                    int i15 = jSONObject2.getInt("Index");
                    c3975iArr3 = c3975iArr2;
                    if (Util.m118a(this.f1544f)) {
                        if (1 == i15) {
                            i15 = 2;
                        } else if (2 == i15) {
                            i15 = 1;
                        }
                    }
                    try {
                        c3975i.f1560b[i13].f1550f = (byte) i15;
                        if (!jSONObject2.has(str10)) {
                            Log.e("PatternHeImpl", "event:" + i + " don't have relativeTime parameters,BAD he!");
                            return null;
                        }
                        int i16 = jSONObject2.getInt(str10);
                        if (i13 > 0 && i16 < i12) {
                            Log.w("PatternHeImpl", "pattern ind:" + i + " event:" + i13 + " relative time seems not right!");
                        }
                        if (i16 < 0) {
                            Log.e("PatternHeImpl", "relativeTime:" + i16);
                            str2 = str7;
                            str3 = str9;
                            str4 = str10;
                            i2 = i11;
                            z = false;
                            str5 = str8;
                            break;
                        }
                        JSONObject jSONObject3 = jSONObject2.getJSONObject("Parameters");
                        int i17 = jSONObject3.getInt(str9);
                        str4 = str10;
                        int i18 = jSONObject3.getInt(str8);
                        i6 = i11;
                        str6 = str8;
                        if (i5 == 4096) {
                            if (!m145c(i17, 0, 100) || !m145c(i18, 0, 100)) {
                                break;
                            }
                            c3975i.f1560b[i13].f1548d = i5;
                            c3975i.f1560b[i13].f1551g = i16;
                            c3975i.f1560b[i13].f1552h = i17;
                            c3975i.f1560b[i13].f1553i = i18;
                            if (4096 == i5) {
                                str2 = str7;
                                str3 = str9;
                                str5 = str6;
                                i7 = 48;
                            } else if (!jSONObject2.has(str7)) {
                                Log.e("PatternHeImpl", "event:" + i + " don't have duration parameters");
                                return null;
                            } else {
                                i7 = jSONObject2.getInt(str7);
                                if (!m145c(i7, 0, 5000)) {
                                    Log.e("PatternHeImpl", "duration must be less than 5000");
                                    str2 = str7;
                                    str3 = str9;
                                    z = false;
                                    i2 = i6;
                                    str5 = str6;
                                    break;
                                }
                                c3975i.f1560b[i13].f1554j = i7;
                                JSONArray jSONArray5 = jSONObject3.getJSONArray("Curve");
                                ((Continuous) c3975i.f1560b[i13]).f1545a = (byte) jSONArray5.length();
                                int length2 = jSONArray5.length();
                                C3978l[] c3978lArr = new C3978l[length2];
                                str2 = str7;
                                int i19 = 0;
                                int i20 = 0;
                                int i21 = -1;
                                while (i19 < jSONArray5.length()) {
                                    JSONObject jSONObject4 = jSONArray5.getJSONObject(i19);
                                    c3978lArr[i19] = new C3978l(this);
                                    i20 = jSONObject4.getInt("Time");
                                    String str11 = str9;
                                    JSONArray jSONArray6 = jSONArray5;
                                    int i22 = (int) (jSONObject4.getDouble(str9) * 100.0d);
                                    String str12 = str6;
                                    int i23 = jSONObject4.getInt(str12);
                                    if (i19 == 0 && i20 != 0) {
                                        Log.d("PatternHeImpl", "time of first point is not 0,bad he!");
                                        return null;
                                    } else if (i19 > 0 && i20 < i21) {
                                        Log.d("PatternHeImpl", "point times did not arrange in order,bad he!");
                                        return null;
                                    } else {
                                        c3978lArr[i19].f1567a = i20;
                                        c3978lArr[i19].f1568b = i22;
                                        c3978lArr[i19].f1569c = i23;
                                        i19++;
                                        str6 = str12;
                                        i21 = i20;
                                        str9 = str11;
                                        jSONArray5 = jSONArray6;
                                    }
                                }
                                str3 = str9;
                                str5 = str6;
                                if (i20 != i7) {
                                    Log.e("PatternHeImpl", "event:" + i + " point last time do not match duration parameter");
                                    return null;
                                } else if (length2 > 0) {
                                    ((Continuous) c3975i.f1560b[i13]).f1546b = c3978lArr;
                                } else {
                                    Log.d("PatternHeImpl", "continuous event has nothing in point");
                                    z = false;
                                    int i24 = i7 + i16;
                                    i11 = i6 < i24 ? i24 : i6;
                                    if (!z) {
                                        i2 = i11;
                                        break;
                                    }
                                    i13++;
                                    str8 = str5;
                                    i12 = i16;
                                    i8 = i4;
                                    jSONArray3 = jSONArray4;
                                    min = i14;
                                    str7 = str2;
                                    str9 = str3;
                                    c3975iArr2 = c3975iArr3;
                                    str10 = str4;
                                }
                            }
                            z = z2;
                            int i242 = i7 + i16;
                        } else {
                            if (i5 == 4097 && (!m145c(i17, 0, 100) || !m145c(i18, -50, 150))) {
                                break;
                            }
                            c3975i.f1560b[i13].f1548d = i5;
                            c3975i.f1560b[i13].f1551g = i16;
                            c3975i.f1560b[i13].f1552h = i17;
                            c3975i.f1560b[i13].f1553i = i18;
                            if (4096 == i5) {
                            }
                            z = z2;
                            int i2422 = i7 + i16;
                            if (i6 < i2422) {
                            }
                            if (!z) {
                            }
                        }
                    } catch (Exception e) {
                        c3975iArr = c3975iArr3;
                        e.printStackTrace();
                        return c3975iArr;
                    }
                }
                String str13 = "intensity or frequency is out of [0,100] for continuous event!";
                Log.e("PatternHeImpl", str13);
                str2 = str7;
                str3 = str9;
                i2 = i6;
                str5 = str6;
                z = false;
                if (!z) {
                    Log.e("PatternHeImpl", "current he file data, isn't compliance!!!!!!!");
                    return null;
                }
                c3975iArr3[i] = c3975i;
                i++;
                str8 = str5;
                jSONArray = jSONArray2;
                length = i10;
                i3 = i4;
                str7 = str2;
                str9 = str3;
                c3975iArr2 = c3975iArr3;
                str10 = str4;
                c3975iArr = c3975iArr2;
            } catch (Exception e4) {
                c3975iArr3 = c3975iArr2;
            }
            return c3975iArr;
        }
        return c3975iArr2;
    }

    /* JADX WARN: Code restructure failed: missing block: B:27:0x00b3, code lost:
        android.util.Log.m331e("PatternHeImpl", "haven't get type value");
     */
    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: a */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
    */
    public int mo156a(String str) {
        int i;
        int a;
        Log.d("PatternHeImpl", "getNonRichTapPatternDuration");
        try {
            JSONArray jSONArray = new JSONObject(str).getJSONArray("Pattern");
            int min = Math.min(jSONArray.length(), 16);
            int i2 = min * 2;
            long[] jArr = new long[i2];
            int i3 = 0;
            int i4 = 0;
            int i5 = 0;
            while (true) {
                if (i3 >= min) {
                    break;
                }
                JSONObject jSONObject = jSONArray.getJSONObject(i3).getJSONObject("Event");
                String string = jSONObject.getString("Type");
                if (!TextUtils.equals("continuous", string)) {
                    if (!TextUtils.equals("transient", string)) {
                        break;
                    }
                    int i6 = i3 * 2;
                    jArr[i6] = (jSONObject.getInt("RelativeTime") - i4) - i5;
                    if (jArr[i6] < 0) {
                        jArr[i6] = 50;
                    }
                    JSONObject jSONObject2 = jSONObject.getJSONObject("Parameters");
                    a = m163a(jSONObject2.getInt("Intensity"), jSONObject2.getInt("Frequency"));
                    jArr[i6 + 1] = a;
                } else {
                    int i7 = i3 * 2;
                    jArr[i7] = (jSONObject.getInt("RelativeTime") - i4) - i5;
                    if (jArr[i7] < 0) {
                        jArr[i7] = 50;
                    }
                    a = jSONObject.getInt("Duration");
                    if (a > 50 && a < 100) {
                        a = 50;
                    } else if (a > 100) {
                        a -= 50;
                    }
                    jArr[i7 + 1] = a;
                }
                i5 = a;
                i4 = jSONObject.getInt("RelativeTime");
                i3++;
            }
            int i8 = 0;
            for (int i9 = 0; i9 < i2; i9++) {
                try {
                    i8 = (int) (i8 + jArr[i9]);
                } catch (Exception e) {
                    e = e;
                    i = i8;
                    e.printStackTrace();
                    return i;
                }
            }
            return i8;
        } catch (Exception e2) {
            i = 0;
        }
        return i;
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: a */
    public void mo161a() {
        mo159a(0, 0, 0);
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: a */
    public void mo160a(int i) {
        mo159a(-1, -1, i);
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: a */
    public void mo159a(int i, int i2, int i3) {
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                this.f1541c.vibrate((VibrationEffect) this.f1543e.getMethod("createPatternHeParameter", Integer.TYPE, Integer.TYPE, Integer.TYPE).invoke(null, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3)));
            } else {
                Log.e("PatternHeImpl", "The system apk is low than 26,does not support richTap!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("PatternHeImpl", "The system doesn't integrate richTap software");
        }
    }

    @Override
    public void mo157a(File file, int i, int i2, int i3, int i4) {
        if (!Util.m112a(file.getPath(), ".he")) {
            return;
        }
        Log.d("PatternHeImpl", "looper:" + i + " interval:" + i2 + " amplitude:" + i3 + " freq:" + i4);
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = null;
        BufferedReader bufferedReader2 = null;
        try {
            try {
                try {
                    BufferedReader bufferedReader3 = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    while (true) {
                        try {
                            String readLine = bufferedReader3.readLine();
                            if (readLine == null) {
                                break;
                            }
                            sb.append(readLine);
                        } catch (Exception e) {
                            e = e;
                            bufferedReader2 = bufferedReader3;
                            e.printStackTrace();
                            bufferedReader2.close();
                            mo155a(sb.toString(), i, i2, i3, i4);
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader = bufferedReader3;
                            try {
                                bufferedReader.close();
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                            throw th;
                        }
                    }
                    bufferedReader3.close();
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            } catch (Exception ignored) {
            }
            mo155a(sb.toString(), i, i2, i3, i4);
        } catch (Throwable ignored) {
        }
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: a */
    public void mo155a(String str, int i, int i2, int i3, int i4) {
        int i5;
        int m122a = 0;
        String str2;
        if (this.f1541c == null) {
            str2 = "Please call the init method";
        } else if (i >= 1) {
            try {
                JSONObject jSONObject = new JSONObject(str);
                boolean m96c = Util.m96c();
                if (!m96c) {
                    i5 = jSONObject.getJSONObject("Metadata").getInt("Version");
                    if (!m151b(Util.m122a(), Util.m105b(), i5)) {
                        Log.e("PatternHeImpl", "richtap version check failed, richTapMajorVersion:" + String.format("%x02", Integer.valueOf(m122a)) + " heVersion:" + i5);
                        return;
                    }
                } else {
                    i5 = 0;
                }
                int[] m149b = Util.m122a() < 24 ? m149b(str) : m144c(str);
                if (m149b == null) {
                    Log.e("PatternHeImpl", "serialize he failed!! ,heVersion:" + i5);
                    return;
                }
                int length = m149b.length;
                try {
                    if (Build.VERSION.SDK_INT < 26) {
                        Log.e("PatternHeImpl", "The system is low than 26,does not support richTap!!");
                        return;
                    }
                    Method method = this.f1543e.getMethod("createPatternHeWithParam", int[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE);
                    if (!m96c) {
                        int[] iArr = new int[length + 1];
                        iArr[0] = Util.m122a() < 24 ? 1 : 3;
                        System.arraycopy(m149b, 0, iArr, 1, m149b.length);
                        m149b = iArr;
                    }
                    this.f1541c.vibrate((VibrationEffect) method.invoke(null, m149b, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)));
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w("PatternHeImpl", "for createPatternHe, The system doesn't integrate richTap software");
                    return;
                }
            } catch (Exception e2) {
                e2.printStackTrace();
                return;
            }
        } else {
            str2 = "The minimum count of loop pattern is 1";
        }
        Log.e("PatternHeImpl", str2);
    }

    /* renamed from: a */
    int[] m158a(int i, int i2, int i3, int i4, int i5, int i6, C3975i[] c3975iArr) {
        int i7 = 0;
        for (int i8 = 0; i8 < i3; i8++) {
            i7 += c3975iArr[i8].m138b();
        }
        int i9 = 5;
        int[] iArr = new int[i7 + 5];
        Arrays.fill(iArr, 0);
        iArr[0] = i;
        iArr[1] = i2;
        iArr[2] = i4;
        iArr[3] = i5;
        iArr[4] = iArr[4] | (65535 & i3);
        iArr[4] = ((c3975iArr.length << 16) & (-65536)) | iArr[4];
        for (int i10 = 0; i10 < i3; i10++) {
            int[] m139a = c3975iArr[i10].m139a(i6);
            System.arraycopy(m139a, 0, iArr, i9, m139a.length);
            i9 += m139a.length;
            i6++;
        }
        return iArr;
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: b */
    public void mo153b(int i) {
        mo159a(i, -1, -1);
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: b */
    public void mo152b(int i, int i2) {
        int[] mo137a = new PatternHeEventPreBaked(4097, 0, 0, i, i2).mo137a();
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                this.f1541c.vibrate((VibrationEffect) this.f1543e.getMethod("createPatternHeWithParam", int[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE).invoke(null, mo137a, 1, 0, 255, 0));
            } else {
                Log.e("PatternHeImpl", "The system is low than 26,does not support richTap!!");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w("PatternHeImpl", "The system doesn't integrate richTap software");
        }
    }

    @Override
    public void mo150b(File file, int i, int i2, int i3, int i4) {
        if (!Util.m112a(file.getPath(), ".he")) {
            return;
        }
        Log.d("PatternHeImpl", "looper:" + i + " interval:" + i2 + " amplitude:" + i3 + " freq:" + i4);
        StringBuilder sb = new StringBuilder();
        BufferedReader bufferedReader = null;
        BufferedReader bufferedReader2 = null;
        try {
            try {
                try {
                    BufferedReader bufferedReader3 = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
                    while (true) {
                        try {
                            String readLine = bufferedReader3.readLine();
                            if (readLine == null) {
                                break;
                            }
                            sb.append(readLine);
                        } catch (Exception e) {
                            e = e;
                            bufferedReader2 = bufferedReader3;
                            e.printStackTrace();
                            bufferedReader2.close();
                            mo143c(sb.toString(), i, i2, i3, i4);
                        } catch (Throwable th) {
                            th = th;
                            bufferedReader = bufferedReader3;
                            try {
                                bufferedReader.close();
                            } catch (Exception e2) {
                                e2.printStackTrace();
                            }
                            throw th;
                        }
                    }
                    bufferedReader3.close();
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            } catch (Exception ignored) {
            }
            mo143c(sb.toString(), i, i2, i3, i4);
        } catch (Throwable ignored) {
        }
    }

    @Override
    public void mo148b(String str, int i, int i2, int i3, int i4) {
        int i5;
        int m122a = 0;
        String str2;
        if (this.f1541c == null) {
            str2 = "Please call the init method";
        } else {
            Log.d("PatternHeImpl", "play new he api applyPatternHeWithString(String patternString, int loop,int interval,int amplitude,int freq)");
            if (i >= 1) {
                try {
                    JSONObject jSONObject = new JSONObject(str);
                    if (!Util.m96c()) {
                        int i6 = jSONObject.getJSONObject("Metadata").getInt("Version");
                        if (!m151b(Util.m122a(), Util.m105b(), i6)) {
                            Log.e("PatternHeImpl", "richtap version check failed, richTapMajorVersion:" + String.format("%x02", Integer.valueOf(m122a)) + " heVersion:" + i6);
                            return;
                        }
                        i5 = i6;
                    } else {
                        i5 = 0;
                    }
                    C3975i[] m142d = m142d(str);
                    if (m142d != null && m142d.length != 0) {
                        int andIncrement = f1539a.getAndIncrement();
                        int myPid = Process.myPid();
                        int i7 = 0;
                        int i8 = 0;
                        while (i7 < ((int) Math.ceil(m142d.length / 4.0d))) {
                            int i9 = i7 + 1;
                            int length = m142d.length < i9 * 4 ? m142d.length - (i7 * 4) : 4;
                            C3975i[] c3975iArr = new C3975i[length];
                            System.arraycopy(m142d, i7 * 4, c3975iArr, 0, length);
                            int i10 = length;
                            int[] m158a = m158a(2, i5, length, myPid, andIncrement, i8, c3975iArr);
                            try {
                                if (Build.VERSION.SDK_INT >= 26) {
                                    this.f1541c.vibrate((VibrationEffect) this.f1543e.getMethod("createPatternHeWithParam", int[].class, Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE).invoke(null, m158a, Integer.valueOf(i), Integer.valueOf(i2), Integer.valueOf(i3), Integer.valueOf(i4)));
                                } else {
                                    Log.e("PatternHeImpl", "The system is low than 26,does not support richTap!!");
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                                Log.w("PatternHeImpl", "for createPatternHe, The system doesn't integrate richTap software");
                            }
                            i7 = i9;
                            i8 = i10;
                        }
                        return;
                    }
                    Log.e("PatternHeImpl", "serialize he failed!!, heVersion:" + i5);
                    return;
                } catch (Exception e2) {
                    e2.printStackTrace();
                    return;
                }
            }
            str2 = "The minimum count of loop pattern is 1";
        }
        Log.e("PatternHeImpl", str2);
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: c */
    public void mo146c(int i) {
        mo159a(-1, i, -1);
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: c */
    public void mo143c(String str, int i, int i2, int i3, int i4) {
        String str2;
        int i5;
        int i6;
        String str3;
        int i7;
        boolean z;
        int i8;
        String str4;
        String str5 = "PatternHeImpl";
        if (this.f1541c == null) {
            str4 = "Please call the init method";
        } else {
            Log.d(str5, "play new he api, applyPatternHeWithStringOnNoRichTap");
            if (i >= 1) {
                try {
                    JSONArray jSONArray = new JSONObject(str).getJSONArray("Pattern");
                    int min = Math.min(jSONArray.length(), 16);
                    int i9 = min * 2;
                    long[] jArr = new long[i9];
                    int[] iArr = new int[i9];
                    int i10 = 0;
                    Arrays.fill(iArr, 0);
                    int i11 = 0;
                    int i12 = 0;
                    int i13 = 0;
                    while (true) {
                        if (i11 >= min) {
                            str2 = str5;
                            i5 = i9;
                            i6 = i10;
                            break;
                        }
                        JSONObject jSONObject = jSONArray.getJSONObject(i11).getJSONObject("Event");
                        String string = jSONObject.getString("Type");
                        JSONArray jSONArray2 = jSONArray;
                        int i14 = min;
                        i5 = i9;
                        if (!TextUtils.equals("continuous", string)) {
                            str3 = str5;
                            if (!TextUtils.equals("transient", string)) {
                                i6 = 0;
                                str2 = str3;
                                Log.e(str2, "haven't get type value");
                                break;
                            }
                            int i15 = i11 * 2;
                            jArr[i15] = (jSONObject.getInt("RelativeTime") - i12) - i13;
                            if (jArr[i15] < 0) {
                                jArr[i15] = 50;
                            }
                            i7 = 0;
                            iArr[i15] = 0;
                            JSONObject jSONObject2 = jSONObject.getJSONObject("Parameters");
                            int i16 = jSONObject2.getInt("Intensity");
                            int a = m163a(i16, jSONObject2.getInt("Frequency"));
                            int i17 = i15 + 1;
                            jArr[i17] = a;
                            z = true;
                            iArr[i17] = Math.max(1, Math.min((int) (((i3 * 1.0d) * i16) / 100.0d), 255));
                            i13 = a;
                            i8 = jSONObject.getInt("RelativeTime");
                        } else {
                            JSONObject jSONObject3 = jSONObject.getJSONObject("Parameters");
                            int[] m154a = m154a(jSONObject3.getJSONArray("Curve"));
                            int i18 = i11 * 2;
                            jArr[i18] = (jSONObject.getInt("RelativeTime") - i12) - i13;
                            if (jArr[i18] < 0) {
                                jArr[i18] = 50;
                            }
                            iArr[i18] = 0;
                            int i19 = jSONObject.getInt("Duration");
                            if (i19 > 50 && i19 < 100) {
                                i19 = 50;
                            } else if (i19 > 100) {
                                i19 -= 50;
                            }
                            int i20 = i18 + 1;
                            jArr[i20] = i19;
                            int max = Math.max(Math.min((m154a[4] * 255) / 100, 255), Math.min((m154a[7] * 255) / 100, 255));
                            int i21 = jSONObject3.getInt("Intensity");
                            int i22 = jSONObject3.getInt("Frequency");
                            str3 = str5;
                            int max2 = Math.max(1, (int) (((max * (i21 / 100.0d)) * i3) / 255.0d));
                            if (i22 < 30) {
                                max2 = 0;
                            }
                            iArr[i20] = max2;
                            i8 = jSONObject.getInt("RelativeTime");
                            i13 = i19;
                            z = true;
                            i7 = 0;
                        }
                        i11++;
                        jSONArray = jSONArray2;
                        min = i14;
                        i9 = i5;
                        str5 = str3;
                        int i23 = i7;
                        i12 = i8;
                        i10 = i23;
                    }
                    Log.d(str2, "times:" + Arrays.toString(jArr));
                    int i24 = i5;
                    for (int i25 = i6; i25 < i24; i25++) {
                        long j = jArr[i25];
                    }
                    if (Build.VERSION.SDK_INT >= 26) {
                        this.f1541c.vibrate(VibrationEffect.createWaveform(jArr, iArr, -1));
                        return;
                    } else {
                        this.f1541c.vibrate(jArr, -1);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }
            }
            str4 = "The minimum count of loop pattern is 1";
        }
        Log.e(str5, str4);
    }

    @Override // com.sysaac.haptic.base.PatternHe
    /* renamed from: d */
    public void mo141d(String str, int i, int i2, int i3, int i4) {
    }
}
