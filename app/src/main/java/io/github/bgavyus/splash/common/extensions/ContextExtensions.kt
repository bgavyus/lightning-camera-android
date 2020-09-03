package io.github.bgavyus.splash.common.extensions

import android.content.Context
import kotlin.reflect.KClass

fun <T : Any> Context.systemService(kClass: KClass<T>) = getSystemService(kClass.java)
    ?: throw RuntimeException()
