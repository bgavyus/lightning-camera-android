package io.github.bgavyus.lightningcamera.logging

import org.junit.Assert.assertEquals
import org.junit.Test

class LoggerTagExtractorTest {
    private val extractor = LoggerTagExtractor()

    @Test
    fun extract() {
        test("com.example.app.module.ClassName")
        test("com.example.app.module.submodule.ClassName")
        test("com.example.app.module.ClassName\$method")
        test("com.example.app.module.ClassName\$method$2")
    }

    private fun test(className: String) {
        assertEquals("ClassName", extractor.extract(className))
    }
}
