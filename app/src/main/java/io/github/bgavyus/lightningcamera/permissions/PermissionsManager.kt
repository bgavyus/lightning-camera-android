package io.github.bgavyus.lightningcamera.permissions

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.extensions.hasGranted
import javax.inject.Inject

class PermissionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionRequester: PermissionRequester,
) {
    suspend fun requestMissing(permissions: Collection<String>): Boolean {
        val missingPermissions = permissions.filterNot(context::hasGranted)

        if (missingPermissions.isEmpty()) {
            Logger.info("Permissions already granted")
            return true
        }

        Logger.info("Requesting: ${missingPermissions.joinToString()}")
        val result = permissionRequester.request(missingPermissions)

        Logger.info("Result: $result")
        return result.all(Map.Entry<String, Boolean>::value)
    }
}
