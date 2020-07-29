package io.github.bgavyus.splash.activities

import android.content.Context
import android.media.MediaFormat
import android.renderscript.RenderScript
import android.view.TextureView
import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.capture.CameraConnection
import io.github.bgavyus.splash.capture.CameraMetadata
import io.github.bgavyus.splash.capture.CameraSession
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Device
import io.github.bgavyus.splash.graphics.ImageConsumerDuplicator
import io.github.bgavyus.splash.graphics.StreamView
import io.github.bgavyus.splash.graphics.detection.Detector
import io.github.bgavyus.splash.graphics.detection.MotionDetector
import io.github.bgavyus.splash.graphics.media.Recorder
import io.github.bgavyus.splash.storage.StandardDirectory
import io.github.bgavyus.splash.storage.Storage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.*

class ViewfinderViewModel(
    private val context: Context,
    private val storage: Storage,
    private val device: Device,
    private val textureView: TextureView,
    private val renderScript: RenderScript
) : DeferScope() {
    private lateinit var recorder: Recorder
    private lateinit var detector: Detector

    suspend fun create() = coroutineScope {
        val currentTimeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())

        val deferredFile = async(Dispatchers.IO) {
            storage.file(
                mimeType = MediaFormat.MIMETYPE_VIDEO_AVC,
                standardDirectory = StandardDirectory.Movies,
                appDirectoryName = context.getString(R.string.video_folder_name),
                name = "VID_$currentTimeStamp.mp4"
            )
                .also { defer(it::close) }
        }

        val cameraMetadata = CameraMetadata(context)
            .apply { collect() }

        val deferredRecorder = async(Dispatchers.IO) {
            val file = deferredFile.await()
            val rotation = cameraMetadata.orientation + device.orientation

            Recorder(file, rotation, cameraMetadata.videoSize, cameraMetadata.fpsRange)
                .also { defer(it::close) }
        }

        val deferredDetector = async(Dispatchers.IO) {
            MotionDetector(renderScript, cameraMetadata.videoSize)
                .also { defer(it::close) }
        }

        val deferredDuplicator = async {
            val view = StreamView(device, textureView, cameraMetadata.videoSize)
                .apply { start() }
                .also { defer(it::close) }

            detector = deferredDetector.await()

            ImageConsumerDuplicator(listOf(detector, view), cameraMetadata.videoSize)
                .apply { start() }
                .also { defer(it::close) }
        }

        val connection = CameraConnection(context, cameraMetadata)
            .apply { open() }
            .also { defer(it::close) }

        recorder = deferredRecorder.await()
        val duplicator = deferredDuplicator.await()

        CameraSession(cameraMetadata, connection, listOf(recorder, duplicator))
            .apply { open() }
            .also { defer(it::close) }
    }

    fun record() = recorder.record()
    fun loss() = recorder.loss()
    fun detectingStates() = detector.detectingStates()
}
