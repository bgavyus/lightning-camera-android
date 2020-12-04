package io.github.bgavyus.lightningcamera.common.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

fun CoroutineScope.cancel(throwable: Throwable) = cancel(throwable.javaClass.simpleName, throwable)
