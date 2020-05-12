package io.github.bgavyus.splash.detection

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.renderscript.Allocation
import android.renderscript.RenderScript
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack

class LightningDetector(inputBitmap: Bitmap, listener: DetectionListener) :
    RenderScriptDetector(inputBitmap, listener) {
    companion object {
        private const val ZERO: Short = 0
    }

    private val script = ScriptC_lightning(rs).apply {
        closeStack.push(::destroy)
    }

    override fun rsDetected(): Boolean {
        return script.reduce_detected(inputAllocation).get() != ZERO
    }
}
