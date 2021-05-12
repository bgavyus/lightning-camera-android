package io.github.bgavyus.lightningcamera.extensions.android.renderscript

import android.renderscript.Allocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun Allocation.buffers() = callbackFlow {
    setOnBufferAvailableListener { trySendBlocking(it) }
    awaitClose { setOnBufferAvailableListener(null) }
}
