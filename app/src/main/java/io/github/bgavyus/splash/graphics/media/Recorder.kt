package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.Deferrer
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.Snake
import io.github.bgavyus.splash.common.area
import io.github.bgavyus.splash.graphics.ImageConsumer
import io.github.bgavyus.splash.storage.StorageFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer

class Recorder private constructor(
    file: StorageFile,
    size: Size,
    fpsRange: Range<Int>,
    rotation: Rotation
) : Deferrer(), ImageConsumer {
    companion object {
        private val TAG = Recorder::class.simpleName

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MILLIS_IN_UNIT = 1_000
        private const val MICROS_IN_UNIT = 1_000 * MILLIS_IN_UNIT
        private const val KEY_FRAME_INTERVAL_FRAMES = 1
        private const val COMPRESSION_RATIO = 5
        private const val PLAYBACK_FPS = 5
        private const val BUFFER_TIME_MILLISECONDS = 50

        suspend fun init(
            file: StorageFile,
            size: Size,
            fpsRange: Range<Int>,
            rotation: Rotation
        ) = withContext(Dispatchers.IO) { Recorder(file, size, fpsRange, rotation) }
    }

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE)
        .apply { defer(::release) }

    private val snake: Snake<Sample>

    init {
        val samplesSize = fpsRange.upper * BUFFER_TIME_MILLISECONDS / MILLIS_IN_UNIT
        val samples = Array(samplesSize) { Sample(size.area) }

        defer {
            Log.d(TAG, "Freeing samples")
            samples.forEach { it.close() }
        }

        snake = Snake(samples)
    }

    private lateinit var writer: Writer
    private var recording = false

    private val mediaCodecCallback = object : MediaCodec.Callback() {
        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(TAG, "onOutputFormatChanged(format = $format)")

            writer = Writer(file, format, rotation)
                .also { defer(it::close) }
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

        override fun onError(codec: MediaCodec, error: MediaCodec.CodecException) {
            Log.e(TAG, "Media codec error", error)
        }

        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
            Log.d(TAG, "onInputBufferAvailable(index = $index)")
            throw NotImplementedError()
        }
    }

    override val surface: Surface

    init {
        val format = MediaFormat.createVideoFormat(MIME_TYPE, size.width, size.height).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            setInteger(MediaFormat.KEY_BIT_RATE, fpsRange.upper * size.area / COMPRESSION_RATIO)
            setInteger(MediaFormat.KEY_CAPTURE_RATE, fpsRange.upper)
            setInteger(
                MediaFormat.KEY_FRAME_RATE,
                PLAYBACK_FPS
            )

            setFloat(
                MediaFormat.KEY_I_FRAME_INTERVAL,
                KEY_FRAME_INTERVAL_FRAMES.toFloat() / PLAYBACK_FPS
            )
        }

        encoder.run {
            setCallback(mediaCodecCallback, /* handler = */ null)

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
            defer(::loss)
        }
    }

    private fun feed(buffer: ByteBuffer, info: MediaCodec.BufferInfo) = snake.feed { sample ->
        sample.copyFrom(buffer, info)
    }

    fun record() {
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

    fun loss() {
        recording = false
    }
}
