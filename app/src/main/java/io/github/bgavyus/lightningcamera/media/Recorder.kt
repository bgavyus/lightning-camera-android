package io.github.bgavyus.lightningcamera.media

import android.media.MediaFormat
import android.util.Size
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.Degrees
import io.github.bgavyus.lightningcamera.utilities.Hertz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.launchIn
import kotlin.math.ceil

@AutoFactory
class Recorder(
    @Provided private val writerFactory: SamplesWriterFactory,
    private val encoder: Encoder,
    private val recording: Flow<Boolean>,
    videoSize: Size,
    frameRate: Hertz,
    orientation: Flow<Degrees>,
) : DeferScope() {
    companion object {
        private const val minBufferSeconds = 0.05
    }

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val sessionDeferScope = DeferScope()
        .also { defer(it::close) }

    private val snake = SamplesSnake(
        sampleSize = videoSize.area,
        samplesCount = ceil(frameRate.value * minBufferSeconds).toInt()
    )

    init {
        combine(encoder.format.filterNotNull(), orientation, ::restartSession)
            .launchIn(scope)
    }

    private fun restartSession(format: MediaFormat, orientation: Degrees) {
        stopSession()
        startSession(format, orientation)
    }

    private fun stopSession() = sessionDeferScope.close()

    private fun startSession(format: MediaFormat, orientation: Degrees) {
        val scope = CoroutineScope(Dispatchers.IO)
            .apply { sessionDeferScope.defer(::cancel) }

        val normalizer = PresentationTimeNormalizer()

        val writer = writerFactory.create(format, orientation)
            .also { sessionDeferScope.defer(it::close) }

        val pipeline = SamplesPipeline(listOf(normalizer, writer))

        sessionDeferScope.defer { snake.drain {} }

        combine(encoder.samples, recording) { sample, recording ->
            if (recording) {
                snake.drain(pipeline::process)
                pipeline.process(sample)
            } else {
                snake.feed(sample)
            }
        }
            .launchIn(scope)
    }
}
