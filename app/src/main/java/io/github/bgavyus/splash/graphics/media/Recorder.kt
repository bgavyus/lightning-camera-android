package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Log
import android.util.Size
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.extensions.area
import io.github.bgavyus.splash.graphics.ImageConsumer
import io.github.bgavyus.splash.storage.StorageFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import java.nio.ByteBuffer

class Recorder(
    videoSize: Size,
    framesPerSecond: Int
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

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer { cancel() } }

    var rotation = Rotation.Natural
    val file = MutableStateFlow(null as StorageFile?)
    private val format = MutableStateFlow(null as MediaFormat?)

    private val encoder: Encoder

    init {
        defer(::closeWriter)
        bind()

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
                framesPerSecond * videoSize.area / COMPRESSION_FACTOR
            )

            setInteger(MediaFormat.KEY_CAPTURE_RATE, framesPerSecond)

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

    private fun bind() {
        combine(file, format) { file, format ->
            closeWriter()

            if (file != null && format != null) {
                writer = Writer(file, format, rotation)
            }
        }
            .launchIn(scope)
    }

    private fun closeWriter() {
        writer?.close()
        writer = null
    }

    private var writer: Writer? = null
    private var recording = false

    private val snake = SamplesSnake(
        sampleSize = videoSize.area,
        samplesCount = framesPerSecond * MIN_BUFFER_TIME_MILLISECONDS / MILLIS_IN_UNIT + KEY_FRAME_INTERVAL_FRAMES - 1
    )

    private var lastPts = 0L

    private var ptsGenerator =
        generateSequence(0L) { it + MICROS_IN_UNIT / PLAYBACK_FPS }.iterator()

    override val surface get() = encoder.surface

    override fun onFormatAvailable(format: MediaFormat) {
        this.format.value = format
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
        Log.i(TAG, "Recording")
        snake.drain(::write)
        recording = true
    }

    private fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        // TODO: Use actual PTS
        info.presentationTimeUs = ptsGenerator.next()
        writer?.write(buffer, info)
    }

    fun loss() {
        Log.i(TAG, "Losing")
        recording = false
    }

    override fun onEncoderError(error: MediaCodec.CodecException) {
        // TODO: Propagate errors
    }
}
