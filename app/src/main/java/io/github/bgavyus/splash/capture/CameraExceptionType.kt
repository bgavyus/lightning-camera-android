package io.github.bgavyus.splash.capture

import androidx.annotation.StringRes
import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.common.ResourceCapable

enum class CameraExceptionType(@StringRes override val resourceId: Int): ResourceCapable {
    HighSpeedNotAvailable(R.string.error_high_speed_camera_not_available),
    ConfigureFailed(R.string.error_camera_generic),
    Disconnected(R.string.error_camera_disconnected),
    InUse(R.string.error_camera_in_use),
    MaxInUse(R.string.error_max_cameras_in_use),
    Disabled(R.string.error_camera_disabled),
    Device(R.string.error_camera_device),
    Service(R.string.error_camera_generic),
    Unknown(R.string.error_camera_generic);
}
