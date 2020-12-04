package io.github.bgavyus.lightningcamera.common.extensions

import android.content.Context

inline fun <reified T> Context.systemService() = getSystemService(T::class.java)
    ?: throw RuntimeException()
