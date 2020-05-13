package io.github.bgavyus.splash.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.storage.StorageFile
import java.nio.ByteBuffer


class RetroRecorder(
    file: StorageFile,
    size: Size,
    fpsRange: Range<Int>,
    rotation: Rotation,
    private val listener: RecorderListener
) : Recorder {
    companion object {
        private val TAG = RetroRecorder::class.simpleName

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MILLIS_IN_UNIT = 1_000
        private const val MICROS_IN_UNIT = 1_000 * MILLIS_IN_UNIT
        private const val KEY_FRAME_INTERVAL_FRAMES = 1
        private const val COMPRESSION_RATIO = 5
        private const val PLAYBACK_FPS = 5
        private const val BUFFER_TIME_MILLI_SECONDS = 50
    }

    private val closeStack = CloseStack()

    private val thread = HandlerThread(TAG).apply {
        start()

        closeStack.push {
            quitSafely()
        }
    }

    private val handler = Handler(thread.looper)

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE).apply {
        closeStack.push(::release)
    }

    private val sink = SamplesSink(
        size = fpsRange.upper * BUFFER_TIME_MILLI_SECONDS / MILLIS_IN_UNIT,
        maxSampleSize = size.area
    ).apply {
        closeStack.push(::close)
    }

    private lateinit var writer: Writer
    private var recording = false

    private val mediaCodecCallback = object : MediaCodec.Callback() {
        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(TAG, "onOutputFormatChanged(format = $format)")

            writer = Writer(file, format, rotation).apply {
                closeStack.push(::close)
            }
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo
        ) {
            try {
                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    return
                }

                if (info.size == 0) {
                    return
                }

                val buffer = encoder.getOutputBuffer(index)
                    ?: return

                if (recording) {
                    write(buffer, info)
                } else {
                    sink.pour(buffer, info)
                }
            } finally {
                encoder.releaseOutputBuffer(index, /* render = */ false)
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            Log.d(TAG, "onError(e = $e)")
            onError()
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            Log.d(TAG, "onInputBufferAvailable(index = $index)")
            throw NotImplementedError()
        }
    }

    override val inputSurface: Surface

    init {
        val format = MediaFormat.createVideoFormat(MIME_TYPE, size.width, size.height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            setInteger(MediaFormat.KEY_BIT_RATE, fpsRange.upper * size.area / COMPRESSION_RATIO)
            setInteger(MediaFormat.KEY_CAPTURE_RATE, fpsRange.upper)
            setInteger(MediaFormat.KEY_FRAME_RATE, PLAYBACK_FPS)

            setFloat(
                MediaFormat.KEY_I_FRAME_INTERVAL,
                KEY_FRAME_INTERVAL_FRAMES.toFloat() / PLAYBACK_FPS
            )
        }

        encoder.run {
            setCallback(mediaCodecCallback, handler)

            configure(
                format,
                /* surface = */ null,
                /* crypto = */ null,
                MediaCodec.CONFIGURE_FLAG_ENCODE
            )

            inputSurface = createInputSurface().apply {
                closeStack.push(::release)
            }

            start()
            closeStack.push(::stop)
        }
    }

    override fun record() {
        drain()
        recording = true
    }

    private fun drain() = sink.drain { sample ->
        write(sample.buffer, sample.info)
    }

    private var ptsGenerator =
        generateSequence(0L) { it + MICROS_IN_UNIT / PLAYBACK_FPS }.iterator()

    private fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        info.presentationTimeUs = ptsGenerator.next()
        writer.write(buffer, info)
    }

    override fun loss() {
        recording = false
    }

    fun onError() = listener.onRecorderError()
    override fun close() = closeStack.close()
}

val Size.area get() = width * height
