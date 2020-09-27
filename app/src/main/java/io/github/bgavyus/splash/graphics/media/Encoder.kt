package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.view.Surface
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Logger
import io.github.bgavyus.splash.common.SingleThreadHandler

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
                Logger.debug("onOutputFormatChanged(format = $format)")
                listener?.onFormatAvailable(format)
            }

            // TODO: Convert to native
            override fun onOutputBufferAvailable(
                codec: MediaCodec,
                index: Int,
                info: MediaCodec.BufferInfo
            ) {
                try {
                    if (info.codecConfig) {
                        Logger.debug("Got codec config")
                        return
                    }

                    if (info.endOfStream) {
                        Logger.warn("Got end of stream")
                        return
                    }

                    if (info.empty) {
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
