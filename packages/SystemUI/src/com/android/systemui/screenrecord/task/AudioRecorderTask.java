/*
 * Copyright (C) 2020 Paranoid Android
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

package com.android.systemui.screenrecord.task;

import android.media.AudioRecord;
import android.media.MediaCodec;

import com.android.systemui.screenrecord.RecordingService;

import java.nio.ByteBuffer;

public class AudioRecorderTask implements Runnable {

    private final Object mAudioEncoderLock = new Object();

    private MediaCodec mAudioEncoder;
    private AudioRecord mInternalAudio;

    private boolean mAudioEncoding;
    private boolean mAudioRecording;
    private ByteBuffer mInputBuffer;
    private int mReadResult;

    public AudioRecorderTask(RecordingService service) {
        mAudioEncoding = service.mAudioEncoding;
        mAudioEncoder = service.mAudioEncoder;
        mAudioRecording = service.mAudioRecording;
        mInternalAudio = service.mInternalAudio;
    }

    @Override
    public void run(){
        long audioPresentationTimeNs;
        byte[] tempBuffer = new byte[RecordingService.SAMPLES_PER_FRAME];
        while (mAudioRecording) {
            audioPresentationTimeNs = System.nanoTime();
            mReadResult = mInternalAudio.read(tempBuffer, 0, RecordingService.SAMPLES_PER_FRAME);
            if (mReadResult == AudioRecord.ERROR_BAD_VALUE || mReadResult == AudioRecord.ERROR_INVALID_OPERATION) {
                continue;
            }
            // send current frame data to encoder
            try {
                synchronized (mAudioEncoderLock) {
                    if (mAudioEncoding) {
                        int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                            mInputBuffer = mAudioEncoder.getInputBuffer(inputBufferIndex);
                            mInputBuffer.clear();
                            mInputBuffer.put(tempBuffer);
                            mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, tempBuffer.length, audioPresentationTimeNs / 1000, 0);
                        }
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        // finished recording -> send it to the encoder
        audioPresentationTimeNs = System.nanoTime();
        mReadResult = mInternalAudio.read(tempBuffer, 0, RecordingService.SAMPLES_PER_FRAME);
        if (mReadResult == AudioRecord.ERROR_BAD_VALUE
            || mReadResult == AudioRecord.ERROR_INVALID_OPERATION)
        // send current frame data to encoder
        try {
            synchronized (mAudioEncoderLock) {
                if (mAudioEncoding) {
                    int inputBufferIndex = mAudioEncoder.dequeueInputBuffer(-1);
                    if (inputBufferIndex >= 0) {
                        mInputBuffer = mAudioEncoder.getInputBuffer(inputBufferIndex);
                        mInputBuffer.clear();
                        mInputBuffer.put(tempBuffer);
                        mAudioEncoder.queueInputBuffer(inputBufferIndex, 0, tempBuffer.length, audioPresentationTimeNs / 1000, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                    }
                }
            }
        } catch (Throwable t) {
            t.printStackTrace();
        }
        mInternalAudio.stop();
        mInternalAudio.release();
        mInternalAudio = null;
    }
}