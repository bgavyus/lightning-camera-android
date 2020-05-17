package io.github.bgavyus.splash.detection

import android.os.Handler
import android.os.HandlerThread
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Log
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack

abstract class Detector(size: Size, private val listener: DetectionListener) : AutoCloseable,
    Allocation.OnBufferAvailableListener {
    companion object {
        private val TAG = Detector::class.simpleName

        const val CHANNELS = 3
        const val MAX_INTENSITY = 255
    }

    internal val closeStack = CloseStack()

    // TODO: Create common HandlerThread helper
    private val thread = HandlerThread(TAG).apply {
        start()

        closeStack.push {
            Log.d(TAG, "Quiting detector thread")
            quitSafely()
        }
    }

    private val handler = Handler(thread.looper)

    internal val rs = RenderScript.create(App.context).apply {
        closeStack.push(::destroy)
    }

    internal val inputAllocation = Allocation.createTyped(
        rs,
        Type.createXY(rs, Element.U8_4(rs), size.width, size.height),
        Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
    ).apply {
        closeStack.push(::destroy)
        setOnBufferAvailableListener(this@Detector)
    }

    val surface: Surface = inputAllocation.surface

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
