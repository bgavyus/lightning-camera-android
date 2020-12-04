package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.Snake
import java.nio.ByteBuffer

class SamplesSnake(sampleSize: Int, samplesCount: Int) : DeferScope() {
    private val snake = Snake(Array(samplesCount) { Sample(sampleSize) })

    fun feed(buffer: ByteBuffer, info: MediaCodec.BufferInfo) = snake.feed { sample ->
        sample.copyFrom(buffer, info)
        sample
    }

    fun drain(block: (ByteBuffer, MediaCodec.BufferInfo) -> Unit) {
        var reachedKeyFrame = false
        var skippedFrames = 0

        snake.drain { sample ->
            if (reachedKeyFrame || sample.info.keyFrame) {
                reachedKeyFrame = true
                block(sample.buffer, sample.info)
            } else {
                skippedFrames++
            }
        }

        Logger.debug("Skipped partial frames: $skippedFrames")
    }
}
