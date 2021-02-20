package io.github.bgavyus.lightningcamera.utilities

import io.github.bgavyus.lightningcamera.logging.Logger
import java.util.*

open class DeferScope : AutoCloseable {
    private val stack = ArrayDeque<() -> Unit>()

    fun defer(block: () -> Unit) = synchronized(this) {
        stack.push(block)
    }

    override fun close() = synchronized(this) {
        while (!stack.isEmpty()) {
            val block = stack.pop()
            Logger.log("Invoking: $block")
            block.invoke()
        }
    }
}
