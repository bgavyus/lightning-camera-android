package io.github.bgavyus.splash.ui.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.capture.*
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.databinding.ActivityViewfinderBinding
import io.github.bgavyus.splash.detection.DetectionListener
import io.github.bgavyus.splash.detection.MotionDetector
import io.github.bgavyus.splash.media.Recorder
import io.github.bgavyus.splash.media.RecorderListener
import io.github.bgavyus.splash.media.RetroRecorder
import io.github.bgavyus.splash.media.SurfaceBroadcaster
import io.github.bgavyus.splash.permissions.PermissionGroup
import io.github.bgavyus.splash.permissions.PermissionsActivity
import io.github.bgavyus.splash.storage.Storage
import io.github.bgavyus.splash.storage.VideoFile
import io.github.bgavyus.splash.ui.views.StreamView
import io.github.bgavyus.splash.ui.views.StreamViewListener
import java.io.IOException

// TODO: Remove all logic from this class
// TODO: Handle rotation
// TODO: Dim screen after some time
// TODO: Indicate that capture has occurred (visual + audible)
// TODO: Add start/stop button
// TODO: Parallel execution when possible
class ViewfinderActivity : PermissionsActivity(), Thread.UncaughtExceptionHandler, CameraListener,
    DetectionListener, RecorderListener,
    StreamViewListener {

    companion object {
        private val TAG = ViewfinderActivity::class.simpleName
    }

    private val closeStack = CloseStack()

    private lateinit var binding: ActivityViewfinderBinding
    private lateinit var camera: Camera
    private lateinit var recorder: Recorder

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Activity.onCreate(savedInstanceState = $savedInstanceState)")
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        binding = ActivityViewfinderBinding.inflate(layoutInflater)
        setContentView(binding.root)
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

    private fun onPermissionsAvailable() {
        try {
            camera = Camera()

            val file = VideoFile()
                .also(closeStack::push)

            val rotation = camera.orientation + App.deviceOrientation

            recorder = RetroRecorder(file, camera.size, camera.fpsRange, rotation, this)
                .also(closeStack::push)

            StreamView(binding.streamView, camera.size, this)
                .also(closeStack::push)
        } catch (error: CameraError) {
            onCameraError(error.type)
        } catch (_: IOException) {
            finishWithMessage(R.string.error_io)
        }
    }

    override fun onStreamViewAvailable(streamView: StreamView) {
        val detector = MotionDetector(camera.size, this)
            .also(closeStack::push)

        val surfaceBroadcaster = SurfaceBroadcaster(camera.size, listOf(detector, streamView))
            .also(closeStack::push)

        try {
            CameraStream(camera, listOf(recorder, surfaceBroadcaster), this)
                .also(closeStack::push)
        } catch (error: CameraError) {
            onCameraError(error.type)
        }
    }

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

    override fun uncaughtException(thread: Thread, error: Throwable) =
        finishWithMessage(R.string.error_uncaught)

    private fun finishWithMessage(resourceId: Int) {
        Log.d(TAG, "finishWithMessage: ${App.defaultString(resourceId)}")
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
