package io.github.bgavyus.splash.media

import android.media.MediaCodec
import java.nio.ByteBuffer
import java.util.*

class SamplesSink(private val size: Int, private val maxSampleSize: Int) : AutoCloseable {
    // TODO: Use array
    private val deque = ArrayDeque<Sample>(size)

    fun pour(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (deque.size < size) {
            add(buffer, info)
        } else {
            rotate(buffer, info)
        }
    }

    private fun add(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val mediaBuffer = Sample(buffer, info, maxSampleSize)
        deque.addLast(mediaBuffer)
    }

    private fun rotate(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        val mediaBuffer = deque.removeFirst()
        mediaBuffer.copyFrom(buffer, info)
        deque.addLast(mediaBuffer)
    }

    fun drain(block: (Sample) -> Unit = {}) {
        while (deque.size > 0) {
            deque.removeFirst().use(block)
        }
    }

    override fun close() = drain()
}
