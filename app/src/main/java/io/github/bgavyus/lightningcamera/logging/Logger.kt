package io.github.bgavyus.lightningcamera.logging

import io.github.bgavyus.lightningcamera.extensions.callerStackTraceElement
import io.github.bgavyus.lightningcamera.extensions.fileNameWithoutExtension

object Logger {
    private val printer = PrinterProvider.get()

    fun log(message: String) {
        val tag = Throwable().callerStackTraceElement.fileNameWithoutExtension
        val threadName = Thread.currentThread().name
        printer.println("$tag: [$threadName] $message")
    }
}
