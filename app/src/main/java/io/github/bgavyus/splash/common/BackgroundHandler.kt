package io.github.bgavyus.splash.common

import android.os.Handler
import android.os.HandlerThread
import android.util.Log

class BackgroundHandler(name: String?) : Handler(), AutoCloseable {
    companion object {
        private val TAG = BackgroundHandler::class.simpleName
    }

    private val thread = HandlerThread(name)
        .apply { start() }

    init {
        Handler(thread.looper)
    }

    override fun close() {
        Log.d(TAG, "Quiting thread: ${thread.name}")
        thread.quitSafely()
    }
}
