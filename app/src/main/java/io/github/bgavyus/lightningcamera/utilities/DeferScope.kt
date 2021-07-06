package io.github.bgavyus.lightningcamera.utilities

import com.google.common.collect.Queues
import java.util.*

open class DeferScope : AutoCloseable {
    private val stack = Queues.synchronizedDeque(ArrayDeque<() -> Unit>())

    fun defer(block: () -> Unit) {
        stack.push(block)
    }

    override fun close() {
        while (true) {
            val block = stack.poll() ?: break
            block()
        }
    }
}
