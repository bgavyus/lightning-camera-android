package io.github.bgavyus.lightningcamera.utilities

import java.util.*

open class DeferScope : AutoCloseable {
    private val stack = ArrayDeque<() -> Unit>()

    @Synchronized
    fun defer(block: () -> Unit) {
        stack.push(block)
    }

    @Synchronized
    override fun close() {
        while (true) {
            val block = stack.poll() ?: break
            block()
        }
    }
}
