package io.github.bgavyus.lightningcamera.extensions.android.content

import android.content.Context
import android.content.pm.PackageManager

inline fun <reified T> Context.systemService(): T = getSystemService(T::class.java)

fun Context.hasGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
