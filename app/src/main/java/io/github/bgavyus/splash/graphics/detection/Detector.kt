package io.github.bgavyus.splash.graphics.detection

import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.Type
import android.util.Size
import android.view.Surface
import io.github.bgavyus.splash.common.App
import io.github.bgavyus.splash.common.DeferScope
import io.github.bgavyus.splash.common.SingleThreadHandler
import io.github.bgavyus.splash.graphics.ImageConsumer
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

abstract class Detector(size: Size) : DeferScope(), ImageConsumer {
    companion object {
        private val TAG = Detector::class.simpleName

        const val CHANNELS = 3
        const val MAX_INTENSITY = 255
    }

    private val handler = SingleThreadHandler(TAG)
        .apply { defer(::close) }

    protected val rs: RenderScript = RenderScript.create(App.context)
        .apply { defer(::destroy) }

    protected val inputAllocation: Allocation = Allocation.createTyped(
        rs,
        Type.createXY(rs, Element.U8_4(rs), size.width, size.height),
        Allocation.USAGE_IO_INPUT or Allocation.USAGE_SCRIPT
    ).apply {
        defer(::destroy)
    }

    override val surface: Surface = inputAllocation.surface
    abstract val detecting: Boolean

    val detectingStates
        get() = inputAllocation.buffers
            .onEach { inputAllocation.ioReceive() }
            .map { detecting }
            .distinctUntilChanged()
            .flowOn(handler.asCoroutineDispatcher(TAG))
}
