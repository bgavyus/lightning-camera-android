package io.github.bgavyus.splash.common

import android.app.Application
import android.util.Log
import io.github.bgavyus.splash.storage.Storage
import kotlin.system.exitProcess

// TODO: Use dependency injection
class Application : Application(), Thread.UncaughtExceptionHandler {
    companion object {
        private val TAG = Application::class.simpleName

        const val UNCAUGHT_EXCEPTION_STATUS_CODE = 1

        lateinit var context: Application
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        Thread.setDefaultUncaughtExceptionHandler(this)
        logState()
    }

    private fun logState() {
        val device = Device(context)
        val storage = Storage(context.contentResolver)
        Log.i(TAG, "Device Orientation: ${device.orientation}")
        Log.i(TAG, "Display Supported Modes: ${device.display.supportedModes.joinToString()}")
        Log.i(TAG, "Display mode: ${device.display.mode}")
        Log.i(TAG, "Storage: ${if (storage.legacy) "legacy" else "scoped"}")
    }

    override fun uncaughtException(thread: Thread, error: Throwable) {
        Log.e(TAG, "Uncaught Exception from: ${thread.name}", error)
        exitProcess(UNCAUGHT_EXCEPTION_STATUS_CODE)
    }
}
