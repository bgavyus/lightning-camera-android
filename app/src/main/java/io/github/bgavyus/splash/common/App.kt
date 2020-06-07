package io.github.bgavyus.splash.common

import android.app.Application
import android.content.res.Configuration
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import io.github.bgavyus.splash.storage.Storage
import java.util.*
import kotlin.system.exitProcess

class App : Application(), Thread.UncaughtExceptionHandler {
    companion object {
        private val TAG = App::class.simpleName

        const val UNCAUGHT_EXCEPTION_STATUS_CODE = 1

        lateinit var context: App

        val deviceOrientation: Rotation
            get() = -Rotation.fromSurfaceRotation(windowManager.defaultDisplay.rotation)

        private val windowManager by lazy {
            context.getSystemService(WindowManager::class.java)
                ?: throw RuntimeException("Failed to get window manager service")
        }

        fun defaultString(resourceId: Int): String {
            val config = Configuration().apply { setLocale(Locale.ROOT) }
            return context.createConfigurationContext(config).getString(resourceId)
        }

        fun showMessage(resourceId: Int) {
            Toast.makeText(context, resourceId, Toast.LENGTH_LONG).run {
                setGravity(Gravity.CENTER, /* xOffset = */ 0, /* yOffset = */ 0)
                show()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        context = this
        Thread.setDefaultUncaughtExceptionHandler(this)
        logState()
    }

    private fun logState() {
        Log.d(TAG, "Device Orientation: $deviceOrientation")
        Log.d(TAG, "Display FPS: ${windowManager.defaultDisplay.refreshRate}")
        Log.d(TAG, "Storage: ${if (Storage.legacy) "legacy" else "scoped"}")
    }

    override fun uncaughtException(thread: Thread, error: Throwable) {
        Log.e(TAG, "Uncaught Exception from: ${thread.name}", error)
        exitProcess(UNCAUGHT_EXCEPTION_STATUS_CODE)
    }
}
