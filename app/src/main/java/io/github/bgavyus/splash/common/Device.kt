package io.github.bgavyus.splash.common

import android.content.Context
import android.view.Display
import android.view.WindowManager

class Device(
    private val context: Context
) {
    val display: Display
        get() = context.getSystemService(WindowManager::class.java)?.defaultDisplay
            ?: throw RuntimeException("Failed to get window manager service")

    val orientation get() = -Rotation.fromSurfaceRotation(display.rotation)
}
