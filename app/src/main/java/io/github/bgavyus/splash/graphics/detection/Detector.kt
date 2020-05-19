package io.github.bgavyus.splash.graphics.detection

import android.renderscript.*
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.BackgroundHandler
import io.github.bgavyus.splash.common.CloseStack
import io.github.bgavyus.splash.graphics.ImageConsumer

abstract class Detector(size: Size, private val listener: DetectionListener) : ImageConsumer,
    Allocation.OnBufferAvailableListener {
    companion object {
        private val TAG = Detector::class.simpleName

        const val CHANNELS = 3
        const val MAX_INTENSITY = 255
    }

    internal val closeStack = CloseStack()

    private val handler = BackgroundHandler(TAG)
        .also(closeStack::push)

    internal val rs = RenderScript.create(App.context)
        .also(closeStack::push)

    internal val inputAllocation = Allocation.createTyped(
        rs,
        Type.createXY(rs, Element.U8_4(rs), size.width, size.height),
        Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
    )
        .also(closeStack::push)
        .apply { setOnBufferAvailableListener(this@Detector) }

    override val surface: Surface = inputAllocation.surface

    private var lastDetected = false

    override fun onBufferAvailable(a: Allocation?) {
        handler.post {
            inputAllocation.ioReceive()
            propagate(detected)
        }
    }

    abstract val detected: Boolean

    private fun propagate(detected: Boolean) {
        if (detected == lastDetected) {
            return
        }

        lastDetected = detected

        if (detected) {
            listener.onDetectionStarted()
        } else {
            listener.onDetectionEnded()
        }
    }

    override fun close() = closeStack.close()
}
