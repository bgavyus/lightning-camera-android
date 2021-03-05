package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import io.github.bgavyus.lightningcamera.extensions.android.media.copyFrom
import io.github.bgavyus.lightningcamera.extensions.java.nio.copyFrom
import io.github.bgavyus.lightningcamera.utilities.Snake
import java.nio.ByteBuffer

class SamplesSnake(sampleSize: Int, samplesCount: Int) : SamplesProcessor {
    private val snake = Snake(Array(samplesCount) {
        Sample(ByteBuffer.allocateDirect(sampleSize), MediaCodec.BufferInfo())
    })

    override fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo) = snake.feed { sample ->
        sample.also {
            it.buffer.copyFrom(buffer)
            it.info.copyFrom(info)
        }
    }

    fun drain(processor: SamplesProcessor) = snake.drain { sample ->
        processor.process(sample.buffer, sample.info)
    }

    fun recycle() = snake.recycle()
}
