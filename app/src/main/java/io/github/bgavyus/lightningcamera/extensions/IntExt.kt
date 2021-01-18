package io.github.bgavyus.lightningcamera.extensions

import android.util.Log

operator fun Int.contains(mask: Int) = this and mask == mask

val Int.logSymbol
    get() = when (this) {
        Log.VERBOSE -> 'V'
        Log.DEBUG -> 'D'
        Log.INFO -> 'I'
        Log.WARN -> 'W'
        Log.ERROR -> 'E'
        Log.ASSERT -> 'A'
        else -> '?'
    }
