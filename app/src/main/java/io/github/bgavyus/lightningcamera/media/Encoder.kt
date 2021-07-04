package io.github.bgavyus.lightningcamera.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.os.Handler
import android.util.Size
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.media.*
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@AutoFactory
class Encoder(
    @Provided handler: Handler,
    size: Size,
    frameRate: FrameRate,
) : DeferScope() {
    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private val _format = MutableStateFlow(null as MediaFormat?)
    val format = _format.asStateFlow()

    var samplesProcessor: SamplesProcessor? = null

    private val codec = MediaCodec.createEncoderByType(mimeType).apply {
        defer(::release)

        encoderEvents(handler)
            .onEach(::handleEvent)
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
            defer(::tryFlush)
        }
    }

    private fun handleEvent(event: EncoderEvent) = when (event) {
        is EncoderEvent.FormatChanged -> onFormatChanged(event.format)
        is EncoderEvent.BufferAvailable -> onBufferAvailable(event.index, event.info)
    }

    private fun onFormatChanged(format: MediaFormat) {
        Logger.log("Format available")
        _format.value = format
    }

    private fun onBufferAvailable(index: Int, info: MediaCodec.BufferInfo) {
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

            samplesProcessor?.process(buffer, info)
        } finally {
            codec.releaseOutputBuffer(index, false)
        }
    }

    companion object {
        private const val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    }
}
