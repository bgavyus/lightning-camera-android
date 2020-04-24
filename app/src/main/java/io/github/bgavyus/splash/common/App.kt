package io.github.bgavyus.splash.common

import android.app.Application
import android.view.WindowManager

class App : Application() {
    companion object {
        lateinit var shared: App
    }

    override fun onCreate() {
        super.onCreate()
        shared = this
    }

    private val displayRotation: Rotation
        get() = getSystemService(WindowManager::class.java)
            ?.let { Rotation.fromSurfaceRotation(it.defaultDisplay.rotation) }
            ?: throw RuntimeException("Failed to get display rotation")

    val deviceOrientation: Rotation get() = -displayRotation
}
