package io.github.bgavyus.lightningcamera.utilities

import android.os.Handler
import android.os.HandlerThread

class SingleThreadHandler private constructor(
    thread: HandlerThread,
) : Handler(thread.looper), AutoCloseable {
    constructor(name: String) : this(HandlerThread(name).apply { start() })

    private val deferScope = DeferScope()
        .also { it.defer(thread::quitSafely) }

    override fun close() = deferScope.close()
}
