package io.github.bgavyus.lightningcamera.graphics.media

import io.github.bgavyus.lightningcamera.common.Snake

class SamplesSnake(sampleSize: Int, samplesCount: Int) {
    private val snake = Snake(Array(samplesCount) { Sample(sampleSize) })

    fun feed(newSample: Sample) = snake.feed { sample ->
        sample.copyFrom(newSample)
        sample
    }

    fun drain(block: (Sample) -> Unit) = snake.drain(block)
}
