package io.github.bgavyus.splash.graphics.views

import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Size
import android.util.SizeF
import android.view.Surface
import android.view.TextureView
import com.otaliastudios.opengl.core.EglCore
import com.otaliastudios.opengl.surface.EglOffscreenSurface
import com.otaliastudios.opengl.texture.GlTexture
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.graphics.ImageConsumer
import kotlin.math.min

class StreamView(
    private val textureView: TextureView,
    private val bufferSize: Size
) : ImageConsumer, TextureView.SurfaceTextureListener {
    companion object {
        private val detachedSurfaceTexture: SurfaceTexture
            get() {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                    val closeStack = CloseStack()

                    val core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
                        .apply { closeStack.push(::release) }

                    EglOffscreenSurface(core, width = 0, height = 0).run {
                        closeStack.push(::release)
                        makeCurrent()
                    }

                    val texture = GlTexture()
                    val surfaceTexture = SurfaceTexture(texture.id)
                    surfaceTexture.detachFromGLContext()
                    closeStack.close()
                    return surfaceTexture
                }

                return SurfaceTexture(/* singleBufferMode = */ false)
            }
    }

    private val closeStack = CloseStack()

    init {
        textureView.run {
            surfaceTexture = detachedSurfaceTexture
            surfaceTextureListener = this@StreamView
        }

        setBufferSize()
    }

    private var _surface = Surface(textureView.surfaceTexture)
        .also(closeStack::push)

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
        matrix.postRotate(App.deviceOrientation.degrees.toFloat(), centerX, centerY)

        textureView.setTransform(matrix)
    }

    override fun close() = closeStack.close()
    override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture?) {}
    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?) = true
}
