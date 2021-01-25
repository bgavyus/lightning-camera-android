package io.github.bgavyus.lightningcamera.logging

class LoggerTagExtractor {
    private val regex = Regex("""(\w+)(?:$|\$)""")

    fun extract(stackTraceElement: StackTraceElement): String {
        val result = regex.find(stackTraceElement.className) ?: throw RuntimeException()
        return result.groupValues[1]
    }
}
