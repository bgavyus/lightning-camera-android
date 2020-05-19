package io.github.bgavyus.splash.graphics

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

class ImageConsumerDuplicator(
    consumers: Iterable<ImageConsumer>,
    bufferSize: Size
) : ImageConsumer, SurfaceTexture.OnFrameAvailableListener {
    companion object {
        private val TAG = ImageConsumerDuplicator::class.simpleName
    }

    private val closeStack = CloseStack()
    private val windows: List<EglWindowSurface>
    private val program: GlTextureProgram
    private val surfaceTexture: SurfaceTexture

    init {
        val core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
            .apply { closeStack.push(::release) }

        windows = consumers.map {
            EglWindowSurface(core, it.surface)
                .apply { closeStack.push(::release) }
        }
            .apply { first().makeCurrent() }

        val texture = GlTexture()

        program = GlTextureProgram().apply {
            this.texture = texture
            closeStack.push(::release)
        }

        surfaceTexture = SurfaceTexture(texture.id).apply {
            setOnFrameAvailableListener(this@ImageConsumerDuplicator)
            setDefaultBufferSize(bufferSize.width, bufferSize.height)
            closeStack.push(::release)
        }
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
