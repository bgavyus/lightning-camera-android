package io.github.bgavyus.splash.recording

import android.media.MediaRecorder
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.storage.StorageFile

class HighSpeedRecorder(
    private val file: StorageFile,
    size: Size,
    fpsRange: Range<Int>,
    rotation: Rotation,
    private val listener: RecorderListener
) : MediaRecorder(), Recorder {
    companion object {
        private val TAG = HighSpeedRecorder::class.simpleName

        private const val VIDEO_ENCODER = VideoEncoder.H264
        private const val VIDEO_OUTPUT_FORMAT = OutputFormat.MPEG_4
        private const val VIDEO_PLAYBACK_FPS = 5
        private const val BIT_RATE_FACTOR = 0.2
    }

    override val inputSurface: Surface get() = surface

    init {
        setOnErrorListener { _, what, extra ->
            Log.d(TAG, "Error: $what $extra")
            file.valid = false
            listener.onRecorderError()
        }

        setVideoSource(VideoSource.SURFACE)
        setOutputFormat(VIDEO_OUTPUT_FORMAT)
        setVideoFrameRate(VIDEO_PLAYBACK_FPS)
        setVideoSize(size.width, size.height)
        setVideoEncodingBitRate((BIT_RATE_FACTOR * fpsRange.upper * size.width * size.height).toInt())
        setCaptureRate(fpsRange.upper.toDouble())
        setOrientationHint(rotation.degrees)
        setVideoEncoder(VIDEO_ENCODER)
        setOutputFile(file.descriptor)
        prepare()
        start()
        pause()
    }

    override fun record() {
        resume()
        file.valid = true
    }

    override fun loss() {
        pause()
    }

    override fun close() {
        try {
            Log.d(TAG, "Stopping")
            stop()
        } catch (error: RuntimeException) {
            Log.d(TAG, "Recorder stop raised RuntimeException: $error")
            file.valid = false
        }

        release()
    }
}
