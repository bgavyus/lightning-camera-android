package io.github.bgavyus.splash

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicHistogram
import android.util.Size
import android.view.TextureView

class LightningDetector(context: Context, textureView: TextureView, videoSize: Size) : Detector {
    private val mHistogram = IntArray(0x100)
    private val mRenderScript = RenderScript.create(context)
    private var mTextureView = textureView
    private var mInputBitmap =
        Bitmap.createBitmap(videoSize.width, videoSize.height, Bitmap.Config.ARGB_8888)
    private var mInputAllocation = Allocation.createFromBitmap(mRenderScript, mInputBitmap)
    private var mOutputAllocation =
        Allocation.createSized(mRenderScript, Element.I32(mRenderScript), mHistogram.size)
    private var mHistogramKernel =
        ScriptIntrinsicHistogram.create(mRenderScript, Element.U8_4(mRenderScript))
            .apply { setOutput(mOutputAllocation) }

    override fun detected(): Boolean {
        mTextureView.getBitmap(mInputBitmap)
        mHistogramKernel.forEach_Dot(mInputAllocation)
        mOutputAllocation.copyTo(mHistogram)
        return mHistogram.last() > 0
    }

    override fun release() {
        mHistogramKernel.destroy()
        mOutputAllocation.destroy()
        mInputAllocation.destroy()
        mRenderScript.destroy()
    }
}
