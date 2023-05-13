package io.github.bgavyus.lightningcamera.extensions.android.content

import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.getSystemService

inline fun <reified T> Context.requireSystemService(): T =
    getSystemService() ?: throw IllegalStateException()

fun Context.hasGranted(permission: String) =
    checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED
