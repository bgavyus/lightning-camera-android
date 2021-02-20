package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.util.Size
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.media.EncoderEvent
import io.github.bgavyus.lightningcamera.extensions.android.media.configureEncoder
import io.github.bgavyus.lightningcamera.extensions.android.media.encoderEvents
import io.github.bgavyus.lightningcamera.extensions.android.media.flagsSet
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.Hertz
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

@AutoFactory
class Encoder(
    @Provided handler: Handler,
    size: Size,
    frameRate: Hertz,
) : DeferScope() {
    companion object {
        private const val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val _format = MutableStateFlow(null as MediaFormat?)
    val format = _format.asStateFlow()

    private val _samples = MutableSharedFlow<Sample>()
    val samples = _samples.asSharedFlow()

    private val codec = MediaCodec.createEncoderByType(mimeType).apply {
        defer(::release)

        encoderEvents(handler).onEach {
            when (it) {
                is EncoderEvent.FormatChanged -> onFormatChanged(it.format)
                is EncoderEvent.BufferAvailable -> onBufferAvailable(it.index, it.info)
            }
        }
            .launchIn(scope)

        val format = FormatFactory.create(size, frameRate, mimeType)
        configureEncoder(format)
    }

    val surface = codec.createInputSurface()
        .apply { defer(::release) }

    init {
        codec.apply {
            start()
            defer(::stop)
            defer(::flush)
        }
    }

    private fun onFormatChanged(format: MediaFormat) {
        Logger.log("Format available")
        _format.value = format
    }

    private suspend fun onBufferAvailable(index: Int, info: MediaCodec.BufferInfo) {
        try {
            if (MediaCodec.BUFFER_FLAG_CODEC_CONFIG in info.flagsSet) {
                Logger.log("Got codec config")
                return
            }

            if (MediaCodec.BUFFER_FLAG_END_OF_STREAM in info.flagsSet) {
                throw RuntimeException()
            }

            val buffer = codec.getOutputBuffer(index)
                ?: throw RuntimeException()

            _samples.emit(Sample(buffer, info))
        } finally {
            codec.releaseOutputBuffer(index, false)
        }
    }
}
