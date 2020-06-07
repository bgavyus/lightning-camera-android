package io.github.bgavyus.splash.common

import android.os.Handler
import android.os.HandlerThread
import android.util.Log

class SingleThreadHandler : Handler, AutoCloseable {
    companion object {
        private val TAG = SingleThreadHandler::class.simpleName
    }

    private val deferScope = DeferScope()

    constructor(name: String?) : this(HandlerThread(name).apply { start() })

    private constructor(thread: HandlerThread) : super(thread.looper) {
        thread.apply {
            deferScope.defer {
                Log.d(TAG, "Quiting $name")
                quitSafely()
            }
        }
    }

    override fun close() = deferScope.close()
}
