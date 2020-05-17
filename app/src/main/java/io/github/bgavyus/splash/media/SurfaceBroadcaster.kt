package io.github.bgavyus.splash.media

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.util.Log
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.common.ImageConsumer
import io.github.bgavyus.splash.media.gles.EglCore
import io.github.bgavyus.splash.media.gles.FullFrameRect
import io.github.bgavyus.splash.media.gles.Texture2dProgram
import io.github.bgavyus.splash.media.gles.WindowSurface

// TODO: Organize GLES package
class SurfaceBroadcaster(
    bufferSize: Size,
    consumers: List<ImageConsumer>
) : ImageConsumer, AutoCloseable, SurfaceTexture.OnFrameAvailableListener {
    companion object {
        private val TAG = SurfaceBroadcaster::class.simpleName
    }

    private val closeStack = CloseStack()

    private val eglCore = EglCore(null, EglCore.FLAG_RECORDABLE).apply {
        closeStack.push(::release)
    }

    private val windowSurfaces = consumers.map {
        WindowSurface(eglCore, it.surface, false).apply {
            closeStack.push(::release)
        }
    }.apply {
        first().makeCurrent()
    }

    private val program = Texture2dProgram(Texture2dProgram.ProgramType.TEXTURE_EXT).apply {
        closeStack.push(::release)
    }

    private val fullFrameBlit = FullFrameRect(program)
    private val textureId = fullFrameBlit.createTextureObject()

    private val surfaceTexture = SurfaceTexture(textureId).apply {
        setOnFrameAvailableListener(this@SurfaceBroadcaster)
        setDefaultBufferSize(bufferSize.width, bufferSize.height)
        closeStack.push(::release)
    }

    override val surface = Surface(surfaceTexture).apply {
        closeStack.push(::release)
    }

    init {
        GLES20.glViewport(0, 0, bufferSize.width, bufferSize.height)
    }

    private val matrix = FloatArray(4 * 4)

    override fun onFrameAvailable(surface: SurfaceTexture?) {
        if (closeStack.isEmpty()) {
            Log.d(TAG, "Ignoring frame after release")
            return
        }

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(matrix)

        for (windowSurface in windowSurfaces) {
            windowSurface.makeCurrent()
            fullFrameBlit.drawFrame(textureId, matrix)
            windowSurface.swapBuffers()
        }
    }

    override fun close() = closeStack.close()
}
