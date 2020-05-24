package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import android.util.Size
import io.github.bgavyus.splash.common.area
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MotionDetector private constructor(size: Size) : Detector(size) {
    companion object {
        private val TAG = MotionDetector::class.simpleName

        // TODO: Adjust threshold dynamically
        const val RATIO_THRESHOLD = 0.01

        suspend fun init(size: Size) =
            withContext(Dispatchers.IO) { MotionDetector(size) }
    }

    private val maxRate = CHANNELS * MAX_INTENSITY * size.area.toFloat()

    private val script = ScriptC_motion(rs)
        .apply { defer(::destroy) }

    private val lastFrameAllocation = Allocation.createTyped(rs, inputAllocation.type)
        .apply { defer(::destroy) }

    override val detected: Boolean
        get() {
            val ratio = script.reduce_rate(inputAllocation, lastFrameAllocation).get() / maxRate
            lastFrameAllocation.copyFrom(inputAllocation)
            // Log.v(TAG, "Ratio: $ratio")
            return ratio > RATIO_THRESHOLD
        }
}
