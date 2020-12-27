package io.github.bgavyus.lightningcamera.permissions

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.lightningcamera.common.Logger
import javax.inject.Inject

class PermissionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionRequester: PermissionRequester
) {
    private val requestedPermissions = context.requestedPermissions().toSet()
        .also { Logger.info("Requested permissions: ${it.joinToString()}") }

    suspend fun requestMissing(permissions: Collection<String>): Set<String> {
        val missingPermissions = permissions.filterNot(::isPermitted)

        if (missingPermissions.isEmpty()) {
            return emptySet()
        }

        Logger.info("Requesting permissions: ${missingPermissions.joinToString()}")
        val result = permissionRequester.request(missingPermissions)
        return result.filterNot(Map.Entry<String, Boolean>::value).keys
    }

    private fun isPermitted(permission: String) =
        permission !in requestedPermissions || context.hasGranted(permission)
}

private fun Context.requestedPermissions() =
    packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS).requestedPermissions

private fun Context.hasGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
