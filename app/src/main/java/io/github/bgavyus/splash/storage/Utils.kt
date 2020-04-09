package io.github.bgavyus.splash.storage

import android.os.Build
import android.os.Environment

fun isStorageScoped(): Boolean {
    if (Build.VERSION.SDK_INT < 29) {
        return false
    }

    return !Environment.isExternalStorageLegacy()
}
