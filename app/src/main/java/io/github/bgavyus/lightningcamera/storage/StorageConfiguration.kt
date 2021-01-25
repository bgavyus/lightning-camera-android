package io.github.bgavyus.lightningcamera.storage

import android.os.Build

object StorageConfiguration {
    val isScoped = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
}
