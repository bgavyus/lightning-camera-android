package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler

class Encoder(format: MediaFormat) : DeferScope() {
    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    var listener: EncoderListener? = null

    val surface: Surface

    init {
        val mimeType = format.getString(MediaFormat.KEY_MIME)
            ?: throw IllegalArgumentException()

        val callback = object : MediaCodec.Callback() {
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Logger.debug("Output format changed: $format)")
                listener?.onFormatAvailable(format)
            }

            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                try {
                    if (info.isCodecConfig) {
                        Logger.debug("Got codec config")
                        return
                    }

                    if (info.isEndOfStream) {
                        Logger.warn("Got end of stream")
                        return
                    }

                    if (info.size == 0) {
                        Logger.warn("Got empty buffer")
                        return
                    }

                    try {
                        val buffer = codec.getOutputBuffer(index)

                        if (buffer == null) {
                            Logger.warn("Got null buffer")
                            return
                        }

                        listener?.onBufferAvailable(buffer, info)
                    } catch (_: IllegalStateException) {
                        Logger.debug("Ignoring buffer after release")
                    }
                } finally {
                    try {
                        codec.releaseOutputBuffer(index, false)
                    } catch (_: IllegalStateException) {
                        Logger.debug("Ignoring buffer after release")
                    }
                }
            }

            override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) {
                Logger.error("Media codec error", error)
                listener?.onEncoderError(error)
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}
        }

        MediaCodec.createEncoderByType(mimeType).run {
            defer(::release)
            setCallback(callback, handler)

            configure(
                format,
                null,
                null,
                MediaCodec.CONFIGURE_FLAG_ENCODE
            )

            surface = createInputSurface()
                .apply { defer(::release) }

            start()
            defer(::stop)
            defer(::flush)
        }
    }
}

val MediaCodec.BufferInfo.isCodecConfig get() = flags.isSet(MediaCodec.BUFFER_FLAG_CODEC_CONFIG)
val MediaCodec.BufferInfo.isEndOfStream get() = flags.isSet(MediaCodec.BUFFER_FLAG_END_OF_STREAM)

private fun Int.isSet(flag: Int) = and(flag) != 0
