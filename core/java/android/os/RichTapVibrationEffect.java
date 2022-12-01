/*
 * Copyright (C) 2022 Paranoid Android
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.os;

import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.SuppressLint;
import android.annotation.TestApi;
import android.content.res.Resources;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.VibrationEffect;
import android.util.Log;

import com.android.internal.R;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class RichTapVibrationEffect {

    private static final String TAG = "RichTapVibrationEffect";

    @NonNull public static final Parcelable.Creator<VibrationEffect> CREATOR;
    private static String DEFAULT_EXT_PREBAKED_STRENGTH = null;
    private static final int EFFECT_ID_START = 4096;
    private static final int PARCEL_TOKEN_EXT_PREBAKED = 501;
    private static final int PARCEL_TOKEN_ENVELOPE = 502;
    private static final int PARCEL_TOKEN_PATTERN_HE = 503;
    private static final int PARCEL_TOKEN_PATTERN_HE_LOOP_PARAMETER = 504;

    private static final int VIBRATION_EFFECT_SUPPORT_UNKNOWN = 0;
    private static final int VIBRATION_EFFECT_SUPPORT_YES = 1;
    private static final int VIBRATION_EFFECT_SUPPORT_NO = 2;

    private static Map<String, Integer> mEffectStrength;

    static {
        mEffectStrength = new HashMap();
        mEffectStrength.put("LIGHT", VIBRATION_EFFECT_SUPPORT_UNKNOWN);
        mEffectStrength.put("MEDIUM", VIBRATION_EFFECT_SUPPORT_YES);
        mEffectStrength.put("STRONG", VIBRATION_EFFECT_SUPPORT_NO);
        DEFAULT_EXT_PREBAKED_STRENGTH = "STRONG";
        CREATOR = new Parcelable.Creator<VibrationEffect>() {
            @Override
            public VibrationEffect createFromParcel(Parcel in) {
                int token = in.readInt();
                Log.d(TAG, "read token: " + token + "!");
                switch (token) {
                    case PARCEL_TOKEN_EXT_PREBAKED:
                        return new ExtPrebaked(in);
                    case PARCEL_TOKEN_ENVELOPE:
                        return new Envelope(in);
                    case PARCEL_TOKEN_PATTERN_HE:
                        return new PatternHe(in);
                    case PARCEL_TOKEN_PATTERN_HE_LOOP_PARAMETER:
                        return new PatternHeParameter(in);
                    default:
                        throw new IllegalStateException("Unexpected vibration event type token in parcel.");
                }
            }

            @Override
            public VibrationEffect[] newArray(int size) {
                return new VibrationEffect[size];
            }
        };
    }

    /** @hide */
	public RichTapVibrationEffect() { }

    /** @hide */
    @NonNull
    public static VibrationEffect createExtPreBaked(int effectId, int strength) {
        mEffectStrength.get(DEFAULT_EXT_PREBAKED_STRENGTH).intValue();
        VibrationEffect effect = new ExtPrebaked(effectId + EFFECT_ID_START, strength);
        effect.validate();
        return effect;
    }

    /** @hide */
    @NonNull
    public static VibrationEffect createEnvelope(@NonNull int[] relativeTime, @NonNull int[] scale, 
        @NonNull int[] freq, boolean steepMode, int amplitude) {
        VibrationEffect effect = new Envelope(relativeTime, scale, freq, steepMode, amplitude);
        effect.validate();
        return effect;
    }

    /** @hide */
    @NonNull
    public static VibrationEffect createPatternHeParameter(int interval, int amplitude, int freq) {
        VibrationEffect effect = new PatternHeParameter(interval, amplitude, freq);
        effect.validate();
        return effect;
    }

    /** @hide */
    @NonNull
    public static VibrationEffect createPatternHeWithParam(@NonNull int[] patternInfo, int looper, 
        int interval, int amplitude, int freq) {
        VibrationEffect effect = new PatternHe(patternInfo, looper, interval, amplitude, freq);
        effect.validate();
        return effect;
    }

    /** @hide */
    @TestApi
    public static final class ExtPrebaked extends VibrationEffect implements Parcelable {
        private int mEffectId;
        private int mStrength;

        ExtPrebaked(@NonNull Parcel in) {
            this(in.readInt(), in.readInt());
        }

        /** @hide */
        public ExtPrebaked(int effectId, int strength) {
            mEffectId = effectId;
            mStrength = strength;
        }

        public int getId() {
            return mEffectId;
        }

        public int getScale() {
            return mStrength;
        }

        /** @hide */
        @NonNull
        @Override
        public ExtPrebaked resolve(int defaultAmplitude) {
            return this;
        }

        /** @hide */
        @NonNull
        @Override
        public ExtPrebaked scale(float scaleFactor) {
            return this;
        }

        @Override
        public long getDuration() {
            return -1L;
        }

        /** @hide */
        @Override
        public void validate() {
            if (mEffectId < 0) {
                throw new IllegalArgumentException("Unknown ExtPrebaked effect type (value=" + mEffectId + ")");
            }
            int i = mStrength;
            if (i < 1 || i > 100) {
                throw new IllegalArgumentException("mStrength must be between 1 and 100 inclusive (mStrength=" + mStrength + ")");
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof ExtPrebaked)) {
                return false;
            }
            ExtPrebaked other = (ExtPrebaked) o;
            return mEffectId == other.mEffectId;
        }

        @Override
        public int hashCode() {
            return mEffectId;
        }

		@Override
        public String toString() {
            return "ExtPrebaked{mEffectId=" + mEffectId
                    + ", mStrength=" + mStrength
                    + "}";
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeInt(PARCEL_TOKEN_EXT_PREBAKED);
            out.writeInt(mEffectId);
            out.writeInt(mStrength);
        }

        @NonNull
        public static final Creator<ExtPrebaked> CREATOR =
                new Creator<ExtPrebaked>() {
                    @Override
                    public ExtPrebaked createFromParcel(Parcel in) {
                        in.readInt();
                        return new ExtPrebaked(in);
                    }

                    @Override
                    public ExtPrebaked[] newArray(int size) {
                        return new ExtPrebaked[size];
                    }
                };
    }

    /** @hide */
    @TestApi
    public static final class Envelope extends VibrationEffect implements Parcelable {
        private int mAmplitude;
        private int[] mFreq;
        private int[] mRelativeTime;
        private int[] mScale;
        private boolean mSteepMode;

        Envelope(@NonNull Parcel in) {
            this(in.createIntArray(), in.createIntArray(), in.createIntArray(), in.readInt() == 1, in.readInt());
        }

        /** @hide */
        public Envelope(@NonNull int[] relativeTime, @NonNull int[] scale, @NonNull int[] freq, boolean steepMode, int amplitude) {
            mRelativeTime = Arrays.copyOf(relativeTime, 4);
            mScale = Arrays.copyOf(scale, 4);
            mFreq = Arrays.copyOf(freq, 4);
            mSteepMode = steepMode;
            mAmplitude = amplitude;
        }

        @NonNull
        public int[] getRelativeTimeArr() {
            return mRelativeTime;
        }

        @NonNull
        public int[] getScaleArr() {
            return mScale;
        }

        @NonNull
        public int[] getFreqArr() {
            return mFreq;
        }

        public boolean isSteepMode() {
            return mSteepMode;
        }

        public int getAmplitude() {
            return mAmplitude;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /** @hide */
        @NonNull
        @Override
        public Envelope resolve(int defaultAmplitude) {
            return this;
        }

        /** @hide */
        @NonNull
        @Override
        public Envelope scale(float scaleFactor) {
            return this;
        }

        @Override
        public long getDuration() {
            return -1L;
        }

        /** @hide */
        @Override
        public void validate() {
            for (int i = 0; i < 4; i++) {
                if (mRelativeTime[i] < 0) {
                    throw new IllegalArgumentException("relative time can not be negative");
                }
                if (mScale[i] < 0) {
                    throw new IllegalArgumentException("scale can not be negative");
                }
                if (mFreq[i] < 0) {
                    throw new IllegalArgumentException("freq must be positive");
                }
            }
            int amplitude = mAmplitude;
            if (amplitude < -1 || amplitude == 0 || amplitude > 255) {
                throw new IllegalArgumentException("amplitude must either be DEFAULT_AMPLITUDE, or between 1 and 255 inclusive (amplitude=" + mAmplitude + ")");
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof Envelope)) {
                return false;
            }
            Envelope other = (Envelope) o;
            int[] timeAttr = other.getRelativeTimeArr();
            int[] scaleArr = other.getScaleArr();
            int[] freqArr = other.getFreqArr();
            return mAmplitude == other.getAmplitude() 
                && Arrays.equals(timeAttr, mRelativeTime) 
                && Arrays.equals(scaleArr, mScale) 
                && Arrays.equals(freqArr, mFreq) 
                && other.isSteepMode() == mSteepMode;
        }

        @Override
        public int hashCode() {
            return mRelativeTime[2] + mScale[2] + mFreq[2];
        }

        @Override
        public String toString() {
            return "Envelope: {RelativeTime=" + mRelativeTime 
                    + ", Scale = " + mScale 
                    + ", Frequency = " + mFreq 
                    + ", SteepMode = " + mSteepMode 
                    + ", Amplitude = " + mAmplitude + "}";
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeInt(PARCEL_TOKEN_ENVELOPE);
            out.writeIntArray(mRelativeTime);
            out.writeIntArray(mScale);
            out.writeIntArray(mFreq);
            out.writeInt(mSteepMode ? 1 : 0);
            out.writeInt(mAmplitude);
        }

        @NonNull
        public static final Creator<Envelope> CREATOR =
                new Creator<Envelope>() {
                    @Override
                    public Envelope createFromParcel(Parcel in) {
                        in.readInt();
                        return new Envelope(in);
                    }

                    @Override
                    public Envelope[] newArray(int size) {
                        return new Envelope[size];
                    }
                };
    }

    /** @hide */
    @TestApi
    @SuppressLint("UserHandleName")
    public static final class PatternHeParameter extends VibrationEffect implements Parcelable {
        private final String TAG = "PatternHeParameter";
        private final int mAmplitude;
        private final int mFreq;
        private final int mInterval;

        PatternHeParameter(@NonNull Parcel in) {
            this(in.readInt(), in.readInt(), in.readInt());
        }

        /** @hide */
        public PatternHeParameter(int interval, int amplitude, int freq) {
            mInterval = interval;
            mAmplitude = amplitude;
            mFreq = freq;
            Log.d(TAG, "mInterval:" + mInterval + " mAmplitude:" + mAmplitude + " mFreq:" + mFreq);
        }

        public int getInterval() {
            return mInterval;
        }

        public int getAmplitude() {
            return mAmplitude;
        }

        public int getFreq() {
            return mFreq;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /** @hide */
        @NonNull
        @Override
        public PatternHeParameter resolve(int defaultAmplitude) {
            return this;
        }

        /** @hide */
        @NonNull
        @Override
        public PatternHeParameter scale(float scaleFactor) {
            return this;
        }

        @Override
        public long getDuration() {
            return -1L;
        }

        /** @hide */
        @Override
        public void validate() {
            int amplitude = mAmplitude;
            if (amplitude < -1 || amplitude > 255 || mInterval < -1 || mFreq < -1) {
                throw new IllegalArgumentException("mAmplitude=" + mAmplitude + " mInterval=" + mInterval + " mFreq=" + mFreq);
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof PatternHeParameter)) {
                return false;
            }
            PatternHeParameter other = (PatternHeParameter) o;
            int interval = other.getInterval();
            int amplitude = other.getAmplitude();
            int freq = other.getFreq();
            return interval == mInterval && amplitude == mAmplitude && freq == mFreq;
        }

        @Override
        public int hashCode() {
            int result = 14 + (mInterval * 37);
            return result + (mAmplitude * 37);
        }

        @Override
        public String toString() {
            return "PatternHeParameter: {mAmplitude=" + mAmplitude 
                + "}{mInterval=" + mInterval 
				+ "}";
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeInt(PARCEL_TOKEN_PATTERN_HE_LOOP_PARAMETER);
            out.writeInt(mInterval);
            out.writeInt(mAmplitude);
            out.writeInt(mFreq);
            Log.d(TAG, "writeToParcel mInterval:" + mInterval + " mAmplitude:" + mAmplitude + " mFreq:" + mFreq);
        }

        @NonNull
        public static final Creator<PatternHeParameter> CREATOR =
                new Creator<PatternHeParameter>() {
                    @Override
                    public PatternHeParameter createFromParcel(Parcel in) {
                        in.readInt();
                        return new PatternHeParameter(in);
                    }

                    @Override
                    public PatternHeParameter[] newArray(int size) {
                        return new PatternHeParameter[size];
                    }
                };
    }

    /** @hide */
	@TestApi
    public static final class PatternHe extends VibrationEffect implements Parcelable {
        private int mAmplitude;
        private long mDuration;
        private int mEventCount;
        private int mFreq;
        private int mInterval;
        private int mLooper;
        private int[] mPatternInfo;

        PatternHe(@NonNull Parcel in) {
            mDuration = 100L;
            mPatternInfo = in.createIntArray();
            mLooper = in.readInt();
            mInterval = in.readInt();
            mAmplitude = in.readInt();
            mFreq = in.readInt();
        }

        /** @hide */
        public PatternHe(@NonNull int[] patternInfo, long duration, int eventCount) {
            mDuration = 100L;
            mPatternInfo = patternInfo;
            mDuration = duration;
            mEventCount = eventCount;
        }

        /** @hide */
        public PatternHe(@NonNull int[] patternInfo, int looper, int interval, int amplitude, int freq) {
            mDuration = 100L;
            mPatternInfo = patternInfo;
            mLooper = looper;
            mInterval = interval;
            mFreq = freq;
            mAmplitude = amplitude;
            mDuration = 100L;
            mEventCount = 0;
        }

        @Override
        public long getDuration() {
            return mDuration;
        }

        public int getEventCount() {
            return mEventCount;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        /** @hide */
        @NonNull
        @Override
        public PatternHe resolve(int defaultAmplitude) {
            return this;
        }

        /** @hide */
        @NonNull
        @Override
        public PatternHe scale(float scaleFactor) {
            return this;
        }

        @NonNull
        public int[] getPatternInfo() {
            return mPatternInfo;
        }

        public int getLooper() {
            return mLooper;
        }

        public int getInterval() {
            return mInterval;
        }

        public int getAmplitude() {
            return mAmplitude;
        }

        public int getFreq() {
            return mFreq;
        }

        /** @hide */
        @Override
        public void validate() {
            if (mDuration <= 0) {
                throw new IllegalArgumentException("duration must be positive (duration=" + mDuration + ")");
            }
        }

        @Override
        public boolean equals(@Nullable Object o) {
            if (!(o instanceof PatternHe)) {
                return false;
            }
            PatternHe other = (PatternHe) o;
            return other.mDuration == mDuration && other.mPatternInfo == mPatternInfo;
        }

        @Override
        public int hashCode() {
            int result = 17 + (((int) mDuration) * 37);
            return result + (mEventCount * 37);
        }

        @Override
        public String toString() {
            return "PatternHe{mLooper=" + mLooper 
                + ", mInterval=" + mInterval 
                + "}";
        }

        @Override
        public void writeToParcel(@NonNull Parcel out, int flags) {
            out.writeInt(PARCEL_TOKEN_PATTERN_HE);
            out.writeIntArray(mPatternInfo);
            out.writeInt(mLooper);
            out.writeInt(mInterval);
            out.writeInt(mAmplitude);
            out.writeInt(mFreq);
        }

        @NonNull
        public static final Creator<PatternHe> CREATOR =
                new Creator<PatternHe>() {
                    @Override
                    public PatternHe createFromParcel(Parcel in) {
                        in.readInt();
                        return new PatternHe(in);
                    }

                    @Override
                    public PatternHe[] newArray(int size) {
                        return new PatternHe[size];
                    }
                };
    }

    /** @hide */
    public static boolean isSupported() {
        return Resources.getSystem().getBoolean(R.bool.config_usesRichtapVibration);
    }

    /** @hide */
    public static final boolean isExtendedEffect(int token) {
        switch (token) {
            case PARCEL_TOKEN_EXT_PREBAKED:
            case PARCEL_TOKEN_ENVELOPE:
            case PARCEL_TOKEN_PATTERN_HE:
            case PARCEL_TOKEN_PATTERN_HE_LOOP_PARAMETER:
                return true;
            default:
                return false;
        }
    }

    /** @hide */
    @NonNull
    public static final VibrationEffect createExtendedEffect(@NonNull Parcel in) {
        int offset = in.dataPosition() - 4;
        in.setDataPosition(offset);
        return CREATOR.createFromParcel(in);
    }
}
