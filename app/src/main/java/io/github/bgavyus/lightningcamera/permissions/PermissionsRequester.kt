package io.github.bgavyus.lightningcamera.permissions

import android.content.Context
import androidx.activity.result.ActivityResultCaller
import io.github.bgavyus.lightningcamera.extensions.android.content.hasGranted
import io.github.bgavyus.lightningcamera.extensions.androidx.activity.result.requestMultiplePermissions
import io.github.bgavyus.lightningcamera.logging.Logger
import javax.inject.Inject

class PermissionsRequester @Inject constructor(
    private val context: Context,
    private val caller: ActivityResultCaller,
) {
    suspend fun requestMissing(permissions: Iterable<String>): Boolean {
        val missingPermissions = filterMissing(permissions)

        if (missingPermissions.isEmpty()) {
            return true
        }

        return request(missingPermissions)
    }

    private fun filterMissing(permissions: Iterable<String>) =
        permissions.filterNot(context::hasGranted)

    private suspend fun request(permissions: Collection<String>): Boolean {
        Logger.log("Requesting: ${permissions.joinToString()}")
        val result = caller.requestMultiplePermissions(permissions.toTypedArray())

        Logger.log("Result: $result")
        return result.all(Map.Entry<*, Boolean>::value)
    }
}
