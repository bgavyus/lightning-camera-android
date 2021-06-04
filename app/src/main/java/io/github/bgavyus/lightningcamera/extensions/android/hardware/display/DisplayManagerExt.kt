package io.github.bgavyus.lightningcamera.extensions.android.hardware.display

import android.hardware.display.DisplayManager
import android.os.Handler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

@Suppress("BlockingMethodInNonBlockingContext")
fun DisplayManager.metricsChanges(handler: Handler? = null) = callbackFlow {
    val listener = object : DisplayManager.DisplayListener {
        override fun onDisplayChanged(displayId: Int) {
            trySendBlocking(displayId)
        }

        override fun onDisplayAdded(displayId: Int) {}
        override fun onDisplayRemoved(displayId: Int) {}
    }

    registerDisplayListener(listener, handler)
    awaitClose { unregisterDisplayListener(listener) }
}
