package io.github.bgavyus.lightningcamera.permissions

import android.content.Context
import androidx.activity.ComponentActivity
import dagger.hilt.android.qualifiers.ActivityContext
import io.github.bgavyus.lightningcamera.extensions.android.content.hasGranted
import io.github.bgavyus.lightningcamera.extensions.androidx.activity.result.requestMultiplePermissions
import io.github.bgavyus.lightningcamera.logging.Logger
import javax.inject.Inject

class PermissionsRequester @Inject constructor(
    @ActivityContext private val context: Context,
) {
    private val caller = context as ComponentActivity

    suspend fun requestMissing(permissions: Iterable<String>): Boolean {
        val missingPermissions = filterMissing(permissions)

        if (missingPermissions.isEmpty()) {
            Logger.log("Permissions already granted")
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
        return result.all(Map.Entry<String, Boolean>::value)
    }
}
