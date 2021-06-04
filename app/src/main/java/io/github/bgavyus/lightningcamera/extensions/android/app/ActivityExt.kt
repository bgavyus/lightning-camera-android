package io.github.bgavyus.lightningcamera.extensions.android.app

import android.app.Activity
import android.os.Build
import android.view.Display

val Activity.requireDisplay: Display
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        display ?: throw RuntimeException()
    } else {
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay
    }
