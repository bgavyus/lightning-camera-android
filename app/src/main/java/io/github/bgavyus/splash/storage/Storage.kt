package io.github.bgavyus.splash.storage

import android.os.Build
import android.os.Environment

object Storage {
    val legacy get() = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || Environment.isExternalStorageLegacy()
}
