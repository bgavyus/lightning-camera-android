package io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines

import kotlinx.coroutines.Deferred

fun <T> Deferred<T>.getCompletedOrNull() = runCatching { getCompleted() }.getOrNull()
