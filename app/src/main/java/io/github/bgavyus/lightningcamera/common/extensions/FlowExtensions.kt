package io.github.bgavyus.lightningcamera.common.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

fun Flow<Unit>.callOnEach(action: () -> Unit) = onEach { action() }
fun <T> Flow<T>.callOnEach(action: (T) -> Unit) = onEach { action(it) }

fun <T> Flow<T>.reflectTo(other: MutableStateFlow<T>) = onEach { other.value = it }

fun Flow<Boolean>.onToggle(on: () -> Any?, off: () -> Any?) =
    distinctUntilChanged().onEach { if (it) on() else off() }

infix fun Flow<Boolean>.and(flow: Flow<Boolean>) = combine(flow) { a, b -> a && b }

fun CoroutineScope.launchAll(vararg flows: Flow<Any?>) = flows.forEach { it.launchIn(this) }
