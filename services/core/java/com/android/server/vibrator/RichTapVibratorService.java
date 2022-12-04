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

package com.android.server.vibrator;

import android.annotation.SuppressLint;
import android.content.Context;
import android.hardware.vibrator.IVibrator;
import android.os.Binder;
import android.os.CombinedVibration;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RichTapVibrationEffect;
import android.os.ServiceManager;
import android.os.VibrationEffect;
import android.telephony.TelephonyManager;
import android.util.Slog;

import vendor.aac.hardware.richtap.vibrator.IRichtapCallback;
import vendor.aac.hardware.richtap.vibrator.IRichtapVibrator;

public class RichTapVibratorService {

    private static final String TAG = "RichTapVibratorService";

    static SenderId mCurrentSenderId = new SenderId(0, 0);
    private IRichtapCallback mCallback;
    private volatile IRichtapVibrator sRichtapVibratorService = null;
    private VibHalDeathRecipient mHalDeathLinker = null;
    private boolean mSupportRichTap;

    private IRichtapVibrator getRichtapService() {
        synchronized (RichTapVibratorService.class) {
            if (this.sRichtapVibratorService == null) {
                String vibratorDescriptor = "android$hardware$vibrator$IVibrator".replace('$', '.') + "/default";
                Slog.d(TAG, "vibratorDescriptor:" + vibratorDescriptor);
                IVibrator service = IVibrator.Stub.asInterface(ServiceManager.getService(vibratorDescriptor));
                if (service == null) {
                    Slog.d(TAG, "can not get hal service");
                    return null;
                }
                Slog.d(TAG, "vibratorHalService:" + service);
                try {
                    Slog.d(TAG, "Capabilities:" + service.getCapabilities());
                } catch (Exception e) {
                    Slog.d(TAG, "getCapabilities failed", e);
                }
                try {
                    IBinder binder = service.asBinder().getExtension();
                    if (binder != null) {
                        sRichtapVibratorService = IRichtapVibrator.Stub.asInterface(Binder.allowBlocking(binder));
                        mHalDeathLinker = new VibHalDeathRecipient(this);
                        mCurrentSenderId.reset();
                        binder.linkToDeath(mHalDeathLinker, 0);
                    } else {
                        sRichtapVibratorService = null;
                        Slog.e(TAG, "getExtension == null");
                    }
                } catch (Exception e) {
                    Slog.e(TAG, "getExtension fail", e);
                }
            }
            return this.sRichtapVibratorService;
        }
    }

    public RichTapVibratorService(boolean supportRichTap, IRichtapCallback callback) {
        mSupportRichTap = supportRichTap;
        mCallback = callback;
    }

    public boolean disposeTelephonyCallState(Context context) {
        boolean calling = false;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (TelephonyManager.CALL_STATE_OFFHOOK == tm.getCallState()) {
            calling = true;
        }
        if (calling) {
            Slog.i(TAG, "current is calling state, stop richtap effect loop");
            richTapVibratorStop();
        }
        return calling;
    }

    public boolean disposeRichtapEffectParams(CombinedVibration combEffect) {
        if (!(combEffect instanceof CombinedVibration.Mono)) {
            return false;
        }
        RichTapVibrationEffect.PatternHeParameter effect = ((CombinedVibration.Mono) combEffect).getEffect();
        if (effect instanceof RichTapVibrationEffect.PatternHeParameter) {
            RichTapVibrationEffect.PatternHeParameter param = effect;
            int interval = param.getInterval();
            int amplitude = param.getAmplitude();
            int freq = param.getFreq();
            Slog.d(TAG, "receive data  interval:" + interval + " amplitude:" + amplitude + " freq:" + freq);
            try {
                IRichtapVibrator service = getRichtapService();
                if (service != null) {
                    Slog.d(TAG, "aac richtap performHeParam");
                    service.performHeParam(interval, amplitude, freq, this.mCallback);
                    return true;
                }
                return true;
            } catch (Exception e) {
                Slog.e(TAG, "aac richtap performHeParam fail.", e);
                return true;
            }
        }
        Slog.d(TAG, "none richtap effect, do nothing!!");
        return false;
    }

    public void richTapVibratorOn(long millis) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                Slog.d(TAG, "aac richtap doVibratorOn");
                service.on((int) millis, this.mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOn fail.", e);
        }
    }

    public void richTapVibratorOff() {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                Slog.d(TAG, "aac richtap doVibratorOff");
                service.off(this.mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOff fail.", e);
        }
    }

    public void richTapVibratorSetAmplitude(int amplitude) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                Slog.d(TAG, "aac richtap doVibratorSetAmplitude");
                service.setAmplitude(amplitude, this.mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorSetAmplitude fail.", e);
        }
    }

    public int richTapVibratorPerform(int id, byte scale) {
        int timeout = 0;
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                Slog.d(TAG, "perform richtap vibrator");
                timeout = service.perform(id, scale, this.mCallback);
                Slog.d(TAG, "aac richtap perform timeout:" + timeout);
                return timeout;
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap perform fail.", e);
        }
        return timeout;
    }

    public int getRichTapPrebakStrength(int effectStrength) {
        switch (effectStrength) {
            case 0:
                return 69;
            case 1:
                return 89;
            case 2:
                return 99;
            default:
                Slog.d(TAG, "wrong Effect Strength!!");
                return 0;
        }
    }

    @SuppressLint("DefaultLocale")
    public void richTapVibratorOnEnvelope(int[] relativeTime, int[] scaleArr, int[] freqArr, boolean steepMode, int amplitude) {
        int[] params = new int[12];
        for (int i = 0; i < relativeTime.length; i++) {
            params[i * 3] = relativeTime[i];
            params[(i * 3) + 1] = scaleArr[i];
            params[(i * 3) + 2] = freqArr[i];
            String temp = String.format("relativeTime, scale, freq = { %d, %d, %d }", params[i * 3], params[(i * 3) + 1], params[(i * 3) + 2]);
            Slog.d(TAG, temp);
        }
        Slog.d(TAG, "vibrator perform envelope");
        richTapVibratorSetAmplitude(amplitude);
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                Slog.d(TAG, "aac richtap performEnvelope");
                service.performEnvelope(params, steepMode, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap performEnvelope fail.", e);
        }
    }

    public void richTapVibratorOnPatternHe(VibrationEffect effect) {
        RichTapVibrationEffect.PatternHe newEffect = (RichTapVibrationEffect.PatternHe) effect;
        int[] pattern = newEffect.getPatternInfo();
        int looper = newEffect.getLooper();
        int interval = newEffect.getInterval();
        int amplitude = newEffect.getAmplitude();
        int freq = newEffect.getFreq();
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                service.performHe(looper, interval, amplitude, freq, pattern, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOnPatternHe fail.", e);
        }
    }

    public void richTapVibratorOnRawPattern(int[] pattern, int amplitude, int freq) {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                service.performHe(1, 0, amplitude, freq, pattern, mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorOnPatternHe fail.", e);
        }
    }

    public void richTapVibratorStop() {
        try {
            IRichtapVibrator service = getRichtapService();
            if (service != null) {
                Slog.d(TAG, "aac richtap doVibratorStop");
                Slog.d(TAG, "richtap service stop!!");
                service.stop(this.mCallback);
            }
        } catch (Exception e) {
            Slog.e(TAG, "aac richtap doVibratorStop fail.", e);
        }
    }

    void resetHalServiceProxy() {
        sRichtapVibratorService = null;
    }

    public static final class VibHalDeathRecipient implements IBinder.DeathRecipient {
        RichTapVibratorService mRichTapService;

        VibHalDeathRecipient(RichTapVibratorService richtapService) {
            mRichTapService = richtapService;
        }

        @Override
        public void binderDied() {
            Slog.d("RichTapVibratorService", "vibrator hal died,should reset hal proxy!!");
            synchronized (VibHalDeathRecipient.class) {
                if (mRichTapService != null) {
                    Slog.d(TAG, "vibrator hal reset hal proxy");
                    mRichTapService.resetHalServiceProxy();
                }
            }
        }
    }

    public boolean checkIfPrevPatternData(SenderId senderId) {
        if (senderId.getPid() == mCurrentSenderId.getPid() && senderId.getSeq() == mCurrentSenderId.getSeq()) {
            return true;
        }
        return false;
    }

    public void setCurrentSenderId(SenderId senderId) {
        mCurrentSenderId.setPid(senderId.getPid());
        mCurrentSenderId.setSeq(senderId.getSeq());
    }

    public void resetCurrentSenderId() {
        mCurrentSenderId.reset();
    }

    public SenderId getSenderId(VibrationEffect effect) {
        if (effect instanceof RichTapVibrationEffect.PatternHe) {
            RichTapVibrationEffect.PatternHe patternHe = (RichTapVibrationEffect.PatternHe) effect;
            int[] patternData = patternHe.getPatternInfo();
            if (patternData != null && patternData.length > 0) {
                int versionOrType = patternData[0];
                if (versionOrType == 2) {
                    int pid = patternData[2];
                    int seq = patternData[3];
                    Slog.d(TAG, "get sender id pid:" + pid + " seq:" + seq);
                    return new SenderId(pid, seq);
                }
                return null;
            }
            return null;
        }
        return null;
    }

    public boolean checkIfEffectHe2_0(VibrationEffect effect) {
        if (effect instanceof RichTapVibrationEffect.PatternHe) {
            RichTapVibrationEffect.PatternHe patternHe = (RichTapVibrationEffect.PatternHe) effect;
            int[] patternData = patternHe.getPatternInfo();
            int versionOrType = patternData[0];
            if (versionOrType == 2) {
                return true;
            }
        }
        return false;
    }

    public boolean checkIfFirstHe2_0Package(VibrationEffect effect) {
        if (effect instanceof RichTapVibrationEffect.PatternHe) {
            RichTapVibrationEffect.PatternHe patternHe = (RichTapVibrationEffect.PatternHe) effect;
            int[] patternData = patternHe.getPatternInfo();
            int firstPatternIndexInPackage = patternData[5];
            Slog.d(TAG, "checkIfFirstHe2_0Package firstPatternIndexInPackage: " + firstPatternIndexInPackage);
            if (firstPatternIndexInPackage == 0) {
                return true;
            }
            return false;
        }
        return false;
    }

    public static class SenderId {
        int pid;
        int seq;

        SenderId(int pid, int seq) {
            this.pid = pid;
            this.seq = seq;
        }

        void setPid(int pid) {
            this.pid = pid;
        }

        int getPid() {
            return this.pid;
        }

        void setSeq(int seq) {
            this.seq = seq;
        }

        int getSeq() {
            return this.seq;
        }

        void reset() {
            this.pid = 0;
            this.seq = 0;
        }
    }
}
