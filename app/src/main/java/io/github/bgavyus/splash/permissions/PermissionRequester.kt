package io.github.bgavyus.splash.permissions

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
    suspend fun request(permissions: Collection<String>): MutableMap<String, Boolean> {
        val activity = context as ComponentActivity

        return activity.call(
            ActivityResultContracts.RequestMultiplePermissions(),
            permissions.toTypedArray()
        )
    }
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
