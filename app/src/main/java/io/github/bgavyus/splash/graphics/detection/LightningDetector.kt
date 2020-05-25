package io.github.bgavyus.splash.graphics.detection

import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class LightningDetector private constructor(size: Size) : Detector(size) {
    companion object {
        private val TAG = LightningDetector::class.simpleName

        suspend fun init(size: Size) =
            withContext(Dispatchers.IO) { LightningDetector(size) }
    }

    private val script = ScriptC_lightning(rs)
        .apply { defer(::destroy) }

    override val detecting: Boolean
        get() {
            val intensity = script.reduce_intensity(inputAllocation).get()
            // Log.v(TAG, "Value: ${intensity.x} ${intensity.y} ${intensity.z}")
            return (intensity.x + intensity.y + intensity.z) == CHANNELS * MAX_INTENSITY
        }
}
