package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaFormat
import android.util.Size
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Rotation
import io.github.bgavyus.lightningcamera.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class Recorder(
    private val storage: Storage,
    private val encoder: Encoder,
    private val videoSize: Size,
    private val framesPerSecond: Int,
    private val rotation: Flow<Rotation>,
    private val recording: Flow<Boolean>,
) : DeferScope() {
    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val sessionDeferScope = DeferScope()
        .also { defer(it::close) }

    init {
        bind()
    }

    private fun bind() {
        combine(encoder.format, rotation, ::restartSession)
            .launchIn(scope)
    }

    private fun restartSession(format: MediaFormat, rotation: Rotation) {
        stopSession()
        startSession(format, rotation)
    }

    private fun stopSession() = sessionDeferScope.close()

    private fun startSession(format: MediaFormat, rotation: Rotation) {
        val writer = NormalizedWriter(storage, format, rotation)
            .also { sessionDeferScope.defer(it::close) }

        RecorderSession(
            writer,
            videoSize,
            framesPerSecond,
            encoder.samples,
            recording
        )
            .also { sessionDeferScope.defer(it::close) }
    }
}
