package io.github.bgavyus.lightningcamera.graphics

import android.util.Size
import androidx.core.graphics.values
import io.github.bgavyus.lightningcamera.extensions.normalized
import io.github.bgavyus.lightningcamera.utilities.Degrees
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformMatrixFactoryTest {
    private val natural = 0
    private val right = 90
    private val upsideDown = 180
    private val left = 270

    private val wide = Size(200, 100)
    private val tall = Size(100, 200)

    @Test
    fun testLeftTallToTall() = test(left, tall, tall, listOf(0, -0.5, 100, 2, 0, 0))

    @Test
    fun testLeftTallToWide() = test(left, tall, wide, listOf(0, -0.5, 125, 0.5, 0, 0))

    @Test
    fun testLeftWideToTall() = test(left, wide, tall, listOf(0, -0.5, 100, 0.5, 0, 75))

    @Test
    fun testLeftWideToWide() = test(left, wide, wide, listOf(0, -2, 200, 0.5, 0, 0))

    @Test
    fun testNaturalTallToTall() = test(natural, tall, tall, listOf(1, 0, 0, 0, 0.25, 75))

    @Test
    fun testNaturalTallToWide() = test(natural, tall, wide, listOf(1, 0, 0, 0, 1, 0))

    @Test
    fun testNaturalWideToTall() = test(natural, wide, tall, listOf(1, 0, 0, 0, 1, 0))

    @Test
    fun testNaturalWideToWide() = test(natural, wide, wide, listOf(0.25, 0, 75, 0, 1, 0))

    @Test
    fun testRightTallToTall() = test(right, tall, tall, listOf(0, 0.5, 0, -2, 0, 200))

    @Test
    fun testRightTallToWide() = test(right, tall, wide, listOf(0, 0.5, 75, -0.5, 0, 100))

    @Test
    fun testRightWideToTall() = test(right, wide, tall, listOf(0, 0.5, 0, -0.5, 0, 125))

    @Test
    fun testRightWideToWide() = test(right, wide, wide, listOf(0, 2, 0, -0.5, 0, 100))

    @Test
    fun testUpsideDownTallToTall() = test(upsideDown, tall, tall, listOf(-1, 0, 100, 0, -0.25, 125))

    @Test
    fun testUpsideDownTallToWide() = test(upsideDown, tall, wide, listOf(-1, 0, 200, 0, -1, 100))

    @Test
    fun testUpsideDownWideToTall() = test(upsideDown, wide, tall, listOf(-1, 0, 100, 0, -1, 200))

    @Test
    fun testUpsideDownWideToWide() = test(upsideDown, wide, wide, listOf(-0.25, 0, 125, 0, -1, 100))

    private fun test(
        rotation: Int,
        inputSize: Size,
        outputSize: Size,
        values: List<Number>,
    ) {
        val matrix = TransformMatrixFactory.create(Degrees(rotation), inputSize, outputSize)

        val expectedValues = values
            .map(Number::toFloat)

        val actualValue = matrix.values()
            .slice(0..5)
            .map(Float::normalized)

        assertTrue(matrix.isAffine)
        assertTrue(matrix.rectStaysRect())
        assertEquals(expectedValues, actualValue)
    }
}
