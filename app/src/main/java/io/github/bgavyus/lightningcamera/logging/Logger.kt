package io.github.bgavyus.lightningcamera.logging

import io.github.bgavyus.lightningcamera.extensions.callerStackTraceElement

object Logger {
    private val tagExtractor = LoggerTagExtractor()
    private val printer = PrinterProvider.get()

    fun log(message: String) {
        val threadName = Thread.currentThread().name
        val className = Throwable().callerStackTraceElement.className
        val tag = tagExtractor.extract(className)
        printer.println("$tag: [$threadName] $message")
    }
}
