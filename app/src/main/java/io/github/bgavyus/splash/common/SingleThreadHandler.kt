package io.github.bgavyus.splash.common

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import io.github.bgavyus.splash.BuildConfig
import kotlin.time.TimeSource
import kotlin.time.seconds

class SingleThreadHandler : Handler, AutoCloseable {
    companion object {
        private val TAG = SingleThreadHandler::class.simpleName
    }

    private val deferScope = DeferScope()

    constructor(name: String?) : this(HandlerThread(name).apply { start() })

    private constructor(thread: HandlerThread) : super(thread.looper) {
        thread.apply {
            deferScope.defer {
                Log.d(TAG, "Quiting thread $name")
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
            Log.v(TAG, "Dispatch rate for thread ${looper.thread.name}: $dispatchesPerSeconds")
            mark += elapsedTime
            messages = 0
        }
    }

    override fun close() = deferScope.close()
}
