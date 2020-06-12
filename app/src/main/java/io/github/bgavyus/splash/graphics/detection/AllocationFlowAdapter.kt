package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

val Allocation.buffers: Flow<Unit>
    get() = callbackFlow {
        setOnBufferAvailableListener { sendBlocking(Unit) }
        awaitClose { setOnBufferAvailableListener(null) }
    }
