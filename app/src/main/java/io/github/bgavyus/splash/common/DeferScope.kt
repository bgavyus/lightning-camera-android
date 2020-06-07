package io.github.bgavyus.splash.common

import android.util.Log
import java.util.*

// TODO: Support async blocks
open class DeferScope : AutoCloseable {
    companion object {
        private val TAG = DeferScope::class.simpleName
    }

    private val stack = ArrayDeque<Block>()

    fun defer(block: Block) = synchronized(this) {
        stack.push(block)
    }

    override fun close() = synchronized(this) {
        while (!stack.isEmpty()) {
            val block = stack.pop()
            Log.v(TAG, "Closing: $block")

            try {
                block.invoke()
            } catch (error: Throwable) {
                Log.w(TAG, "Error while closing", error)
            }
        }
    }
}

private typealias Block = () -> Unit
