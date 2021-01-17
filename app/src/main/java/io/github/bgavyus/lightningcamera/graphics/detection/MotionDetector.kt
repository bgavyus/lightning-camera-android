package io.github.bgavyus.lightningcamera.graphics.detection

import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Size
import io.github.bgavyus.lightningcamera.common.PeakDetector
import io.github.bgavyus.lightningcamera.extensions.area

class MotionDetector(
    renderScript: RenderScript,
    bufferSize: Size
) : Detector(renderScript, bufferSize) {
    private val maxRate = channels * maxIntensity * bufferSize.area.toDouble()

    private val script = ScriptC_motion(renderScript)
        .apply { defer(::destroy) }

    private val lastFrameAllocation = Allocation.createTyped(renderScript, inputAllocation.type)
        .apply { defer(::destroy) }

    private val peakDetector = PeakDetector(
        windowSize = framesPerSeconds * 10,
        deviationThreshold = 0.01,
        detectionWeight = 0.01
    )

    override fun detecting(): Boolean {
        val ratio = script.reduce_rate(inputAllocation, lastFrameAllocation).get() / maxRate
        lastFrameAllocation.copyFrom(inputAllocation)
        return peakDetector.getDetectingAndAdd(ratio)
    }
}
