package io.github.bgavyus.lightningcamera.extensions.kotlinx

import kotlinx.coroutines.flow.*

fun Flow<Unit>.callOnEach(action: () -> Unit) = onEach { action() }
fun <T> Flow<T>.callOnEach(action: (T) -> Unit) = onEach { action(it) }

fun <T> Flow<T>.reflectTo(other: MutableStateFlow<T>) = onEach { other.value = it }

fun Flow<Boolean>.onToggle(on: () -> Unit, off: () -> Unit) =
    distinctUntilChanged().onEach { if (it) on() else off() }

infix fun Flow<Boolean>.and(flow: Flow<Boolean>) =
    combine(flow) { a, b -> a && b }.distinctUntilChanged()
