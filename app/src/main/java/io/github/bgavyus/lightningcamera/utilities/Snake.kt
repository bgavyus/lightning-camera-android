package io.github.bgavyus.lightningcamera.utilities

import io.github.bgavyus.lightningcamera.extensions.floorMod

class Snake<Element>(private val nodes: Array<Element>) {
    private var head = 0
    private var size = 0

    fun feed(block: (Element) -> Element) {
        nodes[head] = block(nodes[head])
        advance()

        if (!full) {
            grow()
        }
    }

    private fun advance() {
        head = headBased(1)
    }

    private val full get() = size == nodes.size

    private fun grow() {
        size++
    }

    fun drain(block: (Element) -> Unit) {
        while (!isEmpty) {
            block(nodes[tail])
            shrink()
        }
    }

    private val isEmpty get() = size == 0
    private val tail get() = headBased(-size)
    private fun headBased(n: Int) = head + n floorMod nodes.size

    private fun shrink() {
        size--
    }

    fun empty() {
        size = 0
    }
}
