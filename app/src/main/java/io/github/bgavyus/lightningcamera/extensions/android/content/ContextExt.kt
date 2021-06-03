package io.github.bgavyus.lightningcamera.extensions.android.content

import android.content.Context
import android.content.pm.PackageManager

inline fun <reified T> Context.systemService() = getSystemService(T::class.java)
    ?: throw RuntimeException()

fun Context.hasGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED

val Context.requireDisplay get() = display ?: throw RuntimeException()
