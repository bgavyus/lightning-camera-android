package io.github.bgavyus.lightningcamera.logging

class LoggerTagExtractor {
    private val regex = Regex("""(\w+)(?:$|\$)""")

    fun extract(className: String): String {
        val result = regex.find(className) ?: throw RuntimeException()
        return result.groupValues[1]
    }
}
