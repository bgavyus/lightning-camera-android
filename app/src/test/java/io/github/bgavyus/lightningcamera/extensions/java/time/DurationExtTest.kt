package io.github.bgavyus.lightningcamera.extensions.java.time

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class DurationExtTest {
    @Test
    fun testFormats() {
        testFormat("0S", "0:00")
        testFormat("0.1S", "0:00")
        testFormat("1S", "0:01")
        testFormat("10S", "0:10")
        testFormat("1M", "1:00")
        testFormat("1M1S", "1:01")
        testFormat("10M", "10:00")
        testFormat("1H", "1:00:00")
        testFormat("1H1S", "1:00:01")
        testFormat("1H1M", "1:01:00")
        testFormat("1H1M1S", "1:01:01")
        testFormat("100H", "100:00:00")
    }

    private fun testFormat(duration: String, formatted: String) {
        assertEquals(formatted, Duration.parse("PT$duration").format())
    }
}
