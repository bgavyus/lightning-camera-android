package io.github.bgavyus.lightningcamera.logging

import android.util.Log
import android.util.Printer
import io.github.bgavyus.lightningcamera.BuildConfig
import javax.inject.Inject

class LocalPrinter @Inject constructor() : Printer {
    override fun println(line: String) {
        Log.println(Log.DEBUG, BuildConfig.APPLICATION_ID, line)
    }
}
