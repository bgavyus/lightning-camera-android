package io.github.bgavyus.splash.recording

import android.util.Log
import android.util.Range
import android.util.Size
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.storage.VideoFile

class HighSpeedRecorder(
    private val videoFile: VideoFile,
    videoSize: Size,
    fpsRange: Range<Int>,
    rotation: Rotation
) : StatefulMediaRecorder() {
    companion object {
        private val TAG = HighSpeedRecorder::class.simpleName

        private const val VIDEO_ENCODER = VideoEncoder.H264
        private const val VIDEO_OUTPUT_FORMAT = OutputFormat.MPEG_4
        private const val VIDEO_PLAYBACK_FPS = 5
        private const val BIT_RATE_FACTOR = 0.2
    }

    init {
        assert(initial)
        setVideoSource(VideoSource.SURFACE)

        assert(initialized)
        setOutputFormat(VIDEO_OUTPUT_FORMAT)
        setVideoFrameRate(VIDEO_PLAYBACK_FPS)
        setVideoSize(videoSize.width, videoSize.height)

        val encodingBitRate =
            (BIT_RATE_FACTOR * fpsRange.upper * videoSize.width * videoSize.height).toInt()
        Log.d(TAG, "Encoding Bit Rate: $encodingBitRate")
        setVideoEncodingBitRate(encodingBitRate)

        setCaptureRate(fpsRange.upper.toDouble())
        setOrientationHint(rotation.degrees)

        assert(dataSourceConfigured)
        setVideoEncoder(VIDEO_ENCODER)
        setOutputFile(videoFile.descriptor)
        prepare()
    }

    fun record() {
        if (prepared) {
            Log.d(TAG, "Recorder is starting")
            start()
        } else {
            resume()
        }
    }

    override fun start() {
        super.start()
        videoFile.contentValid = true
    }

    fun loss() {
        pause()
    }

    override fun release() {
        if (recording || paused) {
            Log.d(TAG, "Recording on release")

            try {
                stop()
            } catch (error: RuntimeException) {
                Log.d(TAG, "Recorder stop raised RuntimeException: $error")
                videoFile.contentValid = false
            }
        }

        super.release()
    }
}
