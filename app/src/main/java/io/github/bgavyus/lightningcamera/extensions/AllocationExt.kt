package io.github.bgavyus.lightningcamera.extensions

import android.renderscript.Allocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun Allocation.buffers() = callbackFlow {
    setOnBufferAvailableListener { sendBlocking(ioReceive()) }
    awaitClose { setOnBufferAvailableListener(null) }
}
