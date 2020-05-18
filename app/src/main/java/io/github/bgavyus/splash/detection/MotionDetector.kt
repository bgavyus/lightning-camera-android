package io.github.bgavyus.splash.detection

import android.renderscript.Allocation
import android.util.Size
import io.github.bgavyus.splash.common.area

class MotionDetector(size: Size, listener: DetectionListener) :
    Detector(size, listener) {
    companion object {
        private val TAG = MotionDetector::class.simpleName

        // TODO: Adjust threshold dynamically
        const val RATIO_THRESHOLD = 0.01
    }

    private val maxRate = CHANNELS * MAX_INTENSITY * size.area.toFloat()

    private val script = ScriptC_motion(rs)
        .also(closeStack::push)

    private val lastFrameAllocation = Allocation.createTyped(rs, inputAllocation.type)
        .also(closeStack::push)

    override val detected: Boolean
        get() {
            val ratio = script.reduce_rate(inputAllocation, lastFrameAllocation).get() / maxRate
            lastFrameAllocation.copyFrom(inputAllocation)
            // Log.v(TAG, "Ratio: $ratio")
            return ratio > RATIO_THRESHOLD
        }
}
