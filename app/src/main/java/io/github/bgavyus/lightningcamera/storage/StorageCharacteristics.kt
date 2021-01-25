package io.github.bgavyus.lightningcamera.storage

import android.Manifest
import android.os.Build

object StorageCharacteristics {
    val isScoped get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
    val permissions get() = if (isScoped) emptyList() else listOf(Manifest.permission.WRITE_EXTERNAL_STORAGE)
}
