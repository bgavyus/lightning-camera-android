package io.github.bgavyus.lightningcamera.extensions.android.view

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.TextureView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun TextureView.surfaceTextureEvents() = callbackFlow {
    val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            trySendBlocking(SurfaceTextureEvent.Available(surface))
            onSurfaceTextureSizeChanged(surface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
            trySendBlocking(SurfaceTextureEvent.SizeChanged(Size(width, height)))
        }

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            trySendBlocking(SurfaceTextureEvent.Updated)
        }

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = /* shouldRelease */ false
    }

    surfaceTexture?.let { trySendBlocking(SurfaceTextureEvent.Available(it)) }
    surfaceTextureListener = listener
    awaitClose()
}

sealed class SurfaceTextureEvent {
    data class Available(val surface: SurfaceTexture) : SurfaceTextureEvent()
    data class SizeChanged(val size: Size) : SurfaceTextureEvent()
    object Updated : SurfaceTextureEvent()
}
