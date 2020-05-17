package io.github.bgavyus.splash.detection

import android.util.Size

class LightningDetector(size: Size, listener: DetectionListener) :
    Detector(size, listener) {
    companion object {
        private val TAG = LightningDetector::class.simpleName
    }

    private val script = ScriptC_lightning(rs).apply {
        closeStack.push(::destroy)
    }

    override val detected: Boolean
        get() {
            val intensity = script.reduce_intensity(inputAllocation).get()
            // Log.v(TAG, "Value: ${intensity.x} ${intensity.y} ${intensity.z}")
            return (intensity.x + intensity.y + intensity.z) == CHANNELS * MAX_INTENSITY
        }
}
