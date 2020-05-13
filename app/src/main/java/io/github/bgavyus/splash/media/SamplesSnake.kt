package io.github.bgavyus.splash.media

import android.media.MediaCodec
import java.nio.ByteBuffer

class SamplesSnake(maxSize: Int, maxSampleSize: Int) : AutoCloseable {
    private val nodes = Array(maxSize) { Sample(maxSampleSize) }
    private var head = 0
    private var size = 0

    fun feed(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        nodes[head].copyFrom(buffer, info)
        advance()

        if (!full) {
            grow()
        }
    }

    private fun advance() {
        head = headBased(1)
    }

    private val full get() = size == nodes.size

    private fun grow() {
        size++
    }

    fun drain(block: (Sample) -> Unit) {
        while (!empty) {
            block(nodes[tail])
            shrink()
        }
    }

    private val empty get() = size == 0
    private val tail get() = headBased(-size)
    private fun headBased(n: Int) = Math.floorMod(head + n, nodes.size)

    private fun shrink() {
        size--
    }

    override fun close() = nodes.forEach { node ->
        node.close()
    }
}
