package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Size
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.Degrees
import io.github.bgavyus.lightningcamera.utilities.Hertz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*
import java.nio.ByteBuffer

@AutoFactory
class Recorder(
    @Provided private val writerFactory: SamplesWriterFactory,
    private val encoder: Encoder,
    recording: Flow<Boolean>,
    videoSize: Size,
    frameRate: Hertz,
    orientation: Flow<Degrees>,
) : DeferScope() {
    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val recordingState = recording.stateIn(scope, SharingStarted.Eagerly, false)

    private val sessionDeferScope = DeferScope()
        .also { defer(it::close) }

    private val snake = SamplesSnake(videoSize, frameRate)

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
        val normalizer = PresentationTimeNormalizer()

        val writer = writerFactory.create(format, orientation)
            .also { sessionDeferScope.defer(it::close) }

        val pipeline = SamplesPipeline(listOf(normalizer, writer))

        sessionDeferScope.defer(snake::recycle)

        encoder.samplesProcessor = object : SamplesProcessor {
            override fun process(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
                if (recordingState.value) {
                    snake.drain(pipeline)
                    pipeline.process(buffer, info)
                } else {
                    snake.process(buffer, info)
                }
            }
        }

        sessionDeferScope.defer {
            encoder.samplesProcessor = null
        }
    }
}
