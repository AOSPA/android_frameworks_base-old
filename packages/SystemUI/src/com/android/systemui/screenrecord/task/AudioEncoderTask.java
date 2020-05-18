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

import android.media.MediaCodec;
import android.media.MediaMuxer;

import com.android.systemui.screenrecord.RecordingService;

import java.nio.ByteBuffer;

public class AudioEncoderTask implements Runnable {

    private final Object mAudioEncoderLock = new Object();
    private final Object mMuxerLock = new Object();
    private final Object mWriteAudioLock = new Object();

    private MediaCodec mAudioEncoder;
    private MediaCodec.BufferInfo mBufferInfo;
    private MediaMuxer mMuxer;
    private RecordingService mService;

    private boolean mAudioEncoding;
    private boolean mMuxerStarted = false;
    private int mAudioTrackIndex;

    public AudioEncoderTask(RecordingService service) {
        mService = service;
        mAudioEncoding = service.mAudioEncoding;
        mAudioEncoder = service.mAudioEncoder;
        mMuxer = service.mMuxer;
    }

    @Override
    public void run(){
        mAudioEncoding = true;
        mAudioTrackIndex = -1;
        mBufferInfo = new MediaCodec.BufferInfo();
        while (mAudioEncoding){
            int bufferIndex = mAudioEncoder.dequeueOutputBuffer(mBufferInfo, 10);
            if (bufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
                // no output available yet
            } else if (bufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                // should happen before receiving buffers, and should only happen once
                if (mAudioTrackIndex >= 0) {
                    throw new RuntimeException("format changed twice");
                }
                synchronized (mMuxerLock) {
                    mAudioTrackIndex = mMuxer.addTrack(mAudioEncoder.getOutputFormat());
                    if (!mMuxerStarted && mAudioTrackIndex >= 0) {
                        mMuxer.start();
                        mMuxerStarted = true;
                    }
                }
            } else if (bufferIndex < 0) {
                // let's ignore it
            } else {
                if (mMuxerStarted && mAudioTrackIndex >= 0) {
                    ByteBuffer encodedData = mAudioEncoder.getOutputBuffer(bufferIndex);
                    if (encodedData == null) {
                        throw new RuntimeException("encoderOutputBuffer " + bufferIndex + " was null");
                    }
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                        // The codec config data was pulled out and fed to the muxer when we got
                        // the INFO_OUTPUT_FORMAT_CHANGED status. Ignore it.
                        mBufferInfo.size = 0;
                    }
                    if (mBufferInfo.size != 0) {
                        if (mMuxerStarted) {
                            // adjust the ByteBuffer values to match BufferInfo (not needed?)
                            encodedData.position(mBufferInfo.offset);
                            encodedData.limit(mBufferInfo.offset + mBufferInfo.size);
                            synchronized (mWriteAudioLock) {
                                if (mMuxerStarted) {
                                    mMuxer.writeSampleData(mAudioTrackIndex, encodedData, mBufferInfo);
                                }
                            }
                        }
                    }
                    mAudioEncoder.releaseOutputBuffer(bufferIndex, false);
                    if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        // reached EOS
                        mAudioEncoding = false;
                        break;
                    }
                }
            }
        }

        synchronized (mAudioEncoderLock) {
            mAudioEncoder.stop();
            mAudioEncoder.release();
            mAudioEncoder = null;
        }

        synchronized (mMuxerLock) {
            if (mMuxer != null) {
                if (mMuxerStarted) {
                    mMuxer.stop();
                }
                mMuxer.release();
                mMuxer = null;
                mMuxerStarted = false;
            }
        }
    }
}