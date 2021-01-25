package io.github.bgavyus.lightningcamera.logging

import com.google.firebase.crashlytics.FirebaseCrashlytics

class RemoteLogger : MessageLogger {
    private val crashlytics = FirebaseCrashlytics.getInstance()
    override fun log(message: String) = crashlytics.log(message)
}
