package io.github.bgavyus.lightningcamera.graphics.detection

import android.content.Context
import android.renderscript.Allocation
import android.util.Size
import io.github.bgavyus.lightningcamera.common.PeakDetector
import io.github.bgavyus.lightningcamera.extensions.android.util.area

class MotionDetector(
    context: Context,
    bufferSize: Size,
) : RenderScriptDetector(context, bufferSize) {
    private val maxRate = channels * maxIntensity * bufferSize.area.toDouble()

    private val script = ScriptC_motion(renderScript)
        .apply { defer(::destroy) }

    private val lastFrame = Allocation.createTyped(renderScript, type)
        .apply { defer(::destroy) }

    private val peakDetector = PeakDetector(
        windowSize = framesPerSecond * 10,
        deviationThreshold = 0.01,
        detectionWeight = 0.01
    )

    override fun getDetecting(frame: Allocation): Boolean {
        val ratio = script.reduce_rate(frame, lastFrame).get() / maxRate
        lastFrame.copyFrom(frame)
        return peakDetector.getDetectingAndAdd(ratio)
    }
}
