package io.github.bgavyus.splash.common

import android.app.Application
import android.view.WindowManager

class App : Application() {
    companion object {
        lateinit var context: App

        private val windowManager
            get() = context.getSystemService(WindowManager::class.java)
                ?: throw RuntimeException("Failed to get window manager service")

        val deviceOrientation: Rotation
            get() = -Rotation.fromSurfaceRotation(windowManager.defaultDisplay.rotation)
    }

    override fun onCreate() {
        super.onCreate()
        context = this
    }
}
