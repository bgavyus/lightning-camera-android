package io.github.bgavyus.splash.common

import android.util.Range
import android.util.Size

val Size.area get() = width * height

val Range<Int>.middle get() = (lower + upper) / 2
