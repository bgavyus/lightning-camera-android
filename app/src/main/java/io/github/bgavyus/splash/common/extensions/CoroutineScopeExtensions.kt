package io.github.bgavyus.splash.common.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel

fun CoroutineScope.cancel(throwable: Throwable) = cancel(throwable.javaClass.simpleName, throwable)
