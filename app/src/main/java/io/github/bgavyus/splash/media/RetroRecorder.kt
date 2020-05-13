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
import io.github.bgavyus.splash.common.Snake
import io.github.bgavyus.splash.common.area
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
        private const val BUFFER_TIME_MILLISECONDS = 50
    }

    private val closeStack = CloseStack()

    private val thread = HandlerThread(TAG).apply {
        start()

        closeStack.push {
            Log.d(TAG, "Quiting recorder thread")
            quitSafely()
        }
    }

    private val handler = Handler(thread.looper)

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE).apply {
        closeStack.push(::release)
    }

    private val snake: Snake<Sample>

    init {
        val samplesSize = fpsRange.upper * BUFFER_TIME_MILLISECONDS / MILLIS_IN_UNIT
        val samples = Array(samplesSize) { Sample(size.area) }

        closeStack.push {
            Log.d(TAG, "Freeing samples")

            samples.forEach { sample ->
                sample.close()
            }
        }

        snake = Snake(samples)
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
                    Log.d(TAG, "Got codec config")
                    return
                }

                if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                    Log.d(TAG, "Got end of stream")
                    return
                }

                if (info.size == 0) {
                    Log.w(TAG, "Got empty buffer")
                    return
                }

                val buffer = encoder.getOutputBuffer(index)

                if (buffer == null) {
                    Log.w(TAG, "Got null buffer")
                    return
                }

                if (recording) {
                    write(buffer, info)
                } else {
                    feed(buffer, info)
                }
            } finally {
                encoder.releaseOutputBuffer(index, /* render = */ false)
            }

            trackSkippedFrames(info.presentationTimeUs)
        }

        var lastPts = 0L

        fun trackSkippedFrames(pts: Long) {
            if (lastPts > 0) {
                val framesSkipped = PLAYBACK_FPS * (pts - lastPts) / MICROS_IN_UNIT - 1

                if (framesSkipped > 0) {
                    Log.w(TAG, "Frames Skipped: $framesSkipped")
                }
            }

            lastPts = pts
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
            closeStack.push(::flush)
            closeStack.push(::loss)
        }
    }

    private fun feed(buffer: ByteBuffer, info: MediaCodec.BufferInfo) = snake.feed { sample ->
        sample.copyFrom(buffer, info)
    }

    override fun record() {
        drain()
        recording = true
    }

    private fun drain() = snake.drain { sample ->
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
