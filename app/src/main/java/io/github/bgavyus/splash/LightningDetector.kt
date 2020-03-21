package io.github.bgavyus.splash

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicHistogram
import android.util.Size
import android.view.TextureView

class LightningDetector(context: Context, private val textureView: TextureView, videoSize: Size) :
    Detector {
    private val histogram = IntArray(0x100)
    private val renderScript = RenderScript.create(context)
    private val inputBitmap =
        Bitmap.createBitmap(videoSize.width, videoSize.height, Bitmap.Config.ARGB_8888)
    private val bitmapAllocation = Allocation.createFromBitmap(renderScript, inputBitmap)
    private val histogramAllocation =
        Allocation.createSized(renderScript, Element.I32(renderScript), histogram.size)
    private val histogramKernel =
        ScriptIntrinsicHistogram.create(renderScript, Element.U8_4(renderScript))
            .apply { setOutput(histogramAllocation) }

    override fun detected(): Boolean {
        textureView.getBitmap(inputBitmap)
        bitmapAllocation.syncAll(Allocation.USAGE_SCRIPT)
        histogramKernel.forEach_Dot(bitmapAllocation)
        histogramAllocation.copyTo(histogram)
        return histogram.last() > 0
    }

    override fun release() {
        histogramKernel.destroy()
        histogramAllocation.destroy()
        bitmapAllocation.destroy()
        renderScript.destroy()
    }
}
