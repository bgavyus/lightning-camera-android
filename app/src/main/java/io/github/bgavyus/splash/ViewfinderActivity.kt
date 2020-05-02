package io.github.bgavyus.splash

import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import io.github.bgavyus.splash.camera.CameraError
import io.github.bgavyus.splash.camera.CameraErrorType
import io.github.bgavyus.splash.camera.CameraListener
import io.github.bgavyus.splash.camera.HighSpeedCamera
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.detection.DetectionListener
import io.github.bgavyus.splash.detection.Detector
import io.github.bgavyus.splash.detection.LightningDetector
import io.github.bgavyus.splash.flow.FrameDuplicator
import io.github.bgavyus.splash.flow.FrameDuplicatorListener
import io.github.bgavyus.splash.flow.TextureFrameDuplicator
import io.github.bgavyus.splash.permissions.PermissionGroup
import io.github.bgavyus.splash.permissions.PermissionsActivity
import io.github.bgavyus.splash.recording.Recorder
import io.github.bgavyus.splash.recording.RecorderListener
import io.github.bgavyus.splash.recording.RetroRecorder
import io.github.bgavyus.splash.storage.Storage
import io.github.bgavyus.splash.storage.VideoFile
import kotlinx.android.synthetic.main.activity_viewfinder.*
import java.io.IOException

class ViewfinderActivity : PermissionsActivity(), Thread.UncaughtExceptionHandler, CameraListener,
    DetectionListener, FrameDuplicatorListener, RecorderListener {

    companion object {
        private val TAG = ViewfinderActivity::class.simpleName
    }

    private val closeStack = CloseStack()

    private lateinit var recorder: Recorder
    private lateinit var videoFile: VideoFile
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
        Log.d(TAG, "Running in ${if (Storage.scoped) "scoped" else "legacy"} storage mode")
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

    private fun onPermissionsAvailable() {
        initCamera()
    }

    private fun initCamera() {
        try {
            HighSpeedCamera(this).run {
                camera = this
                closeStack.push(::close)
                onCameraAvailable()
            }
        } catch (error: CameraError) {
            onCameraError(error.type)
        }
    }

    private fun onCameraAvailable() {
        initFrameDuplicator()
    }

    private fun initFrameDuplicator() {
        TextureFrameDuplicator(texture_view, camera.videoSize, this)
    }

    override fun onFrameDuplicatorAvailable(frameDuplicator: FrameDuplicator) {
        this.frameDuplicator = frameDuplicator
        initDetector()
    }

    private fun initDetector() {
        detector = LightningDetector(frameDuplicator.outputBitmap, this).apply {
            closeStack.push(::close)
        }

        onDetectorAvailable()
    }

    private fun onDetectorAvailable() {
        initVideoFile()
    }

    private fun initVideoFile() {
        try {
            videoFile = VideoFile().apply {
                closeStack.push(::close)
            }
        } catch (_: IOException) {
            return finishWithMessage(R.string.error_io)
        }

        onVideoFileAvailable()
    }

    private fun onVideoFileAvailable() {
        initRecorder()
    }

    private fun initRecorder() {
        val rotation = camera.sensorOrientation + App.deviceOrientation
        recorder = RetroRecorder(videoFile, camera.videoSize, camera.fpsRange, rotation, this)
            .apply {
                closeStack.push(::close)
            }

        onRecorderAvailable()
    }

    private fun onRecorderAvailable() {
        startCameraStreaming()
    }

    private fun startCameraStreaming() {
        try {
            camera.startStreaming()
        } catch (error: CameraError) {
            onCameraError(error.type)
        }
    }

    override fun onSurfacesNeeded(): List<Surface> {
        return listOf(frameDuplicator.inputSurface, recorder.inputSurface)
    }

    override fun onCameraStreamStarted() {
        initFrameStream()
    }

    private fun initFrameStream() {
        frameDuplicator.run {
            startStreaming()
            closeStack.push(::stopStreaming)
        }
    }

    override fun onFrameAvailable() {
        detect()
    }

    private fun detect() {
        detector.detect()
    }

    override fun onDetectionStarted() {
        Log.i(TAG, "Detection Started")
        record()
    }

    private fun record() {
        recorder.record()
    }

    override fun onDetectionEnded() {
        Log.i(TAG, "Detection Ended")
        lose()
    }

    private fun lose() {
        recorder.loss()
    }

    override fun onCameraError(type: CameraErrorType) {
        finishWithMessage(
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
    }

    override fun onRecorderError() {
        finishWithMessage(R.string.error_recorder)
    }

    override fun onPause() {
        Log.d(TAG, "Activity.onPause")
        super.onPause()
        close()
    }

    override fun uncaughtException(thread: Thread, error: Throwable) {
        finishWithMessage(R.string.error_uncaught)
    }

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

    private fun close() {
        closeStack.close()
    }
}
