package io.github.bgavyus.lightningcamera.extensions.android.view

import android.view.Window
import android.view.WindowManager

fun Window.updateAttributes(block: WindowManager.LayoutParams.() -> Unit) {
    attributes = attributes.apply(block)
}
