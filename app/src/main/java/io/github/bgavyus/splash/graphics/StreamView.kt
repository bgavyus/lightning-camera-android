package io.github.bgavyus.splash.graphics

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.Size
import android.util.SizeF
import android.view.Surface
import android.view.TextureView
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Device
import kotlinx.coroutines.CompletableDeferred
import kotlin.math.min

class StreamView(
    private val device: Device,
    private val textureView: TextureView,
    private val bufferSize: Size
) : DeferScope(), ImageConsumer, TextureView.SurfaceTextureListener {
    private lateinit var _surface: Surface
    private val startCompletion = CompletableDeferred<Unit>()

    suspend fun start() {
        textureView.run {
            surfaceTextureListener = this@StreamView

            if (isAvailable) {
                createSurface()
            }
        }

        startCompletion.await()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) =
        createSurface()

    private fun createSurface() {
        setBufferSize()

        _surface = Surface(textureView.surfaceTexture)
            .apply { defer(::release) }

        startCompletion.complete(Unit)
    }

    override val surface: Surface
        get() {
            adjustBuffer()
            return _surface
        }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) =
        adjustBuffer()

    private fun adjustBuffer() {
        setBufferSize()
        setTransform()
    }

    private fun setBufferSize() =
        textureView.surfaceTexture.setDefaultBufferSize(bufferSize.width, bufferSize.height)

    private fun setTransform() {
        val matrix = Matrix()
        val viewSize = SizeF(textureView.width.toFloat(), textureView.height.toFloat())
        val viewRect = RectF(0f, 0f, viewSize.width, viewSize.height)
        val bufferRect = RectF(0f, 0f, bufferSize.height.toFloat(), bufferSize.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()
        val scale = min(viewSize.width, viewSize.height) / bufferSize.height

        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
        matrix.postScale(scale, scale, centerX, centerY)
        matrix.postRotate(device.orientation.degrees.toFloat(), centerX, centerY)

        textureView.setTransform(matrix)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = /* releaseSurface = */ true
}
