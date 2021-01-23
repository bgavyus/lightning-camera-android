package io.github.bgavyus.lightningcamera.common

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics
import io.github.bgavyus.lightningcamera.BuildConfig

object Logger {
    private val tagRegex = Regex("""(\w+)(?:$|\$)""")
    private val crashlytics = FirebaseCrashlytics.getInstance()

    fun log(message: String) {
        val callerStackTraceElement = Throwable().stackTrace[1]
        val tag = extractTag(callerStackTraceElement)
        log(tag, message)
    }

    private fun extractTag(stackTraceElement: StackTraceElement): String {
        val result = tagRegex.find(stackTraceElement.className)
            ?: throw RuntimeException()

        return result.groupValues[1]
    }

    private fun log(tag: String, message: String) =
        if (BuildConfig.DEBUG) {
            localLog(tag, message)
        } else {
            remoteLog(tag, message)
        }

    private fun localLog(tag: String, message: String) {
        Log.println(Log.DEBUG, "${BuildConfig.APPLICATION_ID}.$tag", message)
    }

    private fun remoteLog(tag: String, message: String) {
        crashlytics.log("$tag: $message")
    }
}
