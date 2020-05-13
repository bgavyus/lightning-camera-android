package io.github.bgavyus.splash.detection

import android.graphics.Bitmap
import android.renderscript.Allocation
import android.util.Log

class MotionDetector(inputBitmap: Bitmap, listener: DetectionListener) :
    RenderScriptDetector(inputBitmap, listener) {
    companion object {
        // TODO: Adjust threshold dynamically
        const val THRESHOLD = 0.1
    }

    private val maxRate = inputBitmap.width.toFloat() * inputBitmap.height * 3 * 255

    private val script = ScriptC_motion(rs).apply {
        closeStack.push(::destroy)
    }

    private val lastFrame = Bitmap.createBitmap(inputBitmap)
    private val lastFrameAllocation = Allocation.createFromBitmap(rs, lastFrame).apply {
        closeStack.push(::destroy)
    }

    override fun rsDetected(): Boolean {
        val ratio = script.reduce_rate(inputAllocation, lastFrameAllocation).get() / maxRate
        lastFrameAllocation.copyFrom(inputAllocation)
        return ratio > THRESHOLD
    }
}
