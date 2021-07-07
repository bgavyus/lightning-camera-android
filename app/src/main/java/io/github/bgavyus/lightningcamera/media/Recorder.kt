package io.github.bgavyus.lightningcamera.media

import android.media.MediaFormat
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.Rotation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

@AutoFactory
class Recorder(
    @Provided private val writerFactory: SamplesWriterFactory,
    private val encoder: Encoder,
    private val queue: SamplesQueue,
    recording: Flow<Boolean>,
    orientation: Flow<Rotation>,
) : DeferScope() {
    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val recordingState = recording.stateIn(scope, SharingStarted.Eagerly, false)

    private val sessionDeferScope = DeferScope()
        .also { defer(it::close) }

    init {
        combine(encoder.format.filterNotNull(), orientation, ::restartSession)
            .launchIn(scope)
    }

    private fun restartSession(format: MediaFormat, orientation: Rotation) {
        stopSession()
        startSession(format, orientation)
    }

    private fun stopSession() {
        sessionDeferScope.close()
    }

    private fun startSession(format: MediaFormat, orientation: Rotation) {
        val normalizer = PresentationTimeNormalizer()

        val writer = writerFactory.create(format, orientation)
            .also { sessionDeferScope.defer(it::close) }

        val pipeline = SamplesPipeline(listOf(normalizer, writer))

        queue.clear()

        encoder.samplesProcessor = SamplesProcessor { buffer, info ->
            val processor = if (recordingState.value) {
                queue.drain(pipeline)
                pipeline
            } else {
                queue
            }

            processor.process(buffer, info)
        }

        sessionDeferScope.defer {
            encoder.samplesProcessor = null
        }
    }
}
