package io.github.bgavyus.lightningcamera.detection

import android.content.Context
import android.os.Handler
import android.util.Size
import com.google.android.gms.tflite.java.TfLite
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MotionDetectorFactory @Inject constructor(
    private val handler: Handler,
    private val context: Context,
) : DeferScope() {
    suspend fun create(
        bufferSize: Size,
        frameRate: FrameRate,
    ) = withContext(handler.asCoroutineDispatcher()) {
        TfLite.initialize(context).await()
        MotionDetector(handler, context, bufferSize, frameRate)
    }
}
