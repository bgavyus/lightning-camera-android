package io.github.bgavyus.lightningcamera.graphics

import android.util.Size
import androidx.core.graphics.values
import io.github.bgavyus.lightningcamera.extensions.normalized
import io.github.bgavyus.lightningcamera.utilities.Rotation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformMatrixFactoryTest {
    private val natural = Rotation.Natural
    private val right = Rotation.Right
    private val upsideDown = Rotation.UpsideDown
    private val left = Rotation.Left

    private val wide = Size(200, 100)
    private val tall = Size(100, 200)

    @Test
    fun testLeftTallToTall() = test(left, tall, tall, false, listOf(0, -0.5, 100, 2, 0, 0))

    @Test
    fun testLeftTallToWide() = test(left, tall, wide, false, listOf(0, -0.5, 125, 0.5, 0, 0))

    @Test
    fun testLeftWideToTall() = test(left, wide, tall, false, listOf(0, -0.5, 100, 0.5, 0, 75))

    @Test
    fun testLeftWideToWide() = test(left, wide, wide, false, listOf(0, -2, 200, 0.5, 0, 0))

    @Test
    fun testNaturalTallToTall() = test(natural, tall, tall, false, listOf(1, 0, 0, 0, 0.25, 75))

    @Test
    fun testNaturalTallToWide() = test(natural, tall, wide, false, listOf(1, 0, 0, 0, 1, 0))

    @Test
    fun testNaturalWideToTall() = test(natural, wide, tall, false, listOf(1, 0, 0, 0, 1, 0))

    @Test
    fun testNaturalWideToWide() = test(natural, wide, wide, false, listOf(0.25, 0, 75, 0, 1, 0))

    @Test
    fun testRightTallToTall() = test(right, tall, tall, false, listOf(0, 0.5, 0, -2, 0, 200))

    @Test
    fun testRightTallToWide() = test(right, tall, wide, false, listOf(0, 0.5, 75, -0.5, 0, 100))

    @Test
    fun testRightWideToTall() = test(right, wide, tall, false, listOf(0, 0.5, 0, -0.5, 0, 125))

    @Test
    fun testRightWideToWide() = test(right, wide, wide, false, listOf(0, 2, 0, -0.5, 0, 100))

    @Test
    fun testUpsideDownTallToTall() =
        test(upsideDown, tall, tall, false, listOf(-1, 0, 100, 0, -0.25, 125))

    @Test
    fun testUpsideDownTallToWide() =
        test(upsideDown, tall, wide, false, listOf(-1, 0, 200, 0, -1, 100))

    @Test
    fun testUpsideDownWideToTall() =
        test(upsideDown, wide, tall, false, listOf(-1, 0, 100, 0, -1, 200))

    @Test
    fun testUpsideDownWideToWide() =
        test(upsideDown, wide, wide, false, listOf(-0.25, 0, 125, 0, -1, 100))

    @Test
    fun testLeftTallToTallFill() = test(left, tall, tall, true, listOf(0, -0.5, 100, 2, 0, 0))

    @Test
    fun testLeftTallToWideFill() = test(left, tall, wide, true, listOf(0, -2, 200, 2, 0, -150))

    @Test
    fun testLeftWideToTallFill() = test(left, wide, tall, true, listOf(0, -2, 250, 2, 0, 0))

    @Test
    fun testLeftWideToWideFill() = test(left, wide, wide, true, listOf(0, -2, 200, 0.5, 0, 0))

    @Test
    fun testNaturalTallToTallFill() = test(natural, tall, tall, true, listOf(4, 0, -150, 0, 1, 0))

    @Test
    fun testNaturalTallToWideFill() = test(natural, tall, wide, true, listOf(1, 0, 0, 0, 1, 0))

    @Test
    fun testNaturalWideToTallFill() = test(natural, wide, tall, true, listOf(1, 0, 0, 0, 1, 0))

    @Test
    fun testNaturalWideToWideFill() = test(natural, wide, wide, true, listOf(1, 0, 0, 0, 4, -150))

    @Test
    fun testRightTallToTallFill() = test(right, tall, tall, true, listOf(0, 0.5, 0, -2, 0, 200))

    @Test
    fun testRightTallToWideFill() = test(right, tall, wide, true, listOf(0, 2, 0, -2, 0, 250))

    @Test
    fun testRightWideToTallFill() = test(right, wide, tall, true, listOf(0, 2, -150, -2, 0, 200))

    @Test
    fun testRightWideToWideFill() = test(right, wide, wide, true, listOf(0, 2, 0, -0.5, 0, 100))

    @Test
    fun testUpsideDownTallToTallFill() =
        test(upsideDown, tall, tall, true, listOf(-4, 0, 250, 0, -1, 200))

    @Test
    fun testUpsideDownTallToWideFill() =
        test(upsideDown, tall, wide, true, listOf(-1, 0, 200, 0, -1, 100))

    @Test
    fun testUpsideDownWideToTallFill() =
        test(upsideDown, wide, tall, true, listOf(-1, 0, 100, 0, -1, 200))

    @Test
    fun testUpsideDownWideToWideFill() =
        test(upsideDown, wide, wide, true, listOf(-1, 0, 200, 0, -4, 250))

    private fun test(
        rotation: Rotation,
        inputSize: Size,
        outputSize: Size,
        fill: Boolean,
        values: List<Number>,
    ) {
        val matrix = TransformMatrixFactory.create(rotation, inputSize, outputSize, fill)

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
