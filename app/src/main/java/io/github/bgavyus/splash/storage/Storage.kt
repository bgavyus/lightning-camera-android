package io.github.bgavyus.splash.storage

import android.os.Build
import android.os.Environment

object Storage {
    val scoped: Boolean
        get() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return false
            }

            return !Environment.isExternalStorageLegacy()
        }
}
