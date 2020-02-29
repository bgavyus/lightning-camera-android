package io.github.bgavyus.splash

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicHistogram
import android.util.Size
import android.view.TextureView

class LightningDetector(context: Context, private var textureView: TextureView, videoSize: Size) :
    Detector {
    private val histogram = IntArray(0x100)
    private val renderScript = RenderScript.create(context)
    private var inputBitmap =
        Bitmap.createBitmap(videoSize.width, videoSize.height, Bitmap.Config.ARGB_8888)
    private var inputAllocation = Allocation.createFromBitmap(renderScript, inputBitmap)
    private var outputAllocation =
        Allocation.createSized(renderScript, Element.I32(renderScript), histogram.size)
    private var histogramKernel =
        ScriptIntrinsicHistogram.create(renderScript, Element.U8_4(renderScript))
            .apply { setOutput(outputAllocation) }

    override fun detected(): Boolean {
        textureView.getBitmap(inputBitmap)
        histogramKernel.forEach_Dot(inputAllocation)
        outputAllocation.copyTo(histogram)
        return histogram.last() > 0
    }

    override fun release() {
        histogramKernel.destroy()
        outputAllocation.destroy()
        inputAllocation.destroy()
        renderScript.destroy()
    }
}
