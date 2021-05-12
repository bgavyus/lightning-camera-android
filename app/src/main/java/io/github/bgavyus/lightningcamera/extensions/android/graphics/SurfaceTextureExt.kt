package io.github.bgavyus.lightningcamera.extensions.android.graphics

import android.graphics.SurfaceTexture
import android.os.Handler
import android.util.Size
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun SurfaceTexture.setDefaultBufferSize(size: Size) = setDefaultBufferSize(size.width, size.height)

fun SurfaceTexture.frames(handler: Handler? = null) = callbackFlow {
    setOnFrameAvailableListener({ trySendBlocking(it) }, handler)
    awaitClose { setOnFrameAvailableListener(null) }
}
