package io.github.bgavyus.splash.media

import android.media.MediaCodec
import java.nio.ByteBuffer

class Sample(buffer: ByteBuffer, info: MediaCodec.BufferInfo, maxSize: Int) : AutoCloseable {
    val buffer: ByteBuffer = ByteBuffer.allocateDirect(maxSize)
    val info = MediaCodec.BufferInfo()

    init {
        copyFrom(buffer, info)
    }

    fun copyFrom(otherBuffer: ByteBuffer, otherInfo: MediaCodec.BufferInfo) {
        buffer.copyFrom(otherBuffer)
        info.copyFrom(otherInfo)
    }

    override fun close() {
        // TODO: Release buffer
    }
}

fun ByteBuffer.copyFrom(other: ByteBuffer) {
    position(other.position())
    limit(other.limit())
    put(other)
}

fun MediaCodec.BufferInfo.copyFrom(other: MediaCodec.BufferInfo) {
    set(other.offset, other.size, other.presentationTimeUs, other.flags)
}
