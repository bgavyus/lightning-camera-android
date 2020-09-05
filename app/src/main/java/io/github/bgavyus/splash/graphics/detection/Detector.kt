package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import io.github.bgavyus.splash.graphics.ImageConsumer
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

abstract class Detector(
    renderScript: RenderScript,
    bufferSize: Size
) : DeferScope(), ImageConsumer {
    companion object {
        const val CHANNELS = 3
        const val MAX_INTENSITY = 255
    }

    private val handler = SingleThreadHandler(Detector::class.simpleName)
        .apply { defer(::close) }

    protected val inputAllocation: Allocation = Allocation.createTyped(
        renderScript,
        Type.createXY(
            renderScript,
            Element.U8_4(renderScript),
            bufferSize.width,
            bufferSize.height
        ),
        Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
    )
        .apply { defer(::destroy) }

    override val surface: Surface = inputAllocation.surface
    abstract fun detecting(): Boolean

    fun detectingStates() = inputAllocation.buffers()
        .map { detecting() }
        .distinctUntilChanged()
        .flowOn(handler.asCoroutineDispatcher(Detector::class.simpleName))
}

private fun Allocation.buffers() = callbackFlow {
    setOnBufferAvailableListener { sendBlocking(ioReceive()) }
    awaitClose { setOnBufferAvailableListener(null) }
}
