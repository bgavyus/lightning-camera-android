package io.github.bgavyus.lightningcamera.extensions

import android.widget.ToggleButton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.callbackFlow

fun ToggleButton.checked() = callbackFlow {
    setOnCheckedChangeListener { _, checked -> sendBlocking(checked) }
    awaitClose { setOnCheckedChangeListener(null) }
}
