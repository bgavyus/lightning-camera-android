package io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines

import kotlinx.coroutines.flow.*

fun <T> Flow<T>.reflectTo(other: MutableStateFlow<T>) = onEach { other.value = it }

fun Flow<Boolean>.onToggle(on: () -> Unit, off: () -> Unit) =
    distinctUntilChanged().onEach { if (it) on() else off() }

infix fun Flow<Boolean>.and(flow: Flow<Boolean>) =
    combine(flow) { a, b -> a && b }
