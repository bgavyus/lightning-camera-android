package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import android.util.Size
import io.github.bgavyus.splash.common.area
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.absoluteValue

class MotionDetector private constructor(size: Size) : Detector(size) {
    companion object {
        private val TAG = MotionDetector::class.simpleName

        suspend fun init(size: Size) =
            withContext(Dispatchers.IO) { MotionDetector(size) }
    }

    private val maxRate = CHANNELS * MAX_INTENSITY * size.area.toFloat()

    private val script = ScriptC_motion(rs)
        .apply { defer(::destroy) }

    private val lastFrameAllocation = Allocation.createTyped(rs, inputAllocation.type)
        .apply { defer(::destroy) }

    private var lastRatio = 0f

    // TODO: Improve detection
    override val detecting: Boolean
        get() {
            val ratio = script.reduce_rate(inputAllocation, lastFrameAllocation).get() / maxRate
            val diff = ratio - lastRatio
            lastFrameAllocation.copyFrom(inputAllocation)
            lastRatio = ratio
            // Log.v(TAG, "Ratio: $ratio")
            return diff.absoluteValue > 0.001
        }
}
