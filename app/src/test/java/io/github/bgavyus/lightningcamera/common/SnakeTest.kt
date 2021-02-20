package io.github.bgavyus.lightningcamera.common

import io.github.bgavyus.lightningcamera.utilities.Snake
import junit.framework.Assert.assertEquals
import org.junit.Test

class SnakeTest {
    @Test
    fun size() {
        val snake = createSnake()
        snake.feed { 1 }
        var count = 0
        snake.drain { count++ }
        assertEquals(1, count)
    }

    @Test
    fun maxSize() {
        val snake = createSnake()

        repeat(4) {
            snake.feed { 1 }
        }

        var count = 0
        snake.drain { count++ }
        assertEquals(3, count)
    }

    @Test
    fun feedOne() {
        val snake = createSnake()
        var value = -1

        snake.feed {
            value = it
            it
        }

        assertEquals(0, value)
    }

    @Test
    fun drainOne() {
        val snake = createSnake()
        snake.feed { 1 }
        var value = 0
        snake.drain { value = it }
        assertEquals(1, value)
    }

    @Test
    fun drainMultiple() {
        val snake = createSnake()

        repeat(3) {
            snake.feed { 1 }
        }

        val list = mutableListOf<Int>()
        snake.drain(list::add)
        assertEquals(listOf(1, 1, 1), list)
    }

    @Test
    fun drainInOrder() {
        val snake = createSnake()

        (1..3).forEach { number ->
            snake.feed { number }
        }

        val list = mutableListOf<Int>()
        snake.drain(list::add)
        assertEquals((1..3).toList(), list)
    }

    @Test
    fun dropOld() {
        val snake = createSnake()

        (1..6).forEach { number ->
            snake.feed { number }
        }

        val list = mutableListOf<Int>()
        snake.drain(list::add)
        assertEquals((4..6).toList(), list)
    }

    @Test
    fun feedBasedOnDroppedValue() {
        val snake = createSnake()

        repeat(6) {
            snake.feed { it + 1 }
        }

        val list = mutableListOf<Int>()
        snake.drain(list::add)
        assertEquals(listOf(2, 2, 2), list)
    }

    private fun createSnake() = Snake(Array(3) { 0 })
}
