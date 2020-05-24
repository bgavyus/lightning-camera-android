package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Log
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.Deferrer
import io.github.bgavyus.splash.graphics.ImageConsumer

abstract class Detector(size: Size) : Deferrer(), ImageConsumer,
    Allocation.OnBufferAvailableListener {
    companion object {
        private val TAG = Detector::class.simpleName

        const val CHANNELS = 3
        const val MAX_INTENSITY = 255
    }

    protected val rs: RenderScript = RenderScript.create(App.context)
        .apply { defer(::destroy) }

    protected val inputAllocation: Allocation = Allocation.createTyped(
        rs,
        Type.createXY(rs, Element.U8_4(rs), size.width, size.height),
        Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
    ).apply {
        defer(::destroy)
        setOnBufferAvailableListener(this@Detector)
    }

    var listener: DetectionListener? = null
    override val surface: Surface = inputAllocation.surface
    private var lastDetected = false
    abstract val detected: Boolean

    override fun onBufferAvailable(a: Allocation?) {
        try {
            inputAllocation.ioReceive()
            propagate(detected)
        } catch (_: NullPointerException) {
            Log.d(TAG, "Ignoring frame after release")
        }
    }

    private fun propagate(detected: Boolean) {
        if (detected == lastDetected) {
            return
        }

        lastDetected = detected

        if (detected) {
            Log.i(TAG, "Detection Started")
            listener?.onDetectionStarted()
        } else {
            Log.i(TAG, "Detection Ended")
            listener?.onDetectionEnded()
        }
    }
}