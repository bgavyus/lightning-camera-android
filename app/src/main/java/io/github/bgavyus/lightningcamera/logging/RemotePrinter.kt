package io.github.bgavyus.lightningcamera.logging

import android.util.Printer
import com.google.firebase.crashlytics.FirebaseCrashlytics

class RemotePrinter : Printer {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    override fun println(line: String) = crashlytics.log(line)
}
