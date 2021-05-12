package io.github.bgavyus.lightningcamera.extensions.android.widget

import android.widget.ToggleButton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun ToggleButton.checked() = callbackFlow {
    setOnCheckedChangeListener { _, checked -> trySendBlocking(checked) }
    awaitClose { setOnCheckedChangeListener(null) }
}
