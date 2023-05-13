package io.github.bgavyus.lightningcamera.extensions.kotlinx.coroutines

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach

fun <T> Flow<T>.onEachChange(action: (old: T, new: T) -> Unit) =
    distinctUntilChanged { old, new ->
        action(old, new)
        false
    }

fun <T> Flow<T>.reflectTo(other: MutableStateFlow<T>) = onEach { other.value = it }

infix fun Flow<Boolean>.and(flow: Flow<Boolean>) = combine(flow) { a, b -> a && b }
