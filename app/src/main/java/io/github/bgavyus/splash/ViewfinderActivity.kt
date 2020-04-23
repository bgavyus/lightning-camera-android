package io.github.bgavyus.splash

import android.media.MediaRecorder
import android.os.Bundle
import android.util.Log
import android.view.Surface
import android.view.View
import io.github.bgavyus.splash.camera.CameraError
import io.github.bgavyus.splash.camera.CameraErrorType
import io.github.bgavyus.splash.camera.CameraListener
import io.github.bgavyus.splash.camera.HighSpeedCamera
import io.github.bgavyus.splash.common.ReleaseStack
import io.github.bgavyus.splash.common.getDefaultString
import io.github.bgavyus.splash.common.showMessage
import io.github.bgavyus.splash.detection.DetectionListener
import io.github.bgavyus.splash.detection.LightningDetector
import io.github.bgavyus.splash.permissions.PermissionGroup
import io.github.bgavyus.splash.permissions.PermissionsActivity
import io.github.bgavyus.splash.recording.HighSpeedRecorder
import io.github.bgavyus.splash.storage.Storage
import io.github.bgavyus.splash.storage.VideoFile
import kotlinx.android.synthetic.main.activity_viewfinder.*
import java.io.IOException

class ViewfinderActivity : PermissionsActivity(), CameraListener, MediaRecorder.OnErrorListener,
    Thread.UncaughtExceptionHandler, DetectionListener, FrameDuplicatorListener {

    companion object {
        private val TAG = ViewfinderActivity::class.simpleName
    }

    private val releaseStack = ReleaseStack()

    private lateinit var recorder: HighSpeedRecorder
    private lateinit var videoFile: VideoFile
    private lateinit var frameDuplicator: FrameDuplicator
    private lateinit var camera: HighSpeedCamera
    private lateinit var detector: LightningDetector

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
        Log.d(TAG, "Device Orientation: ${App.shared.deviceOrientation}")
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
            HighSpeedCamera(listener = this).run {
                camera = this
                releaseStack.push(::release)
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
            releaseStack.push(::release)
        }

        onDetectorAvailable()
    }

    private fun onDetectorAvailable() {
        initVideoFile()
    }

    private fun initVideoFile() {
        try {
            videoFile = VideoFile(getString(R.string.video_folder_name)).apply {
                releaseStack.push(::close)
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
        val rotation = camera.sensorOrientation + App.shared.deviceOrientation
        recorder = HighSpeedRecorder(videoFile, camera.videoSize, camera.fpsRange, rotation).apply {
            setOnErrorListener(this@ViewfinderActivity)
            releaseStack.push(::release)
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
        return listOf(frameDuplicator.inputSurface, recorder.surface)
    }

    override fun onCameraStreamStarted() {
        initFrameStream()
    }

    private fun initFrameStream() {
        frameDuplicator.run {
            startStreaming()
            releaseStack.push(::stopStreaming)
        }
    }

    override fun onFrameAvailable() {
        detect()
    }

    private fun detect() {
        frameDuplicator.propagate()
        detector.process()
    }

    override fun onSubjectEntered() {
        Log.i(TAG, "Subject entered")
        record()
    }

    private fun record() {
        recorder.record()
    }

    override fun onSubjectLeft() {
        Log.i(TAG, "Subject left")
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

    override fun onError(mr: MediaRecorder?, what: Int, extra: Int) {
        Log.d(TAG, "MediaRecorder.onError(what = $what, extra = $extra)")
        finishWithMessage(R.string.error_recorder)
    }

    override fun onPause() {
        Log.d(TAG, "Activity.onPause")
        super.onPause()
        release()
    }

    override fun uncaughtException(thread: Thread, error: Throwable) {
        Log.e(TAG, "Uncaught Exception from ${thread.name}", error)
        finishWithMessage(R.string.error_uncaught)
    }

    private fun finishWithMessage(resourceId: Int) {
        Log.d(TAG, "finishWithMessage: ${getDefaultString(resourceId)}")
        showMessage(resourceId)
        finish()
    }

    override fun finish() {
        Log.d(TAG, "finish")
        super.finish()
        release()
    }

    private fun release() {
        releaseStack.release()
    }
}
