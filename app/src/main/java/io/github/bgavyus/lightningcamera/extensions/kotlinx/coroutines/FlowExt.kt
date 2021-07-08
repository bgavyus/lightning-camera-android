package io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines

import kotlinx.coroutines.flow.*

fun <T> Flow<T>.onEachChange(action: (old: T, new: T) -> Unit) =
    distinctUntilChanged { old, new ->
        action(old, new)
        false
    }

fun <T> Flow<T>.reflectTo(other: MutableStateFlow<T>) = onEach { other.value = it }

infix fun Flow<Boolean>.and(flow: Flow<Boolean>) = combine(flow) { a, b -> a && b }
