package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Range
import android.util.Size
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.area
import io.github.bgavyus.splash.common.middle
import io.github.bgavyus.splash.graphics.ImageConsumer
import io.github.bgavyus.splash.storage.StorageFile
import java.nio.ByteBuffer

class Recorder(
    private val file: StorageFile,
    private val rotation: Rotation,
    videoSize: Size,
    fpsRange: Range<Int>
) : DeferScope(), ImageConsumer, EncoderListener {
    companion object {
        private val TAG = Recorder::class.simpleName

        private const val MILLIS_IN_UNIT = 1_000
        private const val MICROS_IN_UNIT = 1_000_000
        private const val KEY_FRAME_INTERVAL_FRAMES = 10
        private const val COMPRESSION_FACTOR = 5
        private const val PLAYBACK_FPS = 5
        private const val MIN_BUFFER_TIME_MILLISECONDS = 50
    }

    private val encoder: Encoder

    init {
        val format = MediaFormat.createVideoFormat(
            MediaFormat.MIMETYPE_VIDEO_AVC,
            videoSize.width,
            videoSize.height
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            setInteger(
                MediaFormat.KEY_BIT_RATE,
                fpsRange.middle * videoSize.area / COMPRESSION_FACTOR
            )

            setInteger(MediaFormat.KEY_CAPTURE_RATE, fpsRange.middle)

            setInteger(MediaFormat.KEY_FRAME_RATE, PLAYBACK_FPS)

            setFloat(
                MediaFormat.KEY_I_FRAME_INTERVAL,
                KEY_FRAME_INTERVAL_FRAMES.toFloat() / PLAYBACK_FPS
            )
        }

        encoder = Encoder(format).also {
            defer(it::close)
            it.listener = this
        }
    }

    private lateinit var writer: Writer
    private var recording = false

    private val snake = SamplesSnake(
        sampleSize = videoSize.area,
        samplesCount = fpsRange.upper * MIN_BUFFER_TIME_MILLISECONDS / MILLIS_IN_UNIT + KEY_FRAME_INTERVAL_FRAMES - 1
    )

    private var lastPts = 0L

    private var ptsGenerator =
        generateSequence(0L) { it + MICROS_IN_UNIT / PLAYBACK_FPS }.iterator()

    override val surface get() = encoder.surface

    override fun onFormatAvailable(format: MediaFormat) {
        writer = Writer(file, format, rotation)
            .also { defer(it::close) }
    }

    override fun onBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (recording) {
            write(buffer, info)
        } else {
            snake.feed(buffer, info)
        }

        logSkippedFrames(info.presentationTimeUs)
    }

    private fun logSkippedFrames(pts: Long) {
        if (lastPts > 0) {
            val framesSkipped = PLAYBACK_FPS * (pts - lastPts) / MICROS_IN_UNIT - 1

            if (framesSkipped > 0) {
                Log.v(TAG, "Frames Skipped: $framesSkipped")
            }
        }

        lastPts = pts
    }

    fun record() {
        snake.drain(::write)
        recording = true
    }

    private fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        // TODO: Use actual PTS
        info.presentationTimeUs = ptsGenerator.next()
        writer.write(buffer, info)
    }

    fun loss() {
        recording = false
    }

    override fun onEncoderError(error: MediaCodec.CodecException) {
        // TODO: Propagate errors
    }
}
