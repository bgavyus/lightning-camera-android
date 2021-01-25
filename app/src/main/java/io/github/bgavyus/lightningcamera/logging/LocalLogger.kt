package io.github.bgavyus.lightningcamera.logging

import android.util.Log
import io.github.bgavyus.lightningcamera.BuildConfig

class LocalLogger : MessageLogger {
    override fun log(message: String) {
        Log.println(Log.DEBUG, BuildConfig.APPLICATION_ID, message)
    }
}
