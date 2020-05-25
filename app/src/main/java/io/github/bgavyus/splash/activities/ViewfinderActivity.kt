package io.github.bgavyus.splash.activities

import android.os.Bundle
import android.util.Log
import android.view.View
import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.capture.CameraError
import io.github.bgavyus.splash.capture.CameraErrorType
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.Deferrer
import io.github.bgavyus.splash.databinding.ActivityViewfinderBinding
import io.github.bgavyus.splash.flow.DetectionRecorder
import io.github.bgavyus.splash.permissions.PermissionGroup
import io.github.bgavyus.splash.permissions.PermissionsActivity
import io.github.bgavyus.splash.storage.Storage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.IOException

// TODO: Handle rotation
// TODO: Use images in toggle button
// TODO: Indicate that capture has occurred (visual + audible)
// TODO: Replace with fragment
class ViewfinderActivity : PermissionsActivity(), Thread.UncaughtExceptionHandler {
    companion object {
        private val TAG = ViewfinderActivity::class.simpleName
    }

    private val deferrer = Deferrer()
    private val scope = MainScope()

    private lateinit var binding: ActivityViewfinderBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate(savedInstanceState = $savedInstanceState)")
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
        Log.d(TAG, "onResume")
        super.onResume()
        init()
    }

    private fun init() {
        if (!allPermissionsGranted()) {
            Log.i(TAG, "Requesting permissions")
            requestNonGrantedPermissions()
            return
        }

        initDetectionRecorder()
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

    private fun initDetectionRecorder() {
        scope.launch {
            try {
                val recorder = DetectionRecorder.init(binding.textureView)
                    .apply { deferrer.defer(::close) }

                binding.toggleButton.setOnCheckedChangeListener { _, checked ->
                    if (checked) {
                        recorder.record()
                    } else {
                        recorder.loss()
                    }
                }

                deferrer.defer {
                    Log.d(TAG, "Removing toggle listener")
                    binding.toggleButton.setOnCheckedChangeListener(null)
                }
            } catch (error: CameraError) {
                finishWithMessage(cameraErrorToMessage(error.type))
            } catch (_: IOException) {
                finishWithMessage(R.string.error_io)
            }
        }
    }

    private fun cameraErrorToMessage(type: CameraErrorType) = when (type) {
        CameraErrorType.HighSpeedNotAvailable -> R.string.error_high_speed_camera_not_available
        CameraErrorType.InUse -> R.string.error_camera_in_use
        CameraErrorType.MaxInUse -> R.string.error_max_cameras_in_use
        CameraErrorType.Disabled -> R.string.error_camera_disabled
        CameraErrorType.Device -> R.string.error_camera_device
        CameraErrorType.Disconnected -> R.string.error_camera_disconnected
        CameraErrorType.ConfigureFailed -> R.string.error_camera_generic
        CameraErrorType.Generic -> R.string.error_camera_generic
    }

    override fun onPause() {
        Log.d(TAG, "onPause")
        close()
        super.onPause()
    }

    override fun uncaughtException(thread: Thread, error: Throwable) {
        Log.e(TAG, "Uncaught Exception from: ${thread.name}", error)
        finishWithMessage(R.string.error_uncaught)
    }

    private fun finishWithMessage(resourceId: Int) {
        Log.d(TAG, "finishWithMessage: ${App.defaultString(resourceId)}")
        App.showMessage(resourceId)
        finish()
    }

    override fun finish() {
        Log.d(TAG, "finish")
        close()
        super.finish()
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy")
        super.onDestroy()
        scope.cancel()
    }

    private fun close() = deferrer.close()
}
