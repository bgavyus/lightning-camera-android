package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Size
import io.github.bgavyus.splash.common.PeakDetector
import io.github.bgavyus.splash.common.extensions.area

class MotionDetector(
    renderScript: RenderScript,
    bufferSize: Size
) : Detector(renderScript, bufferSize) {
    private val maxRate = CHANNELS * MAX_INTENSITY * bufferSize.area.toDouble()

    private val script = ScriptC_motion(renderScript)
        .apply { defer(::destroy) }

    private val lastFrameAllocation = Allocation.createTyped(renderScript, inputAllocation.type)
        .apply { defer(::destroy) }

    private val peakDetector = PeakDetector(
        windowSize = FRAMES_PER_SECONDS * 10,
        deviationThreshold = 0.01,
        detectionWeight = 0.01
    )

    override fun detecting(): Boolean {
        val ratio = script.reduce_rate(inputAllocation, lastFrameAllocation).get() / maxRate
        lastFrameAllocation.copyFrom(inputAllocation)
        return peakDetector.getDetectingAndAdd(ratio)
    }
}
