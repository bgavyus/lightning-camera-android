package io.github.bgavyus.lightningcamera.logging

import io.github.bgavyus.lightningcamera.extensions.callerStackTraceElement

object Logger {
    private val printer = PrinterProvider.get()

    fun log(message: String) {
        val tag = Throwable().callerStackTraceElement.fileName.substringBeforeLast(".")
        val threadName = Thread.currentThread().name
        printer.println("$tag: [$threadName] $message")
    }
}
