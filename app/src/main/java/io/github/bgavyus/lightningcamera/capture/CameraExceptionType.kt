package io.github.bgavyus.lightningcamera.capture

import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraDevice
import androidx.annotation.StringRes
import io.github.bgavyus.lightningcamera.R

enum class CameraExceptionType {
    HighSpeedNotAvailable,
    ConfigureFailed,
    Disconnected,
    InUse,
    MaxInUse,
    Disabled,
    Device,
    Service,
    Unknown;

    companion object {
        fun fromAccessException(exception: CameraAccessException) = when (exception.reason) {
            CameraAccessException.CAMERA_IN_USE -> InUse
            CameraAccessException.MAX_CAMERAS_IN_USE -> MaxInUse
            CameraAccessException.CAMERA_DISABLED -> Disabled
            CameraAccessException.CAMERA_ERROR -> Device
            else -> Unknown
        }

        fun fromStateError(error: Int) = when (error) {
            CameraDevice.StateCallback.ERROR_CAMERA_IN_USE -> InUse
            CameraDevice.StateCallback.ERROR_MAX_CAMERAS_IN_USE -> MaxInUse
            CameraDevice.StateCallback.ERROR_CAMERA_DISABLED -> Disabled
            CameraDevice.StateCallback.ERROR_CAMERA_DEVICE -> Device
            CameraDevice.StateCallback.ERROR_CAMERA_SERVICE -> Service
            else -> Unknown
        }
    }

    val message
        @StringRes get() = when (this) {
            HighSpeedNotAvailable -> R.string.error_high_speed_camera_not_available
            Disconnected -> R.string.error_camera_disconnected
            InUse -> R.string.error_camera_in_use
            MaxInUse -> R.string.error_max_cameras_in_use
            Disabled -> R.string.error_camera_disabled
            Device -> R.string.error_camera_device
            else -> R.string.error_camera_generic
        }
}
