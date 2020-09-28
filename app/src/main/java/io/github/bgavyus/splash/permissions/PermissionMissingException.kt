package io.github.bgavyus.splash.permissions

import androidx.annotation.StringRes
import io.github.bgavyus.splash.R
import io.github.bgavyus.splash.common.ResourceCapable

open class PermissionMissingException(group: PermissionGroup) : Exception(), ResourceCapable {
    @StringRes override val resourceId = when (group) {
        PermissionGroup.Camera -> R.string.error_camera_permission_not_granted
        PermissionGroup.Storage -> R.string.error_storage_permission_not_granted
    }
}
