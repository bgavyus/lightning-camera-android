package io.github.bgavyus.splash.flow

import android.view.TextureView
import io.github.bgavyus.splash.capture.Camera
import io.github.bgavyus.splash.capture.CameraConnection
import io.github.bgavyus.splash.capture.CameraSession
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.graphics.ImageConsumerDuplicator
import io.github.bgavyus.splash.graphics.detection.DetectionListener
import io.github.bgavyus.splash.graphics.detection.MotionDetector
import io.github.bgavyus.splash.graphics.media.Recorder
import io.github.bgavyus.splash.graphics.views.StreamView
import io.github.bgavyus.splash.storage.VideoFile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class DetectionRecorder private constructor() : DeferScope() {
    companion object {
        suspend fun init(textureView: TextureView, listener: DetectionListener) =
            DetectionRecorder().apply { init(textureView, listener) }
    }

    private lateinit var recorder: Recorder

    private suspend fun init(textureView: TextureView, listener: DetectionListener) = coroutineScope {
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
            MotionDetector.init(camera.size).also {
                defer(it::close)
                it.listener = listener
            }
        }

        val deferredDuplicator = async {
            val view = StreamView.init(textureView, camera.size)
                .also { defer(it::close) }

            val detector = deferredDetector.await()

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
}
