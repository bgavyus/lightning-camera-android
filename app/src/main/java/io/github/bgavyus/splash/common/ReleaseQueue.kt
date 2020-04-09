package io.github.bgavyus.splash.common

import android.util.Log
import java.util.*

class ReleaseQueue : ArrayDeque<() -> Unit>() {
    companion object {
        private val TAG = ReleaseQueue::class.simpleName
    }

    fun releaseAll() {
        while (!isEmpty()) {
            val callback = pop()
            Log.d(TAG, "Releasing: $callback")

            try {
                callback.invoke()
            } catch (error: Throwable) {
                Log.w(TAG, "Exception while calling release callback", error)
            }
        }
    }
}
