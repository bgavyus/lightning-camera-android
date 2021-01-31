package io.github.bgavyus.lightningcamera.graphics.media

import android.util.Size
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Hertz
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlin.math.ceil

class RecorderSession(
    private val writer: Writer,
    videoSize: Size,
    frameRate: Hertz,
    samples: Flow<Sample>,
    recordingFlow: Flow<Boolean>,
) : DeferScope() {
    companion object {
        private const val minBufferSeconds = 0.05
    }

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val snake = SamplesSnake(
        sampleSize = videoSize.area,
        samplesCount = ceil(frameRate.value * minBufferSeconds).toInt()
    )

    init {
        combine(samples, recordingFlow) { sample, recording ->
            if (recording) {
                snake.drain(::write)
                write(sample)
            } else {
                snake.feed(sample)
            }
        }
            .launchIn(scope)
    }

    private fun write(sample: Sample) = writer.write(sample)
}
