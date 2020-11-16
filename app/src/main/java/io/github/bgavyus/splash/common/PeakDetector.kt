package io.github.bgavyus.splash.common

import kotlin.math.absoluteValue

class PeakDetector(
    private val windowSize: Int,
    private val deviationThreshold: Double,
    private val detectionWeight: Double
) {
    private val snake = Snake(Array(windowSize) { 0.0 })
    private var sum = 0.0

    private val mean get() = sum / windowSize

    fun getDetectingAndAdd(sample: Double): Boolean {
//        Logger.verbose("${100 * sample}")
        val detecting = detecting(sample)
//        Logger.verbose("${detecting.toInt()}")
        val sampleWeight = if (detecting) detectionWeight else 1.0
        val weightedSample = (1 - sampleWeight) * mean + sampleWeight * sample
//        Logger.verbose("${100 * weightedSample}")
        add(weightedSample)
        return detecting
    }

    private fun detecting(sample: Double): Boolean {
//        Logger.verbose("${100 * mean}")
        val zScore = (sample - mean).absoluteValue
//        Logger.verbose("${100 * zScore}")
        return zScore > deviationThreshold
    }

    private fun add(sample: Double) {
        snake.feed { removedSample ->
            sum += sample - removedSample
            sample
        }
    }
}
