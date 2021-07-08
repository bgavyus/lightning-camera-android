package io.github.bgavyus.lightningcamera.extensions.android.app

import android.app.Activity
import android.os.Build

val Activity.displayCompat
    get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else window.decorView.display
