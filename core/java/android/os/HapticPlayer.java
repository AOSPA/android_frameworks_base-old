package android.os;

import android.annotation.NonNull;
import android.annotation.SuppressLint;
import android.app.ActivityThread;
import android.content.Context;
import android.os.Build;
import android.os.CombinedVibration;
import android.os.VibrationAttributes;
import android.os.VibrationEffect;
import android.text.TextUtils;
import android.util.Log;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/** @hide */
@SuppressLint("NotCloseable")
public class HapticPlayer {

    /** @hide */
    public static final int ANDROID_VERSIONCODE_O = 26;
    /** @hide */
    public static final int CONTINUOUS_EVENT = 4096;
    /** @hide */
    public static final String EVENT_KEY_DURATION = "Duration";
    /** @hide */
    public static final String EVENT_KEY_EVENT = "Event";
    /** @hide */
    public static final String EVENT_KEY_HE_CURVE = "Curve";
    /** @hide */
    public static final String EVENT_KEY_HE_CURVE_POINT_TIME = "Time";
    /** @hide */
    public static final String EVENT_KEY_HE_FREQUENCY = "Frequency";
    /** @hide */
    public static final String EVENT_KEY_HE_INTENSITY = "Intensity";
    /** @hide */
    public static final String EVENT_KEY_HE_PARAMETERS = "Parameters";
    /** @hide */
    public static final String EVENT_KEY_HE_TYPE = "Type";
    /** @hide */
    public static final String EVENT_KEY_RELATIVE_TIME = "RelativeTime";
    /** @hide */
    public static final String EVENT_TYPE_HE_CONTINUOUS_NAME = "continuous";
    /** @hide */
    public static final String EVENT_TYPE_HE_TRANSIENT_NAME = "transient";
    /** @hide */
    public static final int FORMAT_VERSION = 2;
    /** @hide */
    public static final int HE2_0_PATTERN_WRAP_NUM = 10;
    /** @hide */
    public static final int HE_CURVE_POINT_0_FREQUENCY = 9;
    /** @hide */
    public static final int HE_CURVE_POINT_0_INTENSITY = 8;
    /** @hide */
    public static final int HE_CURVE_POINT_0_TIME = 7;
    /** @hide */
    public static final int HE_DEFAULT_DURATION = 0;
    /** @hide */
    public static final int HE_DEFAULT_RELATIVE_TIME = 400;
    /** @hide */
    public static final int HE_DURATION = 4;
    /** @hide */
    public static final int HE_FREQUENCY = 3;
    /** @hide */
    public static final int HE_INTENSITY = 2;
    /** @hide */
    private static final String HE_META_DATA_KEY = "Metadata";
    /** @hide */
    public static final int HE_POINT_COUNT = 6;
    /** @hide */
    public static final int HE_RELATIVE_TIME = 1;
    /** @hide */
    public static final int HE_TYPE = 0;
    /** @hide */
    public static final int HE_VALUE_LENGTH = 55;
    /** @hide */
    private static final String HE_VERSION_KEY = "Version";
    /** @hide */
    public static final int HE_VIB_INDEX = 5;
    private static final int MAX_EVENT_COUNT = 16;
    private static final int MAX_FREQ = 100;
    private static final int MAX_INTENSITY = 100;
    private static final int MAX_PATERN_EVENT_LAST_TIME = 5000;
    private static final int MAX_PATERN_LAST_TIME = 50000;
    private static final int MAX_POINT_COUNT = 16;
    /** @hide */
    public static final String PATTERN_KEY_EVENT_VIB_ID = "Index";
    /** @hide */
    public static final String PATTERN_KEY_PATTERN = "Pattern";
    /** @hide */
    public static final String PATTERN_KEY_PATTERN_ABS_TIME = "AbsoluteTime";
    private static final String PATTERN_KEY_PATTERN_LIST = "PatternList";
    private static final String TAG = "HapticPlayer";
    /** @hide */
    public static final int TRANSIENT_EVENT = 4097;
    private static final int VIBRATION_EFFECT_SUPPORT_NO = 2;
    private final boolean DEBUG = true;
    private DynamicEffect mEffect;
    private final String mPackageName;
    private boolean mStarted;
    private final Binder mToken;
    private final VibratorManager mVibratorManager;
    private static boolean mAvailable = isSupportRichtap();
    private static ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private static AtomicInteger mSeq = new AtomicInteger();
    private static int mRichtapMajorVersion = 0;

    private HapticPlayer() {
        mToken = new Binder();
        mStarted = false;
        mPackageName = ActivityThread.currentPackageName();
        Context ctx = ActivityThread.currentActivityThread().getSystemContext();
        mVibratorManager = (VibratorManager) ctx.getSystemService(VibratorManager.class);
    }

    /** @hide */
    public HapticPlayer(@NonNull DynamicEffect effect) {
        this();
        mEffect = effect;
    }

    private boolean isInTheInterval(int data, int a, int b) {
        return data >= a && data <= b;
    }

    private static boolean isSupportRichtap() {
        int support = RichTapVibrationEffect.checkIfRichTapSupport();
        return support != 2;
    }

    public static boolean isAvailable() {
        return mAvailable;
    }

    /** @hide */
    public static int getMajorVersion() {
        int support;
        if ((support = RichTapVibrationEffect.checkIfRichTapSupport()) != 2) {
            int clientCode = (16711680 & support) >> 16;
            int majorVersion = (65280 & support) >> 8;
            int minorVersion = (support & 255);
            Log.d(TAG, "clientCode:" + clientCode + " majorVersion:" + majorVersion + " minorVersion:" + minorVersion);
            return majorVersion;
        }
        return 0;
    }

    /** @hide */
    public static int getMinorVersion() {
        int support;
        if (Build.VERSION.SDK_INT >= ANDROID_VERSIONCODE_O && (support = RichTapVibrationEffect.checkIfRichTapSupport()) != 2) {
            int clientCode = (16711680 & support) >> 16;
            int majorVersion = (65280 & support) >> 8;
            int minorVersion = (support & 255) >> 0;
            Log.d(TAG, "clientCode:" + clientCode + " majorVersion:" + majorVersion + " minorVersion:" + minorVersion);
            return minorVersion;
        }
        return 0;
    }

    private static boolean checkSdkSupport(int richTapMajorVersion, int richTapMinorVersion, int heVersion) {
        Log.d(TAG, "check richtap richTapMajorVersion:" + richTapMajorVersion + " heVersion:" + heVersion);
        if (richTapMajorVersion < 22) {
            Log.e(TAG, "can not support he in richtap version:" + String.format("%x", Integer.valueOf(richTapMajorVersion)));
            return false;
        }
        if (richTapMajorVersion == 22) {
            if (heVersion != 1) {
                Log.e(TAG, "RichTap version is " + String.format("%x", Integer.valueOf(richTapMajorVersion)) + " can not support he version: " + heVersion);
                return false;
            }
        } else if (richTapMajorVersion == 23 && heVersion != 1 && heVersion != 2) {
            return false;
        }
        return true;
    }

    private int[] compileSerializationDataHe_1_0(JSONArray pattern,
                                                 int[] patternHeInfo,
                                                 int eventNumberTmp,
                                                 boolean isCompliance) {
        int mDurationLast = 0;
        int mInd = 0;
        int mRelativeTimeLast = 0;
        int mType;

        JSONObject mEventObject;
        while (true) {
            if (mInd >= eventNumberTmp) break;
            try {
                JSONObject patternObject = pattern.getJSONObject(mInd);
                try {
                    mEventObject = patternObject.getJSONObject("Event");
                    String name = mEventObject.getString("Type");
                    if (!TextUtils.equals("continuous", name)) {
                        if (!TextUtils.equals("transient", name)) {
                            break;
                        }
                        mType = TRANSIENT_EVENT;
                    } else {
                        mType = CONTINUOUS_EVENT;
                    }

                    if (!mEventObject.has("RelativeTime")) {
                        String relativeTime = "event:" +
                                mInd +
                                " don't have relativeTime parameters, set default:" +
                                0;
                        Log.e(TAG, relativeTime);
                    } else {
                        mRelativeTimeLast = mEventObject.getInt("RelativeTime");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }

            if (!isInTheInterval(mRelativeTimeLast, 0, 50000)) break;
            int intensity;
            int frequency;
            JSONObject parametersObject;
            try {
                parametersObject = mEventObject.getJSONObject("Parameters");
                intensity = parametersObject.getInt("Intensity");
                frequency = parametersObject.getInt("RelativeTime");
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }

            if (!isInTheInterval(intensity, 0, 100)) break;
            if (!isInTheInterval(frequency, 0, 100)) break;
            patternHeInfo[0] = mType;
            patternHeInfo[1] = mRelativeTimeLast;
            patternHeInfo[2] = intensity;
            patternHeInfo[3] = frequency;
            if (CONTINUOUS_EVENT == mType) {
                if (!mEventObject.has("Duration")) {
                    Log.e(TAG, "event:" + mInd + " don't have duration parameters,set default:0");
                } else {
                    try {
                        mDurationLast = mEventObject.getInt("Duration");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return null;
                    }
                }
                try {
                    if (!isInTheInterval(mDurationLast, 0, 5000)) {
                        break;
                    } else {
                        patternHeInfo[4] = mDurationLast;
                        patternHeInfo[5] = 0;
                        JSONArray curve = parametersObject.getJSONArray("Curve");
                        int pointCount = Math.min(curve.length(), 16);
                        patternHeInfo[6] = pointCount;
                        int i = 0;
                        while (i < pointCount) {
                            JSONObject curveObject = curve.getJSONObject(i);
                            int pointTime = curveObject.getInt("Time");
                            int pointIntensity = (int) (curveObject.getDouble("Intensity") * 100.0d);
                            try {
                                int pointFrequency = curveObject.getInt("Frequency");
                                if (i == 0) {
                                    Log.e(TAG, "first point's time,  intensity must be 0, frequency must between -100 and 100");
                                    break;
                                }
                                if (i > 0 && i < pointCount - 1 && (!isInTheInterval(pointTime, 0, 5000)
                                        || !isInTheInterval(pointIntensity, 0, 100)
                                        || !isInTheInterval(pointFrequency, -100, 100))) {
                                    Log.e(TAG, "point's time must be less than 5000, intensity must between 0~1, frequency must between -100 and 100");
                                    break;
                                }
                                if (pointCount - 1 == i && (pointTime != mDurationLast || pointIntensity != 0 || !isInTheInterval(pointFrequency, -100, 100))) {
                                    Log.e(TAG, "last point's time must equal with duration, and intensity must be 0, frequency must between -100 and 100");
                                    break;
                                }
                                patternHeInfo[(mInd * 55) + (i * 3) + 7] = pointTime;
                                patternHeInfo[(mInd * 55) + (i * 3) + 8] = pointIntensity;
                                patternHeInfo[(mInd * 55) + (i * 3) + 9] = pointFrequency;
                                i++;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                return null;
            }
            if (!isCompliance) break;
            for (int i = 0; i < 55; i++) {
                Log.d(TAG, "patternHeInfo[" + mInd + "][" + i + "]:" + patternHeInfo[i]);
            }
            mInd++;
        }
        if (!isCompliance) {
            Log.e(TAG, "current he file data, isn't compliance!!!!!!!");
            return null;
        }
        int lastEventIndex = ((eventNumberTmp - 1) * 55);
        if (CONTINUOUS_EVENT == patternHeInfo[lastEventIndex]) {
            Log.d(TAG, "last event type is continuous, totalDuration:" + mRelativeTimeLast + mDurationLast);
        } else {
            Log.d(TAG, "last event type is transient, totalDuration:" + mRelativeTimeLast + 80);
        }
        return patternHeInfo;
    }

    private int[] getSerializationDataHe_1_0(String patternString) {
        int mEventNumberTmp;
        int mLen;
        int[] mPatternHeInfo;
        JSONArray mPattern;
        JSONObject mHapticObject;
        try {
            mHapticObject = new JSONObject(patternString);
            mPattern = mHapticObject.getJSONArray("Pattern");
            mEventNumberTmp = Math.min(mPattern.length(), 16);
            mLen = mEventNumberTmp * 55;
            mPatternHeInfo = new int[mLen];
            return compileSerializationDataHe_1_0(mPattern, mPatternHeInfo, mEventNumberTmp, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    int[] generateSerializationDataHe_2_0(int formatVersion, int heVersion, int totalPattern, int pid, int seq, int indexBase, Pattern[] pattern) {
        int totalPatternLen = 0;
        for (Pattern patternTmp : pattern) {
            totalPatternLen += patternTmp.getPatternDataLen();
        }
        int[] data = new int[5 + totalPatternLen];
        Arrays.fill(data, 0);
        data[0] = formatVersion;
        data[1] = heVersion;
        data[2] = pid;
        data[3] = seq;
        data[4] = data[4] | (totalPattern & 65535);
        int patternNum = pattern.length;
        data[4] = data[4] | ((patternNum << 16) & (-65536));
        int patternOffset = 5;
        int patternOffset2 = indexBase;
        for (Pattern patternTmp2 : pattern) {
            int[] patternData = patternTmp2.generateSerializationPatternData(patternOffset2);
            System.arraycopy(patternData, 0, data, patternOffset, patternData.length);
            patternOffset += patternData.length;
            patternOffset2++;
        }
        return data;
    }

    void sendPatternWrapper(int seq, int pid, int heVersion, int loop, int interval, int amplitude, int freq, int totalPatternNum, int patternIndexOffset, Pattern[] list) {
        int[] patternHe = generateSerializationDataHe_2_0(2, heVersion, totalPatternNum, pid, seq, patternIndexOffset, list);
        try {
            try {
                VibrationEffect createPatternHe = RichTapVibrationEffect.createPatternHeWithParam(patternHe, loop, interval, amplitude, freq);
                VibrationAttributes atr = new VibrationAttributes.Builder().build();
                CombinedVibration combinedEffect = CombinedVibration.createParallel(createPatternHe);
                mVibratorManager.vibrate(Process.myUid(), mPackageName, combinedEffect, "DynamicEffect", atr);
            } catch (Exception e) {
                e.printStackTrace();
                Log.w(TAG, "for createPatternHe, The system doesn't integrate richTap software");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseAndSendDataHe_2_0(int seq, int pid, int heVersion, int loop, int interval, int amplitude, int freq, String patternString) {
        JSONObject hapticObject;
        JSONArray patternArray = null;
        int mPatternNum = 0;
        byte[] patternHeInfo = new byte[0];
        Pattern[] mPatternList = new Pattern[0];
        int durationLast = 0;
        boolean isCompliance = false;
        int wrapperIndex = 0;
        int mRelativeTimeLast = 0;
        int wrapperOffset = 0;
        JSONArray eventArray;
        int mType = 0;
        try {
            hapticObject = new JSONObject(patternString);
            patternArray = hapticObject.getJSONArray("PatternList");
            mPatternNum = patternArray.length();
            patternHeInfo = new byte[mPatternNum * 64];
            try {
                mPatternList = new Pattern[mPatternNum];
                isCompliance = true;
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        while (true) {
            if (mRelativeTimeLast >= mPatternNum) {
                break;
            }
            try {
                Pattern pattern = new Pattern();
                JSONObject patternObject = patternArray.getJSONObject(mRelativeTimeLast);
                pattern.mRelativeTime = patternObject.getInt("AbsoluteTime");
                eventArray = patternObject.getJSONArray("Pattern");
                pattern.mEvent = new Event[eventArray.length()];
                int event = 0;
                while (true) {
                    if (event >= eventArray.length()) {
                        break;
                    }
                    try {
                        JSONObject eventObject = eventArray.getJSONObject(event);
                        JSONObject eventTemp = eventObject.getJSONObject("Event");
                        String name = eventTemp.getString("Type");
                        if (!TextUtils.equals("continuous", name)) {
                            if (!TextUtils.equals("transient", name)) {
                                break;
                            }
                            mType = TRANSIENT_EVENT;
                            pattern.mEvent[event] = new TransientEvent();
                        } else {
                            try {
                                pattern.mEvent[event] = new ContinuousEvent();
                                mType = CONTINUOUS_EVENT;
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        int vibId = eventTemp.getInt("Index");
                        wrapperOffset = (byte) vibId;
                        pattern.mEvent[event].mVibId = wrapperOffset;
                        if (!eventTemp.has("RelativeTime")) {
                            Log.e(TAG, "event:" + mRelativeTimeLast + " don't have relativeTime parameters,BAD he!");
                            return;
                        }
                        mRelativeTimeLast = eventTemp.getInt("RelativeTime");
                        if (event > 0 && mRelativeTimeLast < -1) {
                            Log.e(TAG, "pattern ind:" + mRelativeTimeLast + " event:" + event + " relative time is not right!");
                            return;
                        }
                        try {
                            if (!isInTheInterval(mRelativeTimeLast, 0, 50000)) {
                                try {
                                    break;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            } else {
                                JSONObject parametersObject = eventTemp.getJSONObject("Parameters");
                                int intensity = parametersObject.getInt("Intensity");
                                int frequency = parametersObject.getInt("Frequency");
                                try {
                                    if (!isInTheInterval(intensity, 0, 100)) {
                                        break;
                                    }
                                    if (!isInTheInterval(frequency, 0, 100)) {
                                        break;
                                    }
                                    pattern.mEvent[event].mType = mType;
                                    pattern.mEvent[event].mRelativeTime = mRelativeTimeLast;
                                    pattern.mEvent[event].mIntensity = intensity;
                                    pattern.mEvent[event].mFreq = frequency;
                                    if (!eventTemp.has("Duration")) {
                                        try {
                                            Log.e(TAG, "event:" + mRelativeTimeLast + " don't have duration parameters");
                                            return;
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    } else {
                                        durationLast = eventTemp.getInt("Duration");
                                        try {
                                            if (!isInTheInterval(durationLast, 0, 5000)) {
                                                try {
                                                    break;
                                                } catch (Exception e) {
                                                    e.printStackTrace();
                                                }
                                            } else {
                                                pattern.mEvent[event].mDuration = durationLast;
                                                JSONArray curve = parametersObject.getJSONArray("Curve");
                                                ((ContinuousEvent) pattern.mEvent[event]).mPointNum = (byte) curve.length();
                                                Point[] pointArray = new Point[curve.length()];
                                                int prevPointTime = -1;
                                                int pointLastTime = 0;
                                                int i = 0;
                                                while (true) {
                                                    try {
                                                        int intensity4 = curve.length();
                                                        if (i < intensity4) {
                                                            JSONObject curveObject = curve.getJSONObject(i);
                                                            pointArray[i] = new Point();
                                                            int pointTime = curveObject.getInt("Time");
                                                            int pointIntensity = (int) (curveObject.getDouble("Intensity") * 100.0d);
                                                            int pointFrequency = curveObject.getInt("Frequency");
                                                            if (i == 0 && pointTime != 0) {
                                                                Log.d(TAG, "time of first point is not 0,bad he!");
                                                                return;
                                                            } else if (i > 0 && pointTime < prevPointTime) {
                                                                Log.d(TAG, "point times did not arrange in order,bad he!");
                                                                return;
                                                            } else {
                                                                pointArray[i].mTime = pointTime;
                                                                pointArray[i].mIntensity = pointIntensity;
                                                                pointArray[i].mFreq = pointFrequency;
                                                                pointLastTime = pointTime;
                                                                i++;
                                                                prevPointTime = pointTime;
                                                            }
                                                        } else {
                                                            if (pointLastTime != durationLast) {
                                                                Log.e(TAG, "event:" + mRelativeTimeLast + " point last time do not match duration parameter");
                                                                return;
                                                            } else if (pointArray.length > 0) {
                                                                ((ContinuousEvent) pattern.mEvent[event]).mPoint = pointArray;
                                                            } else {
                                                                Log.d(TAG, "continuous event has nothing in point");
                                                                isCompliance = false;
                                                            }
                                                        }
                                                    } catch (Exception e) {
                                                        e.printStackTrace();
                                                    }
                                                }
                                            }
                                        } catch (Exception e) {
                                            e.printStackTrace();
                                        }
                                    }
                                    if (!isCompliance) {
                                        break;
                                    }
                                    for (int i = 0; i < 55; i++) {
                                        Log.d(TAG, "patternHeInfo[" + mRelativeTimeLast + "][" + i + "]:" + ((int) patternHeInfo[(mRelativeTimeLast * 55) + i]));
                                    }
                                    event++;
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (!isCompliance) {
                    Log.e(TAG, "current he file data, isn't compliance!!!!!!!");
                    return;
                }
                try {
                    mPatternList[mRelativeTimeLast] = pattern;
                    int ind2 = mRelativeTimeLast + 1;
                    if (ind2 >= (wrapperIndex + 1) * 10) {
                        Pattern[] patternWrapper = new Pattern[10];
                        for (int i3 = 0; i3 < 10; i3++) {
                            try {
                                patternWrapper[i3] = mPatternList[wrapperOffset + i3];
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        try {
                            sendPatternWrapper(seq, pid, heVersion, loop, interval, amplitude, freq, mPatternNum, wrapperOffset, patternWrapper);
                            wrapperIndex++;
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
    }

    /** @hide */
    public void applyPatternHeWithString(@NonNull String patternString, int loop, int interval, int amplitude, int freq) {
        int heVersion;
        Log.d(TAG, "play new he api");
        if (loop < 1) {
            Log.e(TAG, "The minimum count of loop pattern is 1");
            return;
        }
        try {
            try {
                JSONObject hapticObject = new JSONObject(patternString);
                if (!mAvailable) {
                    heVersion = 0;
                } else {
                    JSONObject metaData = hapticObject.getJSONObject("Metadata");
                    int heVersion2 = metaData.getInt("Version");
                    int richTapMajorVersion = getMajorVersion();
                    int richTapMinorVersion = getMinorVersion();
                    boolean checkPass = checkSdkSupport(richTapMajorVersion, richTapMinorVersion, heVersion2);
                    if (checkPass) {
                        heVersion = heVersion2;
                    } else {
                        Log.e(TAG, "richtap version check failed, richTapMajorVersion:" + String.format("%x02", richTapMajorVersion) + " heVersion:" + heVersion2);
                        return;
                    }
                }
                if (heVersion != 1) {
                    if (heVersion == 2) {
                        int seq = mSeq.getAndIncrement();
                        int pid = Process.myPid();
                        parseAndSendDataHe_2_0(seq, pid, heVersion, loop, interval, amplitude, freq, patternString);
                        return;
                    }
                    Log.e(TAG, "unsupport he version heVersion:" + heVersion);
                    return;
                }
                try {
                    int[] patternHeInfo = getSerializationDataHe_1_0(patternString);
                    if (patternHeInfo == null) {
                        Log.e(TAG, "serialize he failed!! ,heVersion:" + heVersion);
                        return;
                    }
                    int len = patternHeInfo.length;
                    try {
                        int[] realPatternHeInfo = new int[len + 1];
                        realPatternHeInfo[0] = 3;
                        System.arraycopy(patternHeInfo, 0, realPatternHeInfo, 1, patternHeInfo.length);
                        VibrationEffect createPatternHe = RichTapVibrationEffect.createPatternHeWithParam(realPatternHeInfo, loop, interval, amplitude, freq);
                        VibrationAttributes atr = new VibrationAttributes.Builder().build();
                        CombinedVibration combinedEffect = CombinedVibration.createParallel(createPatternHe);
                        mVibratorManager.vibrate(Process.myUid(), mPackageName, combinedEffect, "DynamicEffect", atr);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.w(TAG, "for createPatternHe, The system doesn't integrate richTap software");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** @hide */
    public int getRealLooper(int looper) {
        if (looper < 0) {
            if (looper == -1) {
                return Integer.MAX_VALUE;
            }
            return 0;
        } else if (looper == 0) {
            return 1;
        } else {
            return looper;
        }
    }

    /** @hide */
    public void start(int loop) {
        Log.d(TAG, "start play pattern loop:" + loop);
        if (mEffect == null) {
            Log.e(TAG, "effect is null,do nothing");
        } else {
            final int realLooper = getRealLooper(loop);
            if (realLooper < 0) {
                Log.e(TAG, "looper is not correct realLooper:" + realLooper);
                return;
            }
            mExecutor.execute(() -> {
                String patternJson = null;
                Log.d(TAG, "haptic play start!");
                long startRunTime = System.currentTimeMillis();
                try {
                    mStarted = true;
                    patternJson = mEffect.getPatternInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w(TAG, "for createPatternHe, The system doesn't integrate richTap software");
                }
                if (patternJson == null) {
                    Log.d(TAG, "pattern is null,can not play!");
                    return;
                }
                applyPatternHeWithString(patternJson, realLooper, 0, 255, 0);
                long useTime = System.currentTimeMillis() - startRunTime;
                Log.d(TAG, "run vibrate thread use time:" + useTime);
            });
        }
    }

    /** @hide */
    public void start(int loop, final int interval, final int amplitude) {
        Log.d(TAG, "start with loop:" + loop + " interval:" + interval + " amplitude:" + amplitude);
        boolean checkResult = checkParam(interval, amplitude, -1);
        if (!checkResult) {
            Log.e(TAG, "wrong start param");
        } else if (mEffect == null) {
            Log.e(TAG, "effect is null,do nothing");
        } else {
            final int realLooper = getRealLooper(loop);
            if (realLooper < 0) {
                Log.e(TAG, "looper is not correct realLooper:" + realLooper);
                return;
            }
            mExecutor.execute(() -> {
                String patternJson = null;
                Log.d(TAG, "haptic play start!");
                long startRunTime = System.currentTimeMillis();
                try {
                    mStarted = true;
                    patternJson = mEffect.getPatternInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w(TAG, "for createPatternHe, The system doesn't integrate richTap software");
                }
                if (patternJson == null) {
                    Log.d(TAG, "pattern is null,can not play!");
                    return;
                }
                applyPatternHeWithString(patternJson, realLooper, interval, amplitude, 0);
                long useTime = System.currentTimeMillis() - startRunTime;
                Log.d(TAG, "run vibrate thread use time:" + useTime);
            });
        }
    }

    /** @hide */
    public void start(int loop, final int interval, final int amplitude, final int freq) {
        Log.d(TAG, "start with loop:" + loop + " interval:" + interval + " amplitude:" + amplitude + " freq:" + freq);
        boolean checkResult = checkParam(interval, amplitude, freq);
        if (!checkResult) {
            Log.e(TAG, "wrong start param");
        } else if (mEffect == null) {
            Log.e(TAG, "effect is null,do nothing");
        } else {
            final int realLooper = getRealLooper(loop);
            if (realLooper < 0) {
                Log.e(TAG, "looper is not correct realLooper:" + realLooper);
                return;
            }
            mExecutor.execute(() -> {
                String patternJson = null;
                Log.d(TAG, "haptic play start!");
                long startRunTime = System.currentTimeMillis();
                try {
                    mStarted = true;
                    patternJson = mEffect.getPatternInfo();
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w(TAG, "for createPatternHe, The system doesn't integrate richTap software");
                }
                if (patternJson == null) {
                    Log.d(TAG, "pattern is null,can not play!");
                    return;
                }
                applyPatternHeWithString(patternJson, realLooper, interval, amplitude, freq);
                long useTime = System.currentTimeMillis() - startRunTime;
                Log.d(TAG, "run vibrate thread use time:" + useTime);
            });
        }
    }

    private boolean checkParam(int interval, int amplitude, int freq) {
        if (interval < 0 && interval != -1) {
            Log.e(TAG, "wrong interval param");
            return false;
        } else if (freq < 0 && freq != -1) {
            Log.e(TAG, "wrong freq param");
            return false;
        } else if ((amplitude < 0 && amplitude != -1) || amplitude > 255) {
            Log.e(TAG, "wrong amplitude param");
            return false;
        } else {
            return true;
        }
    }

    /** @hide */
    public void applyPatternHeParam(final int interval, final int amplitude, final int freq) {
        Log.d(TAG, "start with  interval:" + interval + " amplitude:" + amplitude + " freq:" + freq);
        boolean checkResult = checkParam(interval, amplitude, freq);
        if (!checkResult) {
            Log.e(TAG, "wrong param!");
            return;
        }
        try {
            if (mStarted) {
                mExecutor.execute(() -> {
                    try {
                        VibrationEffect createPatternHe = RichTapVibrationEffect.createPatternHeParameter(interval, amplitude, freq);
                        CombinedVibration combinedEffect = CombinedVibration.createParallel(createPatternHe);
                        mVibratorManager.vibrate(Process.myUid(), mPackageName, combinedEffect, "DynamicEffect", null);
                        Log.d(TAG, "haptic apply param");
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.w(TAG, "for createPatternHe, The system doesn't integrate richTap software");
                    }
                });
            } else {
                Log.d(TAG, "haptic player has not started");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.w(TAG, "The system doesn't integrate richTap software");
        }
    }

    /** @hide */
    public void updateInterval(int interval) {
        applyPatternHeParam(interval, -1, -1);
    }

    /** @hide */
    public void updateAmplitude(int amplitude) {
        applyPatternHeParam(-1, amplitude, -1);
    }

    /** @hide */
    public void updateFrequency(int freq) {
        applyPatternHeParam(-1, -1, freq);
    }

    /** @hide */
    public void updateParameter(int interval, int amplitude, int freq) {
        applyPatternHeParam(interval, amplitude, freq);
    }

    /** @hide */
    public void stop() {
        if (mStarted) {
            mExecutor.execute(() -> {
                try {
                    VibrationEffect createPatternHe = RichTapVibrationEffect.createPatternHeParameter(0, 0, 0);
                    CombinedVibration combinedEffect = CombinedVibration.createParallel(createPatternHe);
                    mVibratorManager.vibrate(Process.myUid(), mPackageName, combinedEffect, "DynamicEffect", null);
                    Log.d(TAG, "haptic play stop");
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.w(TAG, "for createPatternHe, The system doesn't integrate richTap software");
                }
            });
        } else {
            Log.d(TAG, "haptic player has not started");
        }
    }

/*    @Override
    public void close() throws Exception { }*/

    public abstract static class Event {
        int mDuration;
        int mFreq;
        int mIntensity;
        int mLen;
        int mRelativeTime;
        int mType;
        int mVibId;

        abstract int[] generateData();

        Event() {
        }

        @NonNull
        @Override
        public String toString() {
            return "Event{mType=" + mType + ", mVibId=" + mVibId + ", mRelativeTime=" + mRelativeTime + ", mIntensity=" + mIntensity + ", mFreq=" + mFreq + ", mDuration=" + mDuration + '}';
        }
    }

    public static class TransientEvent extends Event {
        TransientEvent() {
            super();
            mLen = 7;
        }

        @Override
        int[] generateData() {
            int[] data = new int[mLen];
            Arrays.fill(data, 0);
            data[0] = mType;
            data[1] = mLen - 2;
            data[2] = mVibId;
            data[3] = mRelativeTime;
            data[4] = mIntensity;
            data[5] = mFreq;
            data[6] = mDuration;
            return data;
        }
    }

    public static class Point {
        int mFreq;
        int mIntensity;
        int mTime;

        Point() {
        }
    }

    public static class ContinuousEvent extends Event {
        Point[] mPoint;
        int mPointNum;

        ContinuousEvent() {
            super();
        }

        @Override
        int[] generateData() {
            int[] data = new int[(mPointNum * 3) + 8];
            Arrays.fill(data, 0);
            data[0] = mType;
            data[1] = ((mPointNum * 3) + 8) - 2;
            data[2] = mVibId;
            data[3] = mRelativeTime;
            data[4] = mIntensity;
            data[5] = mFreq;
            data[6] = mDuration;
            data[7] = mPointNum;
            int offset = 8;
            for (int i = 0; i < mPointNum; i++) {
                data[offset] = mPoint[i].mTime;
                int offset2 = offset + 1;
                data[offset2] = mPoint[i].mIntensity;
                int offset3 = offset2 + 1;
                data[offset3] = mPoint[i].mFreq;
                offset = offset3 + 1;
            }
            return data;
        }

        @NonNull
        @Override
        public String toString() {
            return "Continuous{mPointNum=" + mPointNum + ", mPoint=" + Arrays.toString(mPoint) + '}';
        }
    }

    public static class Pattern {
        Event[] mEvent;
        int mRelativeTime;

        Pattern() {
        }

        int getPatternEventLen() {
            int len = 0;
            for (Event event : mEvent) {
                if (event.mType == 4096) {
                    len += (((ContinuousEvent) event).mPointNum * 3) + 8;
                } else if (event.mType == 4097) {
                    len += 7;
                }
            }
            return len;
        }

        int getPatternDataLen() {
            int eventLen = getPatternEventLen();
            return eventLen + 3;
        }

        int[] generateSerializationPatternData(int index) {
            int dataLen = getPatternDataLen();
            int[] data = new int[dataLen];
            Arrays.fill(data, 0);
            data[0] = index;
            data[1] = mRelativeTime;
            Event[] eventArr = mEvent;
            data[2] = eventArr.length;
            int offset = 3;
            for (Event event : eventArr) {
                int[] eventData = event.generateData();
                System.arraycopy(eventData, 0, data, offset, eventData.length);
                offset += eventData.length;
            }
            return data;
        }
    }
}
