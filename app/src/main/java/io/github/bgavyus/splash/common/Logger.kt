package io.github.bgavyus.splash.common

import android.util.Log
import io.github.bgavyus.splash.BuildConfig

object Logger {
    private val tagRegex = Regex("""(\w+)(?:$|\$)""")

    fun verbose(message: String) = log(Log.VERBOSE, message)
    fun debug(message: String) = log(Log.DEBUG, message)
    fun info(message: String) = log(Log.INFO, message)
    fun warn(message: String) = log(Log.WARN, message)
    fun warn(message: String, throwable: Throwable) = log(Log.WARN, concat(message, throwable))
    fun error(message: String, throwable: Throwable) = log(Log.ERROR, concat(message, throwable))

    private fun log(priority: Int, message: String) {
        if (BuildConfig.DEBUG) {
            Log.println(priority, tag(), message)
        }
    }

    private fun tag(): String {
        val element = Throwable().stackTrace[3]
        val result = tagRegex.find(element.className) ?: throw RuntimeException()
        return result.groupValues[1]
    }

    private fun concat(message: String, throwable: Throwable) =
        "$message\n${Log.getStackTraceString(throwable)}"
}
