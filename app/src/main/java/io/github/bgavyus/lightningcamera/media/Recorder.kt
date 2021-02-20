package io.github.bgavyus.lightningcamera.media

import android.media.MediaFormat
import android.util.Size
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.storage.Storage
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

@AutoFactory
class Recorder(
    @Provided private val storage: Storage,
    private val encoder: Encoder,
    private val videoSize: Size,
    private val frameRate: Hertz,
    private val orientation: Flow<Degrees>,
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
        combine(encoder.format.filterNotNull(), orientation, ::restartSession)
            .launchIn(scope)
    }

    private fun restartSession(format: MediaFormat, orientation: Degrees) {
        stopSession()
        startSession(format, orientation)
    }

    private fun stopSession() = sessionDeferScope.close()

    private fun startSession(format: MediaFormat, orientation: Degrees) {
        val writer = NormalizedWriter(storage, format, orientation)
            .also { sessionDeferScope.defer(it::close) }

        RecorderSession(
            writer,
            videoSize,
            frameRate,
            encoder.samples,
            recording
        )
            .also { sessionDeferScope.defer(it::close) }
    }
}
