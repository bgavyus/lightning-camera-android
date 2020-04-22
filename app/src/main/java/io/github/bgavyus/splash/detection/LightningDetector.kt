package io.github.bgavyus.splash.detection

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import io.github.bgavyus.splash.App
import io.github.bgavyus.splash.common.ReleaseStack

class LightningDetector(val bitmap: Bitmap, listener: DetectionListener) :
    Detector(listener) {

    companion object {
        private const val ZERO: Short = 0
    }

    private val releaseStack = ReleaseStack()
    private val rs = RenderScript.create(App.shared).apply {
        releaseStack.push(::destroy)
    }

    private val script = ScriptC_lightning(rs).apply {
        releaseStack.push(::destroy)
    }

    private val inputAllocation = Allocation.createFromBitmap(rs, bitmap).apply {
        releaseStack.push(::destroy)
    }

    fun process() {
        inputAllocation.syncAll(Allocation.USAGE_SCRIPT)
        propagate(script.reduce_detected(inputAllocation).get() != ZERO)
    }

    fun release() {
        releaseStack.release()
    }
}
