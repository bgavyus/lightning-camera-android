package io.github.bgavyus.splash.graphics

import android.graphics.Matrix
import android.graphics.SurfaceTexture
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.Rotation
import io.github.bgavyus.splash.common.extensions.reflectTo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class TextureHolder(surfaceTexture: SurfaceTexture) : DeferScope() {
    private val scope = CoroutineScope(Dispatchers.Default)
        .apply { defer(::cancel) }

    val viewSize = MutableStateFlow(Size(1, 1))
    val bufferSize = MutableStateFlow(Size(1, 1))
    val rotation = MutableStateFlow(Rotation.Natural)
    val transformMatrix = MutableStateFlow(Matrix())

    val surface = Surface(surfaceTexture)

    init {
        combine(viewSize, bufferSize, rotation) { viewSize, bufferSize, rotation ->
            TransformMatrixFactory.create(viewSize, bufferSize, rotation)
        }
            .reflectTo(transformMatrix)
            .launchIn(scope)
    }
}
