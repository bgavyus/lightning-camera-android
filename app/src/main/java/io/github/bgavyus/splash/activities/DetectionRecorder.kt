package io.github.bgavyus.splash.activities

import android.view.TextureView
import io.github.bgavyus.splash.capture.Camera
import io.github.bgavyus.splash.capture.CameraConnection
import io.github.bgavyus.splash.capture.CameraSession
import io.github.bgavyus.splash.common.Application
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.graphics.ImageConsumerDuplicator
import io.github.bgavyus.splash.graphics.detection.Detector
import io.github.bgavyus.splash.graphics.detection.MotionDetector
import io.github.bgavyus.splash.graphics.media.Recorder
import io.github.bgavyus.splash.graphics.views.StreamView
import io.github.bgavyus.splash.storage.VideoFile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

// TODO: Replace with view model
class DetectionRecorder private constructor() : DeferScope() {
    companion object {
        suspend fun init(textureView: TextureView) =
            DetectionRecorder().apply { init(textureView) }
    }

    private lateinit var recorder: Recorder
    private lateinit var detector: Detector

    private suspend fun init(textureView: TextureView) = coroutineScope {
        val deferredFile = async {
            VideoFile.init()
                .also { defer(it::close) }
        }

        val camera = Camera.init()

        val deferredRecorder = async {
            val file = deferredFile.await()
            val rotation = camera.orientation + Application.deviceOrientation

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

    fun record() = recorder.record()
    fun loss() = recorder.loss()
    val detectingStates get() = detector.detectingStates
}
