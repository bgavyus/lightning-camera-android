package io.github.bgavyus.lightningcamera.logging

object Logger {
    private val tagExtractor = LoggerTagExtractor()
    private val printer = PrinterProvider.get()

    fun log(message: String) {
        val threadName = Thread.currentThread().name
        val callerStackTraceElement = Throwable().stackTrace[1]
        val tag = tagExtractor.extract(callerStackTraceElement)
        printer.println("$tag: [$threadName] $message")
    }
}
