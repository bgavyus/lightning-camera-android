package io.github.bgavyus.splash.graphics.detection

import android.renderscript.RenderScript
import android.util.Size

class LightningDetector(
    renderScript: RenderScript,
    bufferSize: Size
) : Detector(renderScript, bufferSize) {
    companion object {
        private val TAG = LightningDetector::class.simpleName
    }

    private val script = ScriptC_lightning(renderScript)
        .apply { defer(::destroy) }

    override fun detecting(): Boolean {
        val intensity = script.reduce_intensity(inputAllocation).get()
        // Log.v(TAG, "Value: ${intensity.x} ${intensity.y} ${intensity.z}")
        return (intensity.x + intensity.y + intensity.z) == CHANNELS * MAX_INTENSITY
    }
}
