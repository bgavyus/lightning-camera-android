package io.github.bgavyus.splash.common

import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.capture.CameraError
import io.github.bgavyus.splash.capture.CameraErrorType
import io.github.bgavyus.splash.permissions.PermissionError
import io.github.bgavyus.splash.permissions.PermissionGroup
import java.io.IOException

val PermissionError.resourceId get() = when (group) {
    PermissionGroup.Camera -> R.string.error_camera_permission_not_granted
    PermissionGroup.Storage -> R.string.error_storage_permission_not_granted
}

val CameraError.resourceId get() = when (type) {
    CameraErrorType.HighSpeedNotAvailable -> R.string.error_high_speed_camera_not_available
    CameraErrorType.InUse -> R.string.error_camera_in_use
    CameraErrorType.MaxInUse -> R.string.error_max_cameras_in_use
    CameraErrorType.Disabled -> R.string.error_camera_disabled
    CameraErrorType.Device -> R.string.error_camera_device
    CameraErrorType.Disconnected -> R.string.error_camera_disconnected
    CameraErrorType.ConfigureFailed -> R.string.error_camera_generic
    CameraErrorType.Service -> R.string.error_camera_generic
    CameraErrorType.Unknown -> R.string.error_camera_generic
}

val IOException.resourceId get() = R.string.error_io
