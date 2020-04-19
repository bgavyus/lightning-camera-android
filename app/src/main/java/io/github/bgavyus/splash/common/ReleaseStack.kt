package io.github.bgavyus.splash.common

import android.util.Log
import java.util.*

class ReleaseStack : ArrayDeque<() -> Unit>() {
    companion object {
        private val TAG = ReleaseStack::class.simpleName
    }

    fun release() {
        while (!isEmpty()) {
            val callback = pop()
            Log.v(TAG, "Releasing: $callback")

            try {
                callback.invoke()
            } catch (error: Throwable) {
                Log.w(TAG, "Exception while calling release callback", error)
            }
        }
    }
}
