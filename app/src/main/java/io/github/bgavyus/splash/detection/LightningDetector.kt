package io.github.bgavyus.splash.detection

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack

class LightningDetector(inputBitmap: Bitmap, listener: DetectionListener) :
    Detector(listener) {

    companion object {
        private const val ZERO: Short = 0
    }

    private val closeStack = CloseStack()
    private val rs = RenderScript.create(App.context).apply {
        closeStack.push(::destroy)
    }

    private val script = ScriptC_lightning(rs).apply {
        closeStack.push(::destroy)
    }

    private val inputAllocation = Allocation.createFromBitmap(rs, inputBitmap).apply {
        closeStack.push(::destroy)
    }

    override fun detected(): Boolean {
        inputAllocation.syncAll(Allocation.USAGE_SCRIPT)
        return script.reduce_detected(inputAllocation).get() != ZERO
    }

    override fun close() {
        closeStack.close()
    }
}
