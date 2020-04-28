package io.github.bgavyus.splash.recording

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.storage.StorageFile

class HighSpeedRecorder(
    private val file: StorageFile,
    size: Size,
    fpsRange: Range<Int>,
    rotation: Rotation,
    private val listener: RecorderListener
) : Recorder {
    companion object {
        private val TAG = HighSpeedRecorder::class.simpleName

        private const val MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val BIT_RATE_FACTOR = 0.2
        private const val KEY_FRAME_INTERVAL_SECONDS = 1
        private const val PLAYBACK_FPS = 5
        private const val OUTPUT_FORMAT = MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4
        private const val INVALID_TRACK = -1
    }

    private val closeStack = CloseStack()

    private val encoder = MediaCodec.createEncoderByType(MIME_TYPE).apply {
        closeStack.push(::release)
    }

    private val muxer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        MediaMuxer(file.descriptor, OUTPUT_FORMAT)
    } else {
        MediaMuxer(file.path, OUTPUT_FORMAT)
    }.apply {
        setOrientationHint(rotation.degrees)
        closeStack.push(::release)
    }

    private val mediaCodecCallback = object : MediaCodec.Callback() {
        private var track = INVALID_TRACK

        override fun onOutputBufferAvailable(
            codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo
        ) {
            try {
                if (!recording) {
                    return
                }

                if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                    return
                }

                if (info.size == 0) {
                    return
                }

                val buffer = encoder.getOutputBuffer(index)
                    ?: return

                if (track == INVALID_TRACK) {
                    throw RuntimeException("Track is invalid")
                }

                muxer.writeSampleData(track, buffer, info)
                file.valid = true
            } finally {
                encoder.releaseOutputBuffer(index, false)
            }
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.d(TAG, "onOutputFormatChanged(format = $format)")

            if (track != INVALID_TRACK) {
                Log.e(TAG, "Format was already set")
                onError()
                return
            }

            muxer.run {
                track = addTrack(format)
                start()
                closeStack.push(::stop)
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

            val bitRate = (BIT_RATE_FACTOR * fpsRange.upper * size.width * size.height).toInt()
            setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
            setInteger(MediaFormat.KEY_FRAME_RATE, PLAYBACK_FPS)
            setInteger(MediaFormat.KEY_CAPTURE_RATE, fpsRange.upper)
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, KEY_FRAME_INTERVAL_SECONDS)
        }

        encoder.run {
            setCallback(mediaCodecCallback)
            configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            inputSurface = encoder.createInputSurface()
            start()
            closeStack.push(::stop)
        }
    }

    private var recording = false

    override fun record() {
        recording = true
    }

    override fun loss() {
        recording = false
    }

    fun onError() {
        file.valid = false
        listener.onRecorderError()
    }

    override fun close() {
        closeStack.close()
    }
}
