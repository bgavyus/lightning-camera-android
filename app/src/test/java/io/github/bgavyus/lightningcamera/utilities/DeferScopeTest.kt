package io.github.bgavyus.lightningcamera.utilities

import org.junit.Assert.assertEquals
import org.junit.Test

class DeferScopeTest {
    @Test
    fun deferOne() {
        var count = 0

        DeferScope().use { scope ->
            scope.defer { count++ }
        }

        assertEquals(1, count)
    }

    @Test
    fun deferMultiple() {
        var count = 0

        DeferScope().use { scope ->
            repeat(3) {
                scope.defer { count++ }
            }
        }

        assertEquals(3, count)
    }

    @Test
    fun deferMultipleInOrder() {
        val list = mutableListOf<Int>()

        DeferScope().use { scope ->
            (1..3).forEach {
                scope.defer { list.add(it) }
            }
        }

        assertEquals((3 downTo 1).toList(), list)
    }
}
