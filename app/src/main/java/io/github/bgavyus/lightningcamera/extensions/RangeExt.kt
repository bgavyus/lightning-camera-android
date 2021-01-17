package io.github.bgavyus.lightningcamera.extensions

import android.util.Range

val <T : Comparable<T>> Range<T>.isSingular get() = lower == upper
