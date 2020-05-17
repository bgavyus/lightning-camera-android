package io.github.bgavyus.splash.ui.views

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.Size
import android.util.SizeF
import android.view.Surface
import android.view.TextureView
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.ImageConsumer
import kotlin.math.min

class StreamView(
    private val textureView: TextureView,
    private val bufferSize: Size,
    private val listener: StreamViewListener
) : ImageConsumer, TextureView.SurfaceTextureListener {
    private lateinit var _surface: Surface

    init {
        if (textureView.isAvailable) {
            loadSurface()
        } else {
            textureView.surfaceTextureListener = this
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        loadSurface()
    }

    private fun loadSurface() {
        setBufferSize()
        _surface = Surface(textureView.surfaceTexture)
        listener.onStreamViewAvailable(this)
    }

    // TODO: Create ImageConsumer interface
    override val surface: Surface
        get() {
            adjustBuffer()
            return _surface
        }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture?, width: Int, height: Int) {
        adjustBuffer()
    }

    private fun adjustBuffer() {
        setBufferSize()
        setTransform()
    }

    private fun setBufferSize() {
        textureView.surfaceTexture.setDefaultBufferSize(bufferSize.width, bufferSize.height)
    }

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
        matrix.postRotate(App.deviceOrientation.degrees.toFloat(), centerX, centerY)

        textureView.setTransform(matrix)
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        return true
    }
}
