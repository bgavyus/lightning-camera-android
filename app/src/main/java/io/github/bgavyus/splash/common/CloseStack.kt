package io.github.bgavyus.splash.common

import android.util.Log
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
}
