package io.github.bgavyus.splash.graphics.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.util.Size
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Logger
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.extensions.area
import io.github.bgavyus.splash.graphics.ImageConsumer
import io.github.bgavyus.splash.storage.Storage
import io.github.bgavyus.splash.storage.StorageFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import java.nio.ByteBuffer

class Recorder(
    private val storage: Storage,
    videoSize: Size,
    framesPerSecond: Int
) : DeferScope(), ImageConsumer, EncoderListener {
    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MILLIS_IN_UNIT = 1_000
        private const val MICROS_IN_UNIT = 1_000_000
        private const val KEY_FRAME_INTERVAL_FRAMES = 10
        private const val COMPRESSION_FACTOR = 5
        private const val PLAYBACK_FPS = 5
        private const val MIN_BUFFER_TIME_MILLISECONDS = 50
    }

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private var format: MediaFormat? = null
    val rotation = MutableStateFlow(Rotation.Natural)
    val lastException = MutableStateFlow(null as Exception?)
    private val encoder: Encoder

    init {
        val format = MediaFormat.createVideoFormat(
            MIME_TYPE,
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

    override val surface get() = encoder.surface

    var file: StorageFile? = null

    private var writer: Writer? = null

    override fun onFormatAvailable(format: MediaFormat) {
        this.format = format
        bind()
    }

    private fun bind() {
        rotation
            .onEach { start() }
            .launchIn(scope)
    }

    suspend fun start() {
        stop()
        val file = storage.generateFile()
        val format = format ?: return
        writer = Writer(file, format, rotation.value)
        this.file = file
    }

    fun stop() {
        writer?.close()
        writer = null
        file?.close()
        file = null
    }

    private var recording = false

    private val snake = SamplesSnake(
        sampleSize = videoSize.area,
        samplesCount = framesPerSecond * MIN_BUFFER_TIME_MILLISECONDS / MILLIS_IN_UNIT + KEY_FRAME_INTERVAL_FRAMES - 1
    )

    override fun onBufferAvailable(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (recording) {
            write(buffer, info)
        } else {
            snake.feed(buffer, info)
        }

        logSkippedFrames(info.presentationTimeUs)
    }

    private var lastPts = 0L

    private fun logSkippedFrames(pts: Long) {
        if (lastPts > 0) {
            val framesSkipped = PLAYBACK_FPS * (pts - lastPts) / MICROS_IN_UNIT - 1

            if (framesSkipped > 0) {
                Logger.verbose("Frames Skipped: $framesSkipped")
            }
        }

        lastPts = pts
    }

    fun record() {
        Logger.info("Recording")
        snake.drain(::write)
        recording = true
    }

    private var ptsGenerator =
        generateSequence(0L) { it + MICROS_IN_UNIT / PLAYBACK_FPS }.iterator()

    private fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        // TODO: Use actual PTS
        info.presentationTimeUs = ptsGenerator.next()
        writer?.write(buffer, info)
    }

    fun lose() {
        Logger.info("Losing")
        recording = false
    }

    override fun onEncoderError(error: MediaCodec.CodecException) {
        lastException.value = error
    }
}
