package io.github.bgavyus.lightningcamera.extensions

import android.util.Range

fun <T : Comparable<T>> T.toRange() = Range(this, this)
