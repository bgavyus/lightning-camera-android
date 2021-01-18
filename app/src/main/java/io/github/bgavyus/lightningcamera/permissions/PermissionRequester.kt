package io.github.bgavyus.lightningcamera.permissions

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.qualifiers.ActivityContext
import io.github.bgavyus.lightningcamera.extensions.androidx.activity.call
import javax.inject.Inject

class PermissionRequester @Inject constructor(
    @ActivityContext private val context: Context,
) {
    suspend fun request(permissions: Collection<String>): Map<String, Boolean> {
        val activity = context as ComponentActivity
        val contract = ActivityResultContracts.RequestMultiplePermissions()
        return activity.call(contract, permissions.toTypedArray())
    }
}
