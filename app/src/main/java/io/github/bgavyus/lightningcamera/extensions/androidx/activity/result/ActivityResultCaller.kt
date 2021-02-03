package io.github.bgavyus.lightningcamera.extensions.androidx.activity.result

import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.CompletableDeferred

suspend fun ActivityResultCaller.requestMultiplePermissions(
    permissions: Array<String>,
): Map<String, Boolean> = call(ActivityResultContracts.RequestMultiplePermissions(), permissions)

suspend fun <Input, Output> ActivityResultCaller.call(
    contract: ActivityResultContract<Input, Output>,
    input: Input,
): Output {
    val deferred = CompletableDeferred<Output>()
    val launcher = registerForActivityResult(contract, deferred::complete)

    try {
        launcher.launch(input)
        return deferred.await()
    } finally {
        launcher.unregister()
    }
}
