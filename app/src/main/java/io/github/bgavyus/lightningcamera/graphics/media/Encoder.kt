package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Size
import android.view.Surface
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import io.github.bgavyus.lightningcamera.extensions.android.media.EncoderEvent
import io.github.bgavyus.lightningcamera.extensions.android.media.encoderEvents
import io.github.bgavyus.lightningcamera.extensions.contains
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.*

class Encoder(size: Size, framesPerSecond: Int) : DeferScope() {
    companion object {
        private const val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    private val dispatcher = handler.asCoroutineDispatcher(javaClass.simpleName)

    private val scope = CoroutineScope(dispatcher)
        .apply { defer(::cancel) }

    private val _format = MutableStateFlow(null as MediaFormat?)
    val format = _format.asStateFlow()

    private val _samples = MutableSharedFlow<Sample>()
    val samples = _samples.asSharedFlow()

    private val codec: MediaCodec
    val surface: Surface

    init {
        codec = MediaCodec.createEncoderByType(mimeType).apply {
            defer(::release)

            encoderEvents(handler).onEach {
                when (it) {
                    is EncoderEvent.FormatChanged -> onFormatChanged(it.format)
                    is EncoderEvent.BufferAvailable -> onBufferAvailable(it.index, it.info)
                }
            }
                .launchIn(scope)

            val format = FormatFactory.create(size, framesPerSecond, mimeType)
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            surface = createInputSurface()
                .apply { defer(::release) }

            start()
            defer(::stop)
            defer(::flush)
        }
    }

    private fun onFormatChanged(format: MediaFormat) {
        Logger.debug("Format available")
        _format.value = format
    }

    private suspend fun onBufferAvailable(index: Int, info: MediaCodec.BufferInfo) {
        try {
            if (MediaCodec.BUFFER_FLAG_CODEC_CONFIG in info.flags) {
                Logger.debug("Got codec config")
                return
            }

            if (MediaCodec.BUFFER_FLAG_END_OF_STREAM in info.flags) {
                Logger.warn("Got end of stream")
                return
            }

            if (info.size == 0) {
                Logger.warn("Got empty buffer")
                return
            }

            val buffer = codec.getOutputBuffer(index) ?: run {
                Logger.warn("Got null buffer")
                return
            }

            _samples.emit(Sample(buffer, info))
        } finally {
            codec.releaseOutputBuffer(index, false)
        }
    }
}
