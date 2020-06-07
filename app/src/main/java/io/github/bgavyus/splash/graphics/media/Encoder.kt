package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import io.github.bgavyus.splash.graphics.ImageConsumer

class Encoder(
    size: Size,
    bitRate: Int,
    captureRate: Int,
    frameRate: Int,
    keyFrameInterval: Float,
    listener: EncoderListener
) : DeferScope(), ImageConsumer {
    companion object {
        private val TAG = Encoder::class.simpleName

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
    }

    override val surface: Surface

    init {
        val callback = object : MediaCodec.Callback() {
            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                Log.d(TAG, "onOutputFormatChanged(format = $format)")
                listener.onFormatAvailable(format)
            }

            // TODO: Convert to native
            override fun onOutputBufferAvailable(
                codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo
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

                        listener.onBufferAvailable(buffer, info)
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
                listener.onEncoderError(error)
            }

            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {}
        }

        val handler = SingleThreadHandler(TAG)
            .apply { defer(::close) }

        val format = MediaFormat.createVideoFormat(MIME_TYPE, size.width, size.height).apply {
            val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_CAPTURE_RATE, captureRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, frameRate)
            setFloat(MediaFormat.KEY_I_FRAME_INTERVAL, keyFrameInterval)
        }

        MediaCodec.createEncoderByType(MIME_TYPE).run {
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
