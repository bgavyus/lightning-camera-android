package io.github.bgavyus.lightningcamera.extensions.android.media

import android.media.ImageReader
import android.os.Handler
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun ImageReader.images(handler: Handler) = callbackFlow {
    setOnImageAvailableListener({ trySendBlocking(it) }, handler)
    awaitClose { setOnImageAvailableListener(null, null) }
}
