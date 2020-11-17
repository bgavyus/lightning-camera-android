package io.github.bgavyus.splash.common

import android.os.Handler
import android.os.HandlerThread

class SingleThreadHandler : Handler, AutoCloseable {
    private val deferScope = DeferScope()

    constructor(name: String?) : this(HandlerThread(name).apply { start() })

    private constructor(thread: HandlerThread) : super(thread.looper) {
        deferScope.defer(thread::quitSafely)
    }

    override fun close() = deferScope.close()
}
