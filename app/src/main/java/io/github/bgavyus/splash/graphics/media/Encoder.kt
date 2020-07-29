package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import io.github.bgavyus.splash.graphics.ImageConsumer

class Encoder(format: MediaFormat) : DeferScope(), ImageConsumer {
    companion object {
        private val TAG = Encoder::class.simpleName
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    var listener: EncoderListener? = null

    override val surface: Surface

    init {
        val callback = object : MediaCodec.Callback() {
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "onOutputFormatChanged(format = $format)")
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
                        Log.d(TAG, "Got codec config")
                        return
                    }

                    if (info.endOfStream) {
                        Log.w(TAG, "Got end of stream")
                        return
                    }

                    if (info.empty) {
                        Log.w(TAG, "Got empty buffer")
                        return
                    }

                    try {
                        val buffer = codec.getOutputBuffer(index)

                        if (buffer == null) {
                            Log.w(TAG, "Got null buffer")
                            return
                        }

                        listener?.onBufferAvailable(buffer, info)
                    } catch (_: IllegalStateException) {
                        Log.d(TAG, "Ignoring buffer after release")
                    }
                } finally {
                    try {
                        codec.releaseOutputBuffer(index, /* render = */ false)
                    } catch (_: IllegalStateException) {
                        Log.d(TAG, "Ignoring buffer after release")
                    }
                }
            }

            override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) {
                Log.e(TAG, "Media codec error", error)
                listener?.onEncoderError(error)
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}
        }

        MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC).run {
            defer(::release)
            setCallback(callback, handler)

            configure(
                format,
                /* surface = */ null,
                /* crypto = */ null,
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
