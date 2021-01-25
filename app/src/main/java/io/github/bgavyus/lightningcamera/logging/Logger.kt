package io.github.bgavyus.lightningcamera.logging

import io.github.bgavyus.lightningcamera.BuildConfig

object Logger {
    private val messageLogger = if (BuildConfig.DEBUG) LocalLogger() else RemoteLogger()
    private val tagExtractor = LoggerTagExtractor()

    fun log(message: String) {
        val threadName = Thread.currentThread().name
        val callerStackTraceElement = Throwable().stackTrace[1]
        val tag = tagExtractor.extract(callerStackTraceElement)
        messageLogger.log("[$threadName] $tag: $message")
    }
}
