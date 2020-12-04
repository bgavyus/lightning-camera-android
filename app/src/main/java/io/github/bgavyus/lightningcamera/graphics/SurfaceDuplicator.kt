package io.github.bgavyus.lightningcamera.graphics

import android.annotation.SuppressLint
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
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext

class SurfaceDuplicator : DeferScope() {
    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    private val dispatcher = handler.asCoroutineDispatcher(javaClass.simpleName)
    private val scope = CoroutineScope(dispatcher)
        .apply { defer(::cancel) }

    private lateinit var core: EglCore
    private lateinit var program: GlTextureProgram
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var entireViewport: GlRect
    lateinit var surface: Surface

    private val windows = mutableSetOf<EglWindowSurface>()

    suspend fun addSurface(surface: Surface) = withContext(dispatcher) {
        if (windows.isEmpty()) {
            core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
                .apply { defer(::release) }
        }

        val windowSurface = EglWindowSurface(core, surface)
            .apply { defer(::release) }

        if (windows.isEmpty()) {
            windowSurface.makeCurrent()
        }

        windows.add(windowSurface)
    }

    suspend fun start() = withContext(dispatcher) {
        val texture = GlTexture()

        program = GlTextureProgram().apply {
            this.texture = texture
            defer(::release)
        }

        surfaceTexture = SurfaceTexture(texture.id).apply {
            defer(::release)

            updates(handler)
                .onEach { onFrameAvailable() }
                .launchIn(scope)
        }

        entireViewport = GlRect()
            .apply { defer(::release) }

        @SuppressLint("Recycle")
        surface = Surface(surfaceTexture)
    }

    fun setBufferSize(bufferSize: Size) {
        surfaceTexture.setDefaultBufferSize(bufferSize.width, bufferSize.height)
    }

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
