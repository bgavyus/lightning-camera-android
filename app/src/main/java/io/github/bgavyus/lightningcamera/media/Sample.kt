package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import io.github.bgavyus.lightningcamera.extensions.android.media.copyFrom
import io.github.bgavyus.lightningcamera.extensions.java.nio.copyFrom
import java.nio.ByteBuffer

data class Sample(val buffer: ByteBuffer, val info: MediaCodec.BufferInfo) {
    constructor(maxSize: Int) : this(ByteBuffer.allocateDirect(maxSize), MediaCodec.BufferInfo())

    fun copyFrom(other: Sample) {
        buffer.copyFrom(other.buffer)
        info.copyFrom(other.info)
    }
}