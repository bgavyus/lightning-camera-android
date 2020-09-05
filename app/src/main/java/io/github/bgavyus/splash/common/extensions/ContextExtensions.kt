package io.github.bgavyus.splash.common.extensions

import android.content.Context

inline fun <reified T> Context.systemService() = getSystemService(T::class.java)
    ?: throw RuntimeException()
