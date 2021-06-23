package io.github.bgavyus.lightningcamera.graphics

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
    handler: Handler,
    bufferSize: Size,
    surfaces: Iterable<Surface>,
) : DeferScope() {
    val surface = run {
        val scope = CoroutineScope(handler.asCoroutineDispatcher())
            .apply { defer(::cancel) }

        val core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
            .apply { defer(::release) }

        val windows = surfaces
            .map { EglWindowSurface(core, it) }
            .onEach { defer(it::release) }
            .apply { first().makeCurrent() }

        val texture = GlTexture()

        val program = GlTextureProgram()
            .apply { defer(::release) }
            .also { it.texture = texture }

        val entireViewport = GlRect()
            .apply { defer(::release) }

        val surfaceTexture = SurfaceTexture(texture.id).apply {
            defer(::release)
            setDefaultBufferSize(bufferSize)

            frames(handler)
                .onEach { surfaceTexture ->
                    surfaceTexture.updateTexImage()
                    surfaceTexture.getTransformMatrix(program.textureTransform)

                    windows.forEach { window ->
                        window.makeCurrent()
                        window.setPresentationTime(surfaceTexture.timestamp)
                        program.draw(entireViewport)
                        window.swapBuffers()
                    }
                }
                .launchIn(scope)
        }

        Surface(surfaceTexture)
    }
}
