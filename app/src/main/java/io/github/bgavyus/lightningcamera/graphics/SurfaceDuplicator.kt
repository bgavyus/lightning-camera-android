package io.github.bgavyus.lightningcamera.graphics

import android.graphics.SurfaceTexture
import android.os.Build
import android.os.Handler
import android.util.Size
import android.view.Surface
import com.otaliastudios.opengl.core.EglCore
import com.otaliastudios.opengl.draw.GlRect
import com.otaliastudios.opengl.program.GlTextureProgram
import com.otaliastudios.opengl.surface.EglWindowSurface
import com.otaliastudios.opengl.texture.GlTexture
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.Logger
import io.github.bgavyus.lightningcamera.extensions.kotlinx.callOnEach
import io.github.bgavyus.lightningcamera.extensions.android.graphics.setDefaultBufferSize
import io.github.bgavyus.lightningcamera.extensions.android.graphics.updates
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn

class SurfaceDuplicator(
    private val handler: Handler,
    bufferSize: Size,
    surfaces: Iterable<Surface>,
) : DeferScope() {
    private val dispatcher = handler.asCoroutineDispatcher(javaClass.simpleName)
    private val scope = CoroutineScope(dispatcher)
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

    private val surfaceTexture = SurfaceTexture(texture.id).apply {
        defer(::release)
        setDefaultBufferSize(bufferSize)

        updates(handler)
            .callOnEach(::onFrameAvailable)
            .launchIn(scope)
    }

    private val entireViewport = GlRect()
        .apply { defer(::release) }

    val surface = Surface(surfaceTexture)

    private fun onFrameAvailable() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (surfaceTexture.isReleased) {
                Logger.debug("Ignoring frame after release")
                return
            }
        }

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(program.textureTransform)

        windows.forEach { window ->
            window.makeCurrent()
            program.draw(entireViewport)
            window.swapBuffers()
        }
    }
}
