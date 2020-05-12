package io.github.bgavyus.splash.detection

import android.graphics.Bitmap
import android.os.Handler
import android.os.HandlerThread
import android.renderscript.Allocation
import android.renderscript.RenderScript
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.CloseStack

abstract class RenderScriptDetector(inputBitmap: Bitmap, listener: DetectionListener) : Detector(listener) {
    companion object {
        private val TAG = RenderScriptDetector::class.simpleName
    }

    internal val closeStack = CloseStack()
    internal val rs = RenderScript.create(App.context).apply {
        closeStack.push(::destroy)
    }

    internal val inputAllocation = Allocation.createFromBitmap(rs, inputBitmap).apply {
        closeStack.push(::destroy)
    }

    private val thread = HandlerThread(TAG).apply {
        start()
        closeStack.push { quitSafely() }
    }

    private val handler = Handler(thread.looper)

    override fun detected(): Boolean {
        inputAllocation.syncAll(Allocation.USAGE_SCRIPT)
        return rsDetected()
    }

    abstract fun rsDetected(): Boolean

    override fun detect() {
        handler.post {
            super.detect()
        }
    }

    override fun close() = closeStack.close()
}
