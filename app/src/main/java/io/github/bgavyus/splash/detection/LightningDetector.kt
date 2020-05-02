package io.github.bgavyus.splash.detection

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.renderscript.Allocation
import android.renderscript.RenderScript
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack

class LightningDetector(inputBitmap: Bitmap, listener: DetectionListener) :
    Detector(listener) {

    companion object {
        private val TAG = LightningDetector::class.simpleName

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

    private val thread = HandlerThread(TAG).apply {
        start()
        closeStack.push { quitSafely() }
    }

    private val handler = Handler(thread.looper)

    override fun detected(): Boolean {
        inputAllocation.syncAll(Allocation.USAGE_SCRIPT)
        return script.reduce_detected(inputAllocation).get() != ZERO
    }

    override fun detect() {
        handler.post {
            super.detect()
        }
    }

    override fun close() {
        closeStack.close()
    }
}
