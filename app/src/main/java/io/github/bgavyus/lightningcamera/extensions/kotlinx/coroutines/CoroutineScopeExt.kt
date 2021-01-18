package io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn

fun CoroutineScope.cancel(throwable: Throwable) = cancel(throwable.javaClass.simpleName, throwable)
fun CoroutineScope.launchAll(vararg flows: Flow<*>) = flows.forEach { it.launchIn(this) }
