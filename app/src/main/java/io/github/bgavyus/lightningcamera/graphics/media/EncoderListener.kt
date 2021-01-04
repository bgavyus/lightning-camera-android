package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface EncoderListener {
    fun onBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo)
    fun onFormatAvailable(format: MediaFormat)
    fun onEncoderError(error: MediaCodec.CodecException)
}
