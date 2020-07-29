package io.github.bgavyus.splash.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import io.github.bgavyus.splash.storage.Storage

class PermissionsManager(
    private val context: Context,
    private val storage: Storage,
    private val permissionsRequester: PermissionsRequester
) {
    companion object {
        private val TAG = PermissionsManager::class.simpleName
    }

    suspend fun grantAll() {
        val permissionsToRequest = missingPermissions()

        if (permissionsToRequest.isEmpty()) {
            return
        }

        Log.i(TAG, "Requesting permissions")
        permissionsRequester.request(permissionsToRequest)

        validateAllGranted()
    }

    private fun missingPermissions() = ArrayList<String>().apply {
        if (!cameraGranted) {
            add(Manifest.permission.CAMERA)
        }

        if (!storageGranted) {
            add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    private fun validateAllGranted() {
        validateCameraGranted()
        validateStorageGranted()
    }

    private fun validateCameraGranted() {
        if (!cameraGranted) {
            throw PermissionError(PermissionGroup.Camera)
        }
    }

    private val cameraGranted
        get() = granted(Manifest.permission.CAMERA)

    private fun validateStorageGranted() {
        if (!storageGranted) {
            throw PermissionError(PermissionGroup.Storage)
        }
    }

    private val storageGranted
        get() = !storage.legacy || granted(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun granted(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
}
