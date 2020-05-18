package io.github.bgavyus.splash.common

import android.renderscript.BaseObj
import android.renderscript.RenderScript
import android.util.Log
import android.view.Surface
import java.util.*

class CloseStack : ArrayDeque<() -> Unit>(), AutoCloseable {
    companion object {
        private val TAG = CloseStack::class.simpleName
    }

    override fun close() {
        while (!isEmpty()) {
            val callback = pop()
            Log.v(TAG, "Closing: $callback")

            try {
                callback.invoke()
            } catch (error: Throwable) {
                Log.w(TAG, "Error while closing", error)
            }
        }
    }

    fun push(closeable: AutoCloseable) = push(closeable::close)
    fun push(surface: Surface) = push(surface::release)
    fun push(renderScript: RenderScript) = push(renderScript::destroy)
    fun push(renderScriptObject: BaseObj) = push(renderScriptObject::destroy)
}
