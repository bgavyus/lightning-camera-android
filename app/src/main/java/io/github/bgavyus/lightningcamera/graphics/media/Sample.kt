package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import java.nio.ByteBuffer

data class Sample(val buffer: ByteBuffer, val info: MediaCodec.BufferInfo) {
    constructor(maxSize: Int) : this(ByteBuffer.allocateDirect(maxSize), MediaCodec.BufferInfo())

    fun copyFrom(otherBuffer: ByteBuffer, otherInfo: MediaCodec.BufferInfo) {
        buffer.copyFrom(otherBuffer)
        info.copyFrom(otherInfo)
    }
}

fun ByteBuffer.copyFrom(other: ByteBuffer) {
    position(other.position())
    limit(other.limit())
    put(other)
}

fun MediaCodec.BufferInfo.copyFrom(other: MediaCodec.BufferInfo) =
    set(other.offset, other.size, other.presentationTimeUs, other.flags)
