package io.github.bgavyus.lightningcamera.permissions

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.extensions.android.content.hasGranted
import io.github.bgavyus.lightningcamera.logging.Logger
import javax.inject.Inject

class PermissionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionRequester: PermissionRequester,
) {
    suspend fun requestMissing(permissions: Iterable<String>): Boolean {
        val missingPermissions = permissions.filterNot(context::hasGranted)

        if (missingPermissions.isEmpty()) {
            Logger.log("Permissions already granted")
            return true
        }

        Logger.log("Requesting: ${missingPermissions.joinToString()}")
        val result = permissionRequester.request(missingPermissions)

        Logger.log("Result: $result")
        return result.all(Map.Entry<String, Boolean>::value)
    }
}
