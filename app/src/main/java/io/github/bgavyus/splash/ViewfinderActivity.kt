package io.github.bgavyus.splash

import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import io.github.bgavyus.splash.sensors.CameraError
import io.github.bgavyus.splash.sensors.CameraErrorType
import io.github.bgavyus.splash.sensors.CameraListener
import io.github.bgavyus.splash.sensors.HighSpeedCamera
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.detection.DetectionListener
import io.github.bgavyus.splash.detection.Detector
import io.github.bgavyus.splash.detection.LightningDetector
import io.github.bgavyus.splash.detection.MotionDetector
import io.github.bgavyus.splash.flow.FrameDuplicator
import io.github.bgavyus.splash.flow.FrameDuplicatorListener
import io.github.bgavyus.splash.flow.TextureFrameDuplicator
import io.github.bgavyus.splash.permissions.PermissionGroup
import io.github.bgavyus.splash.permissions.PermissionsActivity
import io.github.bgavyus.splash.media.Recorder
import io.github.bgavyus.splash.media.RecorderListener
import io.github.bgavyus.splash.media.RetroRecorder
import io.github.bgavyus.splash.storage.Storage
import io.github.bgavyus.splash.storage.StorageFile
import io.github.bgavyus.splash.storage.VideoFile
import kotlinx.android.synthetic.main.activity_viewfinder.*
import java.io.IOException

// TODO: Remove all logic from this class
// TODO: Handle rotation
// TODO: Dim screen after some time
// TODO: Indicate that capture has occurred (visual + audible)
// TODO: Add start/stop button
class ViewfinderActivity : PermissionsActivity(), Thread.UncaughtExceptionHandler, CameraListener,
    DetectionListener, FrameDuplicatorListener, RecorderListener {

    companion object {
        private val TAG = ViewfinderActivity::class.simpleName
    }

    private val closeStack = CloseStack()

    private lateinit var recorder: Recorder
    private lateinit var file: StorageFile
    private lateinit var frameDuplicator: FrameDuplicator
    private lateinit var camera: HighSpeedCamera
    private lateinit var detector: Detector

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Activity.onCreate(savedInstanceState = $savedInstanceState)")
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        setContentView(R.layout.activity_viewfinder)
        Thread.setDefaultUncaughtExceptionHandler(this)

        logState()
    }

    private fun logState() {
        Log.d(TAG, "Device Orientation: ${App.deviceOrientation}")
        Log.d(TAG, "Display FPS: ${windowManager.defaultDisplay.refreshRate}")
        Log.d(TAG, "Storage: ${if (Storage.scoped) "scoped" else "legacy"}")
    }

    override fun onResume() {
        Log.d(TAG, "Activity.onResume")
        super.onResume()
        initPermissions()
    }

    private fun initPermissions() {
        if (!allPermissionsGranted()) {
            Log.i(TAG, "Requesting permissions")
            requestNonGrantedPermissions()
            return
        }

        onPermissionsAvailable()
    }

    override fun onPermissionDenied(group: PermissionGroup) {
        Log.d(TAG, "onPermissionDenied(group = $group)")

        finishWithMessage(
            when (group) {
                PermissionGroup.Camera -> R.string.error_camera_permission_not_granted
                PermissionGroup.Storage -> R.string.error_storage_permission_not_granted
            }
        )
    }

    override fun onAllPermissionsGranted() {
        Log.d(TAG, "onAllPermissionGranted")
        recreate()
    }

    private fun onPermissionsAvailable() = initCamera()

    private fun initCamera() {
        try {
            HighSpeedCamera(this).run {
                camera = this
                onCameraAvailable()
            }
        } catch (error: CameraError) {
            onCameraError(error.type)
        }
    }

    private fun onCameraAvailable() = initFrameDuplicator()

    private fun initFrameDuplicator() {
        TextureFrameDuplicator(texture_view, camera.videoSize, this)
    }

    override fun onFrameDuplicatorAvailable(frameDuplicator: FrameDuplicator) {
        this.frameDuplicator = frameDuplicator
        initDetector()
    }

    private fun initDetector() {
        // TODO: Forward original resolution image
        detector = MotionDetector(frameDuplicator.outputBitmap, this).apply {
            closeStack.push(::close)
        }

        onDetectorAvailable()
    }

    private fun onDetectorAvailable() = initVideoFile()

    private fun initVideoFile() {
        try {
            file = VideoFile().apply {
                closeStack.push(::close)
            }
        } catch (_: IOException) {
            return finishWithMessage(R.string.error_io)
        }

        onVideoFileAvailable()
    }

    private fun onVideoFileAvailable() = initRecorder()

    private fun initRecorder() {
        val rotation = camera.sensorOrientation + App.deviceOrientation
        recorder = RetroRecorder(file, camera.videoSize, camera.fpsRange, rotation, this)
            .apply {
                closeStack.push(::close)
            }

        onRecorderAvailable()
    }

    private fun onRecorderAvailable() = startCameraStreaming()

    private fun startCameraStreaming() {
        try {
            camera.run {
                startStreaming()
                closeStack.push(::stopStreaming)
            }
        } catch (error: CameraError) {
            onCameraError(error.type)
        }
    }

    override fun onSurfacesNeeded() = listOf(frameDuplicator.inputSurface, recorder.inputSurface)

    override fun onCameraStreamStarted() = initFrameStream()

    private fun initFrameStream() {
        frameDuplicator.run {
            startStreaming()
            closeStack.push(::stopStreaming)
        }
    }

    override fun onFrameAvailable() = detector.detect()

    override fun onDetectionStarted() {
        Log.i(TAG, "Detection Started")
        recorder.record()
    }

    override fun onDetectionEnded() {
        Log.i(TAG, "Detection Ended")
        recorder.loss()
    }

    override fun onCameraError(type: CameraErrorType) = finishWithMessage(
        when (type) {
            CameraErrorType.HighSpeedNotAvailable -> R.string.error_high_speed_camera_not_available
            CameraErrorType.InUse -> R.string.error_camera_in_use
            CameraErrorType.MaxInUse -> R.string.error_max_cameras_in_use
            CameraErrorType.Disabled -> R.string.error_camera_disabled
            CameraErrorType.Device -> R.string.error_camera_device
            CameraErrorType.Disconnected -> R.string.error_camera_disconnected
            CameraErrorType.ConfigureFailed -> R.string.error_camera_generic
            CameraErrorType.Generic -> R.string.error_camera_generic
        }
    )

    override fun onRecorderError() = finishWithMessage(R.string.error_recorder)

    override fun onPause() {
        Log.d(TAG, "Activity.onPause")
        super.onPause()
        close()
    }

    override fun uncaughtException(thread: Thread, error: Throwable) = finishWithMessage(R.string.error_uncaught)

    private fun finishWithMessage(resourceId: Int) {
        Log.d(TAG, "finishWithMessage: ${App.getDefaultString(resourceId)}")
        App.showMessage(resourceId)
        finish()
    }

    override fun finish() {
        Log.d(TAG, "finish")
        super.finish()
        close()
    }

    private fun close() = closeStack.close()
}
