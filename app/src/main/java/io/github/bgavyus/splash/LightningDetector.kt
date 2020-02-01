package io.github.bgavyus.splash

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicHistogram
import android.util.Size
import android.view.TextureView

class LightningDetector(context: Context, textureView: TextureView, videoSize: Size) {
    private val mHistogram = IntArray(0x100)
    private val renderScript = RenderScript.create(context)
    private var mTextureView = textureView
    private var mInputBitmap =
        Bitmap.createBitmap(videoSize.width, videoSize.height, Bitmap.Config.ARGB_8888, false)
    private var mInputAllocation = Allocation.createFromBitmap(renderScript, mInputBitmap)
    private var mOutputAllocation =
        Allocation.createSized(renderScript, Element.I32(renderScript), mHistogram.size)
    private var mHistogramKernel =
        ScriptIntrinsicHistogram.create(renderScript, Element.U8_4(renderScript))
            .apply { setOutput(mOutputAllocation) }

    fun hasLightning(): Boolean {
        mTextureView.getBitmap(mInputBitmap)
        mHistogramKernel.forEach_Dot(mInputAllocation)
        mOutputAllocation.copyTo(mHistogram)
        return mHistogram.last() > 0
    }

    fun release() {
        mHistogramKernel.destroy()
        mOutputAllocation.destroy()
        mInputAllocation.destroy()
        renderScript.destroy()
    }
}
