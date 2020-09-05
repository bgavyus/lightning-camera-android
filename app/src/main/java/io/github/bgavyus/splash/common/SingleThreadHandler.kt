package io.github.bgavyus.splash.common

import android.os.Handler
import android.os.HandlerThread
import io.github.bgavyus.splash.BuildConfig
import kotlin.time.TimeSource
import kotlin.time.seconds

class SingleThreadHandler : Handler, AutoCloseable {
    private val deferScope = DeferScope()

    constructor(name: String?) : this(HandlerThread(name).apply { start() })

    private constructor(thread: HandlerThread) : super(thread.looper) {
        thread.apply {
            deferScope.defer {
                Logger.debug("Quiting $name thread")
                quitSafely()
            }
        }

        if (BuildConfig.DEBUG) {
            monitorDispatchRate()
        }
    }

    private fun monitorDispatchRate() {
        val period = 15.seconds
        var mark = TimeSource.Monotonic.markNow()
        var messages = 0

        looper.setMessageLogging {
            val elapsedTime = mark.elapsedNow()
            messages++

            if (elapsedTime < period) {
                return@setMessageLogging
            }

            val dispatches = messages / 2
            val dispatchesPerSeconds = dispatches / elapsedTime.inSeconds
            Logger.verbose("Dispatch rate for thread ${looper.thread.name}: $dispatchesPerSeconds")
            mark += elapsedTime
            messages = 0
        }
    }

    override fun close() = deferScope.close()
}
