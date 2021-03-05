package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.util.Size
import io.github.bgavyus.lightningcamera.extensions.android.media.copyFrom
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.extensions.java.nio.copyFrom
import io.github.bgavyus.lightningcamera.utilities.Hertz
import io.github.bgavyus.lightningcamera.utilities.Snake
import java.nio.ByteBuffer
import kotlin.math.ceil

class SamplesSnake(frameSize: Size, frameRate: Hertz) : SamplesProcessor {
    companion object {
        private const val minBufferSeconds = 0.05
    }

    private val snake = Snake(Array(ceil(frameRate.value * minBufferSeconds).toInt()) {
        Sample(ByteBuffer.allocateDirect(frameSize.area), MediaCodec.BufferInfo())
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
