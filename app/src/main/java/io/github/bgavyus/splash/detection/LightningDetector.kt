package io.github.bgavyus.splash.detection

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.RenderScript
import android.util.Size
import android.view.TextureView
import io.github.bgavyus.splash.common.ReleaseQueue

class LightningDetector(context: Context, private val textureView: TextureView, videoSize: Size) :
    Detector {

    companion object {
        private const val ZERO: Short = 0
    }

    private val releaseQueue = ReleaseQueue()
    private val renderScript = RenderScript.create(context).apply {
        releaseQueue.push(::destroy)
    }

    private val inputBitmap =
        Bitmap.createBitmap(videoSize.width, videoSize.height, Bitmap.Config.ARGB_8888)

    private val bitmapAllocation = Allocation.createFromBitmap(renderScript, inputBitmap).apply {
        releaseQueue.push(::destroy)
    }

    private val script = ScriptC_lightning(renderScript).apply {
        releaseQueue.push(::destroy)
    }

    override fun detected(): Boolean {
        textureView.getBitmap(inputBitmap)
        bitmapAllocation.syncAll(Allocation.USAGE_SCRIPT)
        return script.reduce_detected(bitmapAllocation).get() != ZERO
    }

    override fun release() {
        releaseQueue.release()
    }
}
