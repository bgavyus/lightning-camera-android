package io.github.bgavyus.lightningcamera.extensions

import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import kotlinx.coroutines.CompletableDeferred

suspend fun <Input, Output> ComponentActivity.call(
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
