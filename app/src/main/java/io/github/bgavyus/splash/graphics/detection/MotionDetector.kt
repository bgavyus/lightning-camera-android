package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Size
import io.github.bgavyus.splash.common.extensions.area
import kotlin.math.absoluteValue

class MotionDetector(
    renderScript: RenderScript,
    bufferSize: Size
) : Detector(renderScript, bufferSize) {
    companion object {
        private val TAG = MotionDetector::class.simpleName
    }

    private val maxRate = CHANNELS * MAX_INTENSITY * bufferSize.area.toFloat()

    private val script = ScriptC_motion(renderScript)
        .apply { defer(::destroy) }

    private val lastFrameAllocation = Allocation.createTyped(renderScript, inputAllocation.type)
        .apply { defer(::destroy) }

    private var lastRatio = 0f

    // TODO: Improve detection
    override fun detecting(): Boolean {
        val ratio = script.reduce_rate(inputAllocation, lastFrameAllocation).get() / maxRate
        val diff = ratio - lastRatio
        lastFrameAllocation.copyFrom(inputAllocation)
        lastRatio = ratio
        // Log.v(TAG, "Ratio: $ratio")
        return diff.absoluteValue > 0.001
    }
}
