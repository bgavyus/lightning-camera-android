package io.github.bgavyus.lightningcamera.permissions

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import dagger.hilt.android.qualifiers.ActivityContext
import kotlinx.coroutines.CompletableDeferred
import javax.inject.Inject

class PermissionRequester @Inject constructor(
    @ActivityContext private val context: Context
) {
    suspend fun request(permissions: Collection<String>): Map<String, Boolean> {
        val activity = context as ComponentActivity
        val contract = ActivityResultContracts.RequestMultiplePermissions()
        return activity.call(contract, permissions.toTypedArray())
    }
}

private suspend fun <Input, Output> ComponentActivity.call(
    contract: ActivityResultContract<Input, Output>,
    input: Input,
): Output {
    val deferred = CompletableDeferred<Output>()
    val launcher = registerForActivityResult(contract, deferred::complete)
    launcher.launch(input)
    val output = deferred.await()
    launcher.unregister()
    return output
}
