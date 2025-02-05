/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.camera.video.internal.encoder;

import android.media.MediaCodec;

import com.google.common.util.concurrent.ListenableFuture;

import org.jspecify.annotations.NonNull;

import java.nio.ByteBuffer;

/**
 * The encoded data which is generated by the {@link Encoder}.
 *
 * <p>Once {@link EncodedData} is no longer needed, it has to call {@link #close} to return to
 * encoder, otherwise it may cause leakage or failure.
 *
 * @see EncoderCallback#onEncodedData
 */
public interface EncodedData extends AutoCloseable {
    /**
     * Gets the {@link ByteBuffer} of the encoded data.
     *
     * <p>After {@link #close} is called, the byte buffer will be returned to encoder. Therefore,
     * make sure not to use this byte buffer after {@link #close} is called, otherwise it may get
     * uncertain behavior.
     */
    @NonNull ByteBuffer getByteBuffer();

    /**
     * Gets the {@link ByteBuffer}'s additional information.
     *
     * @see MediaCodec.BufferInfo
     */
    MediaCodec.@NonNull BufferInfo getBufferInfo();

    /** Gets the timestamp of the encoded data in microseconds. */
    long getPresentationTimeUs();

    /** Gets the data size in bytes. */
    long size();

    /**  Returns true if this is a video key frame (I-Frame). */
    boolean isKeyFrame();

    /** The encoded data should be explicitly closed in order to release the resources. */
    @Override
    void close();

    /** The {@link ListenableFuture} that is complete when {@link #close} is called. */
    @NonNull ListenableFuture<Void> getClosedFuture();
}
