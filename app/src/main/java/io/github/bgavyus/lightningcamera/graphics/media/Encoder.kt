package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.view.Surface
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import io.github.bgavyus.lightningcamera.common.extensions.cancel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.*

class Encoder(format: MediaFormat) : DeferScope() {
    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    private val dispatcher = handler.asCoroutineDispatcher(javaClass.simpleName)

    private val scope = CoroutineScope(dispatcher)
        .apply { defer(::cancel) }

    private val _format = MutableSharedFlow<MediaFormat>()
    val format = _format.asSharedFlow()

    private val _samples = MutableSharedFlow<Sample>()
    val samples = _samples.asSharedFlow()

    private val codec: MediaCodec
    val surface: Surface

    init {
        val mimeType = format.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalArgumentException()

        codec = MediaCodec.createEncoderByType(mimeType).apply {
            defer(::release)

            encoderEvents(handler).onEach {
                when (it) {
                    is EncoderEvent.FormatChanged -> onFormatChanged(it.format)
                    is EncoderEvent.BufferAvailable -> onBufferAvailable(it.index, it.info)
                }
            }
                .launchIn(scope)

            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)

            surface = createInputSurface()
                .apply { defer(::release) }

            start()
            defer(::stop)
            defer(::flush)
        }
    }

    private suspend fun onFormatChanged(format: MediaFormat) {
        Logger.debug("Format available")
        _format.emit(format)
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

private sealed class EncoderEvent {
    data class FormatChanged(val format: MediaFormat) : EncoderEvent()
    data class BufferAvailable(val index: Int, val info: MediaCodec.BufferInfo) : EncoderEvent()
}

private fun MediaCodec.encoderEvents(handler: Handler) = callbackFlow<EncoderEvent> {
    val callback = object : MediaCodec.Callback() {
        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) =
            sendBlocking(EncoderEvent.FormatChanged(format))

        override fun onOutputBufferAvailable(
            codec: MediaCodec,
            index: Int,
            info: MediaCodec.BufferInfo,
        ) = sendBlocking(EncoderEvent.BufferAvailable(index, info))

        override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) = cancel(error)
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}
    }

    setCallback(callback, handler)
    awaitClose { setCallback(null) }
}

private operator fun Int.contains(mask: Int) = and(mask) == mask
