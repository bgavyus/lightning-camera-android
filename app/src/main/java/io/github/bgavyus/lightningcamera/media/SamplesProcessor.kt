package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import java.nio.ByteBuffer

interface SamplesProcessor {
    fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo)
}
