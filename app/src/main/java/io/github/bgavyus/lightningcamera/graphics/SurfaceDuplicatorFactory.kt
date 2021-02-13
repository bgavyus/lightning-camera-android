package io.github.bgavyus.lightningcamera.graphics

import android.os.Handler
import android.util.Size
import android.view.Surface
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SurfaceDuplicatorFactory @Inject constructor(
    private val handler: Handler,
) {
    suspend fun create(
        bufferSize: Size,
        surfaces: Iterable<Surface>,
    ) = withContext(handler.asCoroutineDispatcher()) {
        SurfaceDuplicator(handler, bufferSize, surfaces)
    }
}
