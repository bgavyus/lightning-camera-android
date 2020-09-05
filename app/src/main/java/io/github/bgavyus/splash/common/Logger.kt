package io.github.bgavyus.splash.common

import android.util.Log

object Logger {
    private val tagRegex = Regex("""(\w+)(?:$|\$)""")

    fun verbose(message: String) = log(Log.VERBOSE, message)
    fun debug(message: String) = log(Log.DEBUG, message)
    fun info(message: String) = log(Log.INFO, message)
    fun warn(message: String) = log(Log.WARN, message)
    fun warn(message: String, throwable: Throwable) = log(Log.WARN, message, throwable)
    fun error(message: String) = log(Log.ERROR, message)
    fun error(message: String, throwable: Throwable) = log(Log.ERROR, message, throwable)

    private fun log(priority: Int, message: String, throwable: Throwable? = null) {
        Log.println(priority, getTag(), resolveMessage(message, throwable))
    }

    private fun getTag(): String {
        val element = Throwable().stackTrace[4]
        val result = tagRegex.find(element.className) ?: throw RuntimeException()
        return result.groupValues[1]
    }

    private fun resolveMessage(message: String, throwable: Throwable? = null) =
        if (throwable == null) message else "$message\n${Log.getStackTraceString(throwable)}"
}
