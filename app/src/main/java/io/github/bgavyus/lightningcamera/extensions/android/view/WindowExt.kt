package io.github.bgavyus.lightningcamera.extensions.android.view

import android.view.Window
import androidx.core.view.WindowCompat

fun Window.setDecorFitsSystemWindowsCompat(decorFitsSystemWindows: Boolean) =
    WindowCompat.setDecorFitsSystemWindows(this, decorFitsSystemWindows)
