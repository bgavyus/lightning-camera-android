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
import io.github.bgavyus.lightningcamera.common.extensions.callOnEach
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn

class SurfaceDuplicator(
    private val handler: Handler,
    bufferSize: Size,
    surfaces: Iterable<Surface>,
) : DeferScope() {
    private val dispatcher = handler.asCoroutineDispatcher(javaClass.simpleName)
    private val scope = CoroutineScope(dispatcher)
        .apply { defer(::cancel) }

    private var core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
        .apply { defer(::release) }

    private var windows = surfaces
        .map { EglWindowSurface(core, it) }
        .onEach { defer(it::release) }
        .apply { first().makeCurrent() }

    private var texture = GlTexture()

    private var program = GlTextureProgram()
        .apply { defer(::release) }
        .also { it.texture = texture }

    private var surfaceTexture = SurfaceTexture(texture.id).apply {
        defer(::release)
        setDefaultBufferSize(bufferSize.width, bufferSize.height)

        updates(handler)
            .callOnEach(::onFrameAvailable)
            .launchIn(scope)
    }

    private var entireViewport = GlRect()
        .apply { defer(::release) }

    var surface = Surface(surfaceTexture)

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

private fun SurfaceTexture.updates(handler: Handler) = callbackFlow {
    setOnFrameAvailableListener({ sendBlocking(Unit) }, handler)
    awaitClose { setOnFrameAvailableListener(null) }
}
