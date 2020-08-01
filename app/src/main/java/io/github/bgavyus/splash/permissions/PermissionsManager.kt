package io.github.bgavyus.splash.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import dagger.hilt.android.qualifiers.ActivityContext
import io.github.bgavyus.splash.storage.Storage
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

class PermissionsManager @Inject constructor(
    @ActivityContext private val context: Context,
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

        Log.i(TAG, "Requesting permissions")
        request(permissionsToRequest)

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

    private suspend fun request(permissions: Collection<String>) {
        val activity = context as ComponentActivity
        val result = activity.call(RequestMultiplePermissions(), permissions.toTypedArray())
        Log.d(TAG, "Result: $result")
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

private suspend fun <Input, Output> ComponentActivity.call(
    contract: ActivityResultContract<Input, Output>,
    input: Input
): Output {
    val deferred = CompletableDeferred<Output>()
    val launcher = registerForActivityResult(contract) { deferred.complete(it) }
    launcher.launch(input)
    val output = deferred.await()
    launcher.unregister()
    return output
}
