package io.github.bgavyus.splash.media

import android.graphics.SurfaceTexture
import android.util.Log
import android.util.Size
import android.view.Surface
import com.otaliastudios.opengl.core.EglCore
import com.otaliastudios.opengl.draw.GlRect
import com.otaliastudios.opengl.program.GlTextureProgram
import com.otaliastudios.opengl.surface.EglWindowSurface
import com.otaliastudios.opengl.texture.GlTexture
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.common.ImageConsumer

class SurfaceBroadcaster(
    bufferSize: Size,
    consumers: Iterable<ImageConsumer>
) : ImageConsumer, SurfaceTexture.OnFrameAvailableListener {
    companion object {
        private val TAG = SurfaceBroadcaster::class.simpleName
    }

    private val closeStack = CloseStack()

    private val core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
        .apply { closeStack.push(::release) }

    private val windows = consumers.map {
        EglWindowSurface(core, it.surface)
            .apply { closeStack.push(::release) }
    }.apply { first().makeCurrent() }

    private val texture = GlTexture()

    private val program = GlTextureProgram().apply {
        texture = this@SurfaceBroadcaster.texture
        closeStack.push(::release)
    }

    private val surfaceTexture = SurfaceTexture(texture.id).apply {
        setOnFrameAvailableListener(this@SurfaceBroadcaster)
        setDefaultBufferSize(bufferSize.width, bufferSize.height)
        closeStack.push(::release)
    }

    private val entireViewport = GlRect()

    override val surface = Surface(surfaceTexture)
        .also(closeStack::push)

    override fun onFrameAvailable(surface: SurfaceTexture?) {
        if (closeStack.isEmpty()) {
            Log.d(TAG, "Ignoring frame after release")
            return
        }

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(program.textureTransform)

        for (window in windows) {
            window.makeCurrent()
            program.draw(entireViewport)
            window.swapBuffers()
        }
    }

    override fun close() = closeStack.close()
}
