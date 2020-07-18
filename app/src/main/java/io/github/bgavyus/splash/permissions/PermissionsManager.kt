package io.github.bgavyus.splash.permissions

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.fragment.app.FragmentManager
import io.github.bgavyus.splash.common.Application
import io.github.bgavyus.splash.storage.Storage

class PermissionsManager {
    companion object {
        private val TAG = PermissionsManager::class.simpleName

        suspend fun grantAll(fragmentManager: FragmentManager) {
            val permissionsToRequest = missingPermissions

            if (permissionsToRequest.isEmpty()) {
                return
            }

            Log.i(TAG, "Requesting permissions")
            PermissionsRequester.request(permissionsToRequest, fragmentManager)

            validateAllGranted()
        }

        private val missingPermissions
            get() = ArrayList<String>().apply {
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

        fun validateCameraGranted() {
            if (!cameraGranted) {
                throw PermissionError(PermissionGroup.Camera)
            }
        }

        private val cameraGranted get() = granted(Manifest.permission.CAMERA)

        fun validateStorageGranted() {
            if (!storageGranted) {
                throw PermissionError(PermissionGroup.Storage)
            }
        }

        private val storageGranted get() = !Storage.legacy || granted(Manifest.permission.WRITE_EXTERNAL_STORAGE)

        private fun granted(permission: String) =
            Application.context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
    }
}
