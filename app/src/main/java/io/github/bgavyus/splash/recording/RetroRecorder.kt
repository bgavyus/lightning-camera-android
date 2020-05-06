package io.github.bgavyus.splash.recording

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

class RetroRecorder(
    private val file: StorageFile,
    size: Size,
    fpsRange: Range<Int>,
    rotation: Rotation,
    private val listener: RecorderListener
) : Recorder {
    companion object {
        private val TAG = RetroRecorder::class.simpleName

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val MICROS_IN_UNIT = 1_000_000
        private const val KEY_FRAME_INTERVAL_FRAMES = 1
        private const val BIT_RATE_FACTOR = 0.2
        private const val PLAYBACK_FPS = 5
    }

    private val closeStack = CloseStack()

    private val thread = HandlerThread(TAG).apply {
        start()
        closeStack.push { quitSafely() }
    }

    private val handler = Handler(thread.looper)

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE).apply {
        closeStack.push(::release)
    }

    private lateinit var writer: RetroMediaWriter

    private val mediaCodecCallback = object : MediaCodec.Callback() {
        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(TAG, "onOutputFormatChanged(format = $format)")

            if (::writer.isInitialized) {
                Log.e(TAG, "Format was already set")
                onError()
                return
            }

            writer = RetroMediaWriter(file, format, rotation).apply {
                closeStack.push(::close)
            }
        }

        override fun onOutputBufferAvailable(
            codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo
        ) {
            try {
                trackSkippedFrames(info.presentationTimeUs)

                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    return
                }

                if (info.size == 0) {
                    return
                }

                val buffer = encoder.getOutputBuffer(index)
                    ?: return

                writer.write(buffer, info)
                file.valid = true
            } finally {
                encoder.releaseOutputBuffer(index, false)
            }
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

            setInteger(
                MediaFormat.KEY_BIT_RATE,
                (BIT_RATE_FACTOR * fpsRange.upper * size.width * size.height).toInt()
            )

            setInteger(MediaFormat.KEY_CAPTURE_RATE, fpsRange.upper)
            setInteger(MediaFormat.KEY_FRAME_RATE, PLAYBACK_FPS)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_FRAME_INTERVAL_FRAMES / PLAYBACK_FPS)
        }

        encoder.run {
            setCallback(mediaCodecCallback, handler)
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            start()
            closeStack.push(::stop)
        }
    }

    override fun record() {
        writer.stream()
    }

    override fun loss() {
        writer.hold()
    }

    fun onError() {
        file.valid = false
        listener.onRecorderError()
    }

    override fun close() {
        closeStack.close()
    }
}
