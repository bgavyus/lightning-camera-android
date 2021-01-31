package io.github.bgavyus.lightningcamera.graphics.detection

import android.content.Context
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import android.view.Surface
import io.github.bgavyus.lightningcamera.common.DeferScope
import io.github.bgavyus.lightningcamera.common.SingleThreadHandler
import io.github.bgavyus.lightningcamera.extensions.android.renderscript.buffers
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

abstract class RenderScriptDetector(
    context: Context,
    bufferSize: Size,
) : DeferScope() {
    companion object {
        const val channels = 3
        const val maxIntensity = 255
        const val framesPerSecond = 30
    }

    private val handler = SingleThreadHandler(javaClass.simpleName)
        .apply { defer(::close) }

    protected val renderScript: RenderScript = RenderScript.create(context)
        .apply { defer(::destroy) }

    protected val type: Type = Type.createXY(
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

    protected abstract fun getDetecting(frame: Allocation): Boolean

    fun detectingStates() = input.buffers()
        .onEach { it.ioReceive() }
        .map(::getDetecting)
        .distinctUntilChanged()
        .flowOn(handler.asCoroutineDispatcher(javaClass.simpleName))
}
