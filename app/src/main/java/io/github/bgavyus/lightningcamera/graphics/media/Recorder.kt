package io.github.bgavyus.lightningcamera.graphics.media

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.util.Size
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.common.Rotation
import io.github.bgavyus.lightningcamera.common.extensions.area
import io.github.bgavyus.lightningcamera.storage.Storage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.nio.ByteBuffer
import kotlin.math.ceil

class Recorder(
    private val storage: Storage,
    videoSize: Size,
    framesPerSecond: Int,
) : DeferScope() {
    companion object {
        private const val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val microsInUnit = 1_000_000
        private const val playbackFps = 5
        private const val minBufferSeconds = 0.05
    }

    private val scope = CoroutineScope(Dispatchers.IO)
        .apply { defer(::cancel) }

    private var format: MediaFormat? = null
    val rotation = MutableStateFlow(Rotation.Natural)
    private val encoder: Encoder

    init {
        val format = MediaFormat.createVideoFormat(
            mimeType,
            videoSize.width,
            videoSize.height
        ).apply {
            setInteger(
                MediaFormat.KEY_COLOR_FORMAT,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
            )

            val codecInfo = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
                .find { it.supportedTypes.contains(mimeType) }
                ?: throw RuntimeException()

            setInteger(
                MediaFormat.KEY_BIT_RATE,
                codecInfo.getCapabilitiesForType(mimeType).videoCapabilities.bitrateRange.upper
                    .also { Logger.info("Bit rate: $it") }
            )

            setInteger(MediaFormat.KEY_OPERATING_RATE, framesPerSecond)
            setInteger(MediaFormat.KEY_FRAME_RATE, playbackFps)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 0)
        }

        encoder = Encoder(format)
            .also { defer(it::close) }

        encoder.format
            .onEach {
                this.format = it
                bind()
            }
            .launchIn(scope)

        encoder.samples
            .onEach { onBufferAvailable(it.buffer, it.info) }
            .launchIn(scope)
    }

    val surface get() = encoder.surface

    private var writer: Writer? = null

    private fun bind() {
        rotation
            .onEach { start() }
            .launchIn(scope)
    }

    suspend fun start() {
        Logger.info("Starting")
        stop()

        val format = format ?: run {
            Logger.debug("Format unavailable")
            return
        }

        withContext(Dispatchers.IO) {
            writer = Writer(storage, format, rotation.value)
        }
    }

    fun stop() {
        writer?.close()
        writer = null
    }

    private var recording = false

    private val snake = SamplesSnake(
        sampleSize = videoSize.area,
        samplesCount = ceil(framesPerSecond * minBufferSeconds).toInt()
    )

    private fun onBufferAvailable(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
    ) = synchronized(this) {
        if (recording) {
            write(buffer, info)
        } else {
            snake.feed(buffer, info)
        }
    }

    fun record() = synchronized(this) {
        Logger.info("Recording")
        snake.drain(::write)
        recording = true
    }

    private val ptsGenerator =
        generateSequence(0L) { it + microsInUnit / playbackFps }.iterator()

    private fun write(buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        info.presentationTimeUs = ptsGenerator.next()
        writer?.write(buffer, info)
    }

    fun lose() = synchronized(this) {
        Logger.info("Losing")
        recording = false
    }
}
