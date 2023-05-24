package io.github.bgavyus.lightningcamera.detection

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.PixelFormat
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.util.Size
import android.view.Surface
import io.github.bgavyus.lightningcamera.extensions.android.graphics.load
import io.github.bgavyus.lightningcamera.extensions.android.media.firstPlainBuffer
import io.github.bgavyus.lightningcamera.extensions.android.media.images
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.logging.Logger
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.FrameRate
import io.github.bgavyus.lightningcamera.utilities.PeakDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import org.tensorflow.lite.InterpreterApi
import org.tensorflow.lite.support.common.FileUtil
import java.nio.ByteBuffer

class MotionDetector(
    private val handler: Handler,
    context: Context,
    runtime: Runtime,
    bufferSize: Size,
    frameRate: FrameRate,
) : DeferScope() {
    @SuppressLint("WrongConstant")
    private val imageReader = ImageReader.newInstance(
        bufferSize.width,
        bufferSize.height,
        PixelFormat.RGBA_8888,
        3
    )
        .also { defer(it::close) }

    val surface: Surface get() = imageReader.surface

    private val interpreter = InterpreterApi.create(
        FileUtil.loadMappedFile(context, "motion.tflite"),
        InterpreterApi.Options()
            .setRuntime(InterpreterApi.Options.TfLiteRuntime.FROM_SYSTEM_ONLY)
            .setNumThreads(
                runtime.availableProcessors().also { Logger.log("Available Processors: $it") }
            )
    )
        .also { defer(it::close) }
        .apply {
            val pixelFormat = PixelFormat().apply { load(imageReader.imageFormat) }
            val shape = intArrayOf(pixelFormat.bytesPerPixel * bufferSize.area)
            repeat(inputTensorCount) { resizeInput(it, shape) }
        }

    private var lastImageOrNull: Image? = null

    private val inputsOrNulls = arrayOfNulls<ByteBuffer>(2)

    private val outputFloatBuffer = ByteBuffer.allocateDirect(Float.SIZE_BYTES).asFloatBuffer()

    private val outputs = mapOf(0 to outputFloatBuffer)

    private val maxRate = bufferSize.area.toDouble()

    private val peakDetector = PeakDetector(
        windowSize = frameRate.fps * 10,
        deviationThreshold = 0.02,
        detectionWeight = 0.01
    )

    fun detectingStates() = imageReader.images(handler)
        .map(::detecting)
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)

    private fun detecting(imageReader: ImageReader): Boolean {
        val image = imageReader.acquireLatestImage() ?: return false
        val lastImage = lastImageOrNull ?: run {
            lastImageOrNull = image
            return false
        }
        inputsOrNulls[0] = image.firstPlainBuffer
        inputsOrNulls[1] = lastImage.firstPlainBuffer
        val inputs = inputsOrNulls.requireNoNulls()
        outputFloatBuffer.rewind()
        interpreter.runForMultipleInputsOutputs(inputs, outputs)
        lastImage.close()
        lastImageOrNull = image
        val ratio = outputFloatBuffer[0] / maxRate
        return peakDetector.getDetectingAndAdd(ratio)
    }
}
