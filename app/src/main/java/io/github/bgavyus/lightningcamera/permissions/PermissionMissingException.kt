package io.github.bgavyus.lightningcamera.permissions

import androidx.annotation.StringRes
import io.github.bgavyus.lightningcamera.R
import io.github.bgavyus.lightningcamera.common.ResourceCapable

open class PermissionMissingException(val group: PermissionGroup) : Exception(), ResourceCapable {
    @StringRes override val resourceId = when (group) {
        PermissionGroup.Camera -> R.string.error_camera_permission_not_granted
        PermissionGroup.Storage -> R.string.error_storage_permission_not_granted
    }
}
