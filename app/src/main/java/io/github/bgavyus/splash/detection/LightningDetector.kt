package io.github.bgavyus.splash.detection

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.ReleaseStack

class LightningDetector(inputBitmap: Bitmap, listener: DetectionListener) :
    Detector(listener) {

    companion object {
        private const val ZERO: Short = 0
    }

    private val releaseStack = ReleaseStack()
    private val rs = RenderScript.create(App.context).apply {
        releaseStack.push(::destroy)
    }

    private val script = ScriptC_lightning(rs).apply {
        releaseStack.push(::destroy)
    }

    private val inputAllocation = Allocation.createFromBitmap(rs, inputBitmap).apply {
        releaseStack.push(::destroy)
    }

    fun process() {
        inputAllocation.syncAll(Allocation.USAGE_SCRIPT)
        val detected = script.reduce_detected(inputAllocation).get() != ZERO
        propagate(detected)
    }

    fun release() {
        releaseStack.release()
    }
}
