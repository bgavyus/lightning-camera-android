package io.github.bgavyus.lightningcamera.detection

import android.content.Context
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import android.view.Surface
import com.google.auto.factory.AutoFactory
import com.google.auto.factory.Provided
import io.github.bgavyus.lightningcamera.extensions.android.renderscript.buffers
import io.github.bgavyus.lightningcamera.extensions.android.util.area
import io.github.bgavyus.lightningcamera.utilities.DeferScope
import io.github.bgavyus.lightningcamera.utilities.PeakDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@AutoFactory
class MotionDetector(
    @Provided context: Context,
    bufferSize: Size,
) : DeferScope() {
    companion object {
        const val channels = 3
        const val maxIntensity = 255
        const val framesPerSecond = 30
    }

    private val renderScript: RenderScript = RenderScript.create(context)
        .apply { defer(::destroy) }

    private val type: Type = Type.createXY(
        renderScript,
        Element.U8_4(renderScript),
        bufferSize.width,
        bufferSize.height,
    )

    private val input = Allocation.createTyped(
        renderScript,
        type,
        Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT,
    )
        .apply { defer(::destroy) }

    val surface: Surface = input.surface

    private val maxRate = channels * maxIntensity * bufferSize.area.toDouble()

    private val script = ScriptC_motion(renderScript)
        .apply { defer(::destroy) }

    private val lastFrame = Allocation.createTyped(renderScript, type)
        .apply { defer(::destroy) }

    private val peakDetector = PeakDetector(
        windowSize = framesPerSecond * 10,
        deviationThreshold = 0.01,
        detectionWeight = 0.01
    )

    private fun detecting(frame: Allocation): Boolean {
        val ratio = script.reduce_rate(frame, lastFrame).get() / maxRate
        lastFrame.copyFrom(frame)
        return peakDetector.getDetectingAndAdd(ratio)
    }

    fun detectingStates() = input.buffers()
        .onEach { it.ioReceive() }
        .map(::detecting)
        .distinctUntilChanged()
        .flowOn(Dispatchers.IO)
}
