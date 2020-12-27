package io.github.bgavyus.lightningcamera.permissions

import androidx.annotation.StringRes
import io.github.bgavyus.lightningcamera.common.ResourceCapable

open class PermissionMissingException(@StringRes override val resourceId: Int) : Exception(),
    ResourceCapable
