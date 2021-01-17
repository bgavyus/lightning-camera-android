package io.github.bgavyus.lightningcamera.extensions

import android.graphics.SurfaceTexture
import android.util.Size
import android.view.TextureView
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun TextureView.surfaceTextureEvents() = callbackFlow<SurfaceTextureEvent> {
    val listener = object : TextureView.SurfaceTextureListener {
        override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
            sendBlocking(SurfaceTextureEvent.Available(surface))
            onSurfaceTextureSizeChanged(surface, width, height)
        }

        override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) =
            sendBlocking(SurfaceTextureEvent.SizeChanged(Size(width, height)))

        override fun onSurfaceTextureUpdated(surface: SurfaceTexture) =
            sendBlocking(SurfaceTextureEvent.Updated)

        override fun onSurfaceTextureDestroyed(surface: SurfaceTexture) = /* shouldRelease */ false
    }

    surfaceTexture?.let { listener.onSurfaceTextureAvailable(it, width, height) }
    surfaceTextureListener = listener
    awaitClose()
}

sealed class SurfaceTextureEvent {
    data class Available(val surface: SurfaceTexture) : SurfaceTextureEvent()
    data class SizeChanged(val size: Size) : SurfaceTextureEvent()
    object Updated : SurfaceTextureEvent()
}
