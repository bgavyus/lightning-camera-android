package io.github.bgavyus.splash.common.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

fun <T> Flow<T>.callOnEach(action: (T) -> Unit) = onEach { action(it) }

// TODO: Replace with assignment to StateFlow
fun <T> Flow<T>.reflectTo(other: MutableStateFlow<T>) = onEach { other.value = it }

fun Flow<Boolean>.onToggle(on: () -> Any?, off: () -> Any?) =
    distinctUntilChanged().onEach { if (it) on() else off() }

fun CoroutineScope.launchAll(vararg flows: Flow<Any?>) = flows.forEach { it.launchIn(this) }
