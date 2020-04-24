package io.github.bgavyus.splash.flow

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.util.Size
import android.util.SizeF
import android.view.Surface
import android.view.TextureView
import io.github.bgavyus.splash.common.App
import kotlin.math.min

class TextureFrameDuplicator(
    private val textureView: TextureView,
    private val bufferSize: Size,
    private val listener: FrameDuplicatorListener
) : FrameDuplicator, TextureView.SurfaceTextureListener {

    private lateinit var surface: Surface
    private lateinit var bitmap: Bitmap

    init {
        if (textureView.isAvailable) {
            loadSurface()
        } else {
            textureView.surfaceTextureListener = this
        }
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
        textureView.surfaceTextureListener = null
        loadSurface()
    }

    private fun loadSurface() {
        setBufferSize()
        surface = Surface(textureView.surfaceTexture)
        listener.onFrameDuplicatorAvailable(this)
    }

    override val inputSurface: Surface
        get() {
            adjustBuffer()
            return surface
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

    override val outputBitmap: Bitmap
        get() {
            if (!::bitmap.isInitialized) {
                bitmap = textureView.bitmap
            }

            return bitmap
        }

    override fun startStreaming() {
        textureView.surfaceTextureListener = this
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {
        textureView.getBitmap(bitmap)
        listener.onFrameAvailable()
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
        stopStreaming()
        return true
    }

    override fun stopStreaming() {
        textureView.surfaceTextureListener = null
    }
}
