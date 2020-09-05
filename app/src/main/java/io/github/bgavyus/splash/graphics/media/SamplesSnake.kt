package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Logger
import io.github.bgavyus.splash.common.Snake
import java.nio.ByteBuffer

class SamplesSnake(sampleSize: Int, samplesCount: Int) : DeferScope() {
    private val snake = Snake(Array(samplesCount) { Sample(sampleSize) }.apply {
        defer {
            Logger.debug("Freeing samples")
            forEach { it.close() }
        }
    })

    fun feed(buffer: ByteBuffer, info: MediaCodec.BufferInfo) = snake.feed { sample ->
        sample.copyFrom(buffer, info)
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
