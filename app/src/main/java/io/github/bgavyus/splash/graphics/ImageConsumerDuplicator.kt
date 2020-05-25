package io.github.bgavyus.splash.graphics

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.Surface
import com.otaliastudios.opengl.core.EglCore
import com.otaliastudios.opengl.draw.GlRect
import com.otaliastudios.opengl.program.GlTextureProgram
import com.otaliastudios.opengl.surface.EglWindowSurface
import com.otaliastudios.opengl.texture.GlTexture
import io.github.bgavyus.splash.common.Deferrer
import io.github.bgavyus.splash.common.SingleThreadHandler
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext

class ImageConsumerDuplicator private constructor() : Deferrer(), ImageConsumer,
    SurfaceTexture.OnFrameAvailableListener {
    companion object {
        private val TAG = ImageConsumerDuplicator::class.simpleName

        suspend fun init(consumers: Iterable<ImageConsumer>, bufferSize: Size) =
            ImageConsumerDuplicator().apply { init(consumers, bufferSize) }
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    private lateinit var windows: List<EglWindowSurface>
    private lateinit var program: GlTextureProgram
    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var entireViewport: GlRect
    override lateinit var surface: Surface

    @SuppressLint("Recycle")
    private suspend fun init(consumers: Iterable<ImageConsumer>, bufferSize: Size) =
        withContext(handler.asCoroutineDispatcher(TAG)) {
            val core = EglCore(flags = EglCore.FLAG_TRY_GLES3)
                .apply { defer(::release) }

            windows = consumers.map {
                EglWindowSurface(core, it.surface)
                    .apply { defer(::release) }
            }
                .apply { first().makeCurrent() }

            val texture = GlTexture()

            program = GlTextureProgram().apply {
                this.texture = texture
                defer(::release)
            }

            surfaceTexture = SurfaceTexture(texture.id).apply {
                setOnFrameAvailableListener(this@ImageConsumerDuplicator, handler)
                setDefaultBufferSize(bufferSize.width, bufferSize.height)
                defer(::release)
            }

            entireViewport = GlRect()
                .apply { defer(::release) }

            surface = Surface(surfaceTexture)
                .apply { defer(::release) }
        }

    override fun onFrameAvailable(surface: SurfaceTexture?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (surfaceTexture.isReleased) {
                Log.d(TAG, "Ignoring frame after release")
                return
            }
        }

        surfaceTexture.updateTexImage()
        surfaceTexture.getTransformMatrix(program.textureTransform)

        for (window in windows) {
            window.makeCurrent()
            program.draw(entireViewport)
            window.swapBuffers()
        }
    }
}
