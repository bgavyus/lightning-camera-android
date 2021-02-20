package io.github.bgavyus.lightningcamera.permissions

import androidx.fragment.app.FragmentActivity
import io.github.bgavyus.lightningcamera.extensions.android.content.hasGranted
import io.github.bgavyus.lightningcamera.extensions.androidx.activity.result.requestMultiplePermissions
import io.github.bgavyus.lightningcamera.logging.Logger
import javax.inject.Inject

class PermissionsRequester @Inject constructor(
    private val activity: FragmentActivity,
) {
    suspend fun requestMissing(permissions: Iterable<String>): Boolean {
        val missingPermissions = filterMissing(permissions)

        if (missingPermissions.isEmpty()) {
            Logger.log("Permissions already granted")
            return true
        }

        return request(missingPermissions)
    }

    private fun filterMissing(permissions: Iterable<String>) =
        permissions.filterNot(activity::hasGranted)

    private suspend fun request(permissions: Collection<String>): Boolean {
        Logger.log("Requesting: ${permissions.joinToString()}")
        val result = activity.requestMultiplePermissions(permissions.toTypedArray())

        Logger.log("Result: $result")
        return result.all(Map.Entry<String, Boolean>::value)
    }
}
