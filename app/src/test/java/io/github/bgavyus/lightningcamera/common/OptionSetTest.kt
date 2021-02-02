package io.github.bgavyus.lightningcamera.common

import org.junit.Assert.*
import org.junit.Test

class OptionSetTest {
    private val a = 1 shl 0
    private val b = 1 shl 1
    private val c = 1 shl 2

    @Test
    fun contains() {
        assertTrue(0 in OptionSet())
        assertTrue(0 in OptionSet(a))
        assertTrue(a in OptionSet(a))
        assertFalse(b in OptionSet(a))
        assertTrue(b in OptionSet(a + b))
        assertFalse(c in OptionSet(a + b))
        assertTrue(a + c in OptionSet(a + c))
    }

    @Test
    fun containsAll() {
        assertTrue(OptionSet(a + c).containsAll(listOf(a, c)))
        assertTrue(OptionSet(a + b).containsAll(listOf(b)))
        assertFalse(OptionSet(a + b).containsAll(listOf(a, c)))
        assertTrue(OptionSet().containsAll(listOf()))
        assertTrue(OptionSet(a + b).containsAll(listOf()))
    }

    @Test
    fun getMask() {
        assertEquals(0, OptionSet().mask)
        assertEquals(a, OptionSet(a).mask)
        assertEquals(a + c, OptionSet(a + c).mask)
    }

    @Test
    fun getSize() {
        assertEquals(0, OptionSet().size)
        assertEquals(1, OptionSet(b).size)
        assertEquals(2, OptionSet(a + c).size)
    }

    @Test
    fun isEmpty() {
        assertTrue(OptionSet().isEmpty())
        assertFalse(OptionSet(b).isEmpty())
        assertFalse(OptionSet(a + c).isEmpty())
    }

    @Test
    operator fun iterator() {
        assertEquals(listOf<Int>(), OptionSet().toList())
        assertEquals(listOf(b), OptionSet(b).toList())
        assertEquals(listOf(a, c), OptionSet(a + c).toList())
    }
}
