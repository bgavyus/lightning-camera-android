package io.github.bgavyus.lightningcamera.extensions.android.util

import android.util.Range

val <T : Comparable<T>> Range<T>.isSingular get() = lower == upper
