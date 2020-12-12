package io.github.bgavyus.lightningcamera.common

import java.util.*

open class DeferScope : AutoCloseable {
    private val stack = ArrayDeque<Block>()

    fun defer(block: Block) = synchronized(this) {
        stack.push(block)
    }

    override fun close() = synchronized(this) {
        while (!stack.isEmpty()) {
            val block = stack.pop()
            Logger.debug("Closing: $block")

            try {
                block.invoke()
            } catch (exception: Exception) {
                Logger.error("Exception while closing", exception)
            }
        }
    }
}

private typealias Block = () -> Unit
