package io.github.bgavyus.splash.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.bgavyus.splash.storage.Storage
import javax.inject.Inject

class PermissionsManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val permissionRequester: PermissionRequester,
    private val storage: Storage
) {
    companion object {
        private val TAG = PermissionsManager::class.simpleName
    }

    suspend fun grantAll() {
        val permissionsToRequest = missingPermissions()

        if (permissionsToRequest.isEmpty()) {
            return
        }

        request(permissionsToRequest)
        validateAllGranted()
    }

    private suspend fun request(permissions: Collection<String>) {
        Log.i(TAG, "Requesting permissions")
        val result = permissionRequester.request(permissions)
        Log.d(TAG, "Result: $result")
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
            throw PermissionMissingException(PermissionGroup.Camera)
        }
    }

    private val cameraGranted
        get() = granted(Manifest.permission.CAMERA)

    private fun validateStorageGranted() {
        if (!storageGranted) {
            throw PermissionMissingException(PermissionGroup.Storage)
        }
    }

    private val storageGranted
        get() = !storage.legacy || granted(Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private fun granted(permission: String) =
        context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

    fun canRequestPermission(id: String) =
        context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
            .requestedPermissions
            .contains(id)
}
