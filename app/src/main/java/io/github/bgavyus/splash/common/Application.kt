package io.github.bgavyus.splash.common

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import io.github.bgavyus.splash.storage.Storage
import javax.inject.Inject
import kotlin.system.exitProcess

@HiltAndroidApp
class Application : Application(), Thread.UncaughtExceptionHandler {
    companion object {
        private val TAG = Application::class.simpleName

        const val UNCAUGHT_EXCEPTION_STATUS_CODE = 1
    }

    @Inject
    lateinit var storage: Storage

    @Inject
    lateinit var device: Device

    override fun onCreate() {
        super.onCreate()
        Thread.setDefaultUncaughtExceptionHandler(this)
        logState()
    }

    private fun logState() {
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
