package io.github.bgavyus.splash.flow

import android.view.TextureView
import io.github.bgavyus.splash.capture.Camera
import io.github.bgavyus.splash.capture.CameraConnection
import io.github.bgavyus.splash.capture.CameraSession
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.Deferrer
import io.github.bgavyus.splash.graphics.ImageConsumerDuplicator
import io.github.bgavyus.splash.graphics.detection.DetectionListener
import io.github.bgavyus.splash.graphics.detection.Detector
import io.github.bgavyus.splash.graphics.detection.MotionDetector
import io.github.bgavyus.splash.graphics.media.Recorder
import io.github.bgavyus.splash.graphics.views.StreamView
import io.github.bgavyus.splash.storage.VideoFile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DetectionRecorder private constructor() : Deferrer(), DetectionListener {
    companion object {
        suspend fun init(textureView: TextureView) = DetectionRecorder().apply { init(textureView) }
    }

    private lateinit var detector: Detector
    private lateinit var recorder: Recorder

    private suspend fun init(textureView: TextureView) = coroutineScope {
        val deferredFile = async {
            VideoFile.init()
                .also { defer(it::close) }
        }

        val camera = Camera.init()

        val deferredRecorder = async {
            val file = deferredFile.await()
            val rotation = camera.orientation + App.deviceOrientation

            Recorder.init(file, camera.size, camera.fpsRange, rotation)
                .also { defer(it::close) }
        }

        val deferredDetector = async {
            MotionDetector.init(camera.size)
                .also { defer(it::close) }
        }

        val deferredDuplicator = async {
            val view = StreamView.init(textureView, camera.size)
                .also { defer(it::close) }

            detector = deferredDetector.await()

            ImageConsumerDuplicator.init(listOf(detector, view), camera.size)
                .also { defer(it::close) }
        }

        val connection = CameraConnection.init(camera)
            .also { defer(it::close) }

        recorder = deferredRecorder.await()
        val duplicator = deferredDuplicator.await()

        CameraSession.init(connection, listOf(recorder, duplicator))
            .also { defer(it::close) }
    }

    override fun onDetectionStarted() = recorder.record()
    override fun onDetectionEnded() = recorder.loss()

    fun record() {
        detector.listener = this
    }

    fun loss() {
        detector.listener = null
        recorder.loss()
    }
}
