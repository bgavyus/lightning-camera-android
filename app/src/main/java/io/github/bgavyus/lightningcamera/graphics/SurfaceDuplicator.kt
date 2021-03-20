package io.github.bgavyus.lightningcamera.graphics

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.os.Handler
import android.util.Size
import android.view.Surface
import com.otaliastudios.opengl.core.EglCore
import com.otaliastudios.opengl.draw.GlRect
import com.otaliastudios.opengl.program.GlTextureProgram
import com.otaliastudios.opengl.surface.EglWindowSurface
import com.otaliastudios.opengl.texture.GlTexture
import io.github.bgavyus.lightningcamera.extensions.android.graphics.frames
import io.github.bgavyus.lightningcamera.extensions.android.graphics.setDefaultBufferSize
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class SurfaceDuplicator(
    private val handler: Handler,
    bufferSize: Size,
    surfaces: Iterable<Surface>,
) : DeferScope() {
    private val scope = CoroutineScope(handler.asCoroutineDispatcher())
        .apply { defer(::cancel) }

    private val core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
        .apply { defer(::release) }

    private val windows = surfaces
        .map { EglWindowSurface(core, it) }
        .onEach { defer(it::release) }
        .apply { first().makeCurrent() }

    private val texture = GlTexture()

    private val program = GlTextureProgram()
        .apply { defer(::release) }
        .also { it.texture = texture }

    private val entireViewport = GlRect()
        .apply { defer(::release) }

    val surface: Surface

    init {
        @SuppressLint("Recycle")
        val surfaceTexture = SurfaceTexture(texture.id).apply {
            defer(::release)
            setDefaultBufferSize(bufferSize)

            frames(handler)
                .onEach(::onFrameAvailable)
                .launchIn(scope)
        }

        surface = Surface(surfaceTexture)
    }

    private fun onFrameAvailable(surfaceTexture: SurfaceTexture) {
        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(program.textureTransform)

        windows.forEach { window ->
            window.makeCurrent()
            window.setPresentationTime(surfaceTexture.timestamp)
            program.draw(entireViewport)
            window.swapBuffers()
        }
    }
}
