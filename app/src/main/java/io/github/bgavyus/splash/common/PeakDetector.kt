package io.github.bgavyus.splash.common

import com.aliasi.stats.OnlineNormalEstimator
import kotlin.math.absoluteValue

class PeakDetector(
    windowSize: Int,
    private val deviationThreshold: Double,
    private val detectionWeight: Double
) {
    private val snake = Snake(Array(windowSize) { 0.0 })
    private val normalEstimator = OnlineNormalEstimator().apply { repeat(windowSize) { handle(0.0) } }

    fun getDetectingAndAdd(sample: Double): Boolean {
//        Logger.verbose("${100 * sample}")
        val detecting = detecting(sample)
//        Logger.verbose("${detecting.toInt()}")
        val sampleWeight = if (detecting) detectionWeight else 1.0
        val weightedSample = (1 - sampleWeight) * normalEstimator.mean() + sampleWeight * sample
//        Logger.verbose("${100 * weightedSample}")
        add(weightedSample)
        return detecting
    }

    private fun detecting(sample: Double): Boolean {
        val mean = normalEstimator.mean()
//        Logger.verbose("${100 * mean}")
        val zScore = (sample - mean).absoluteValue
//        Logger.verbose("${100 * zScore}")
        return zScore > deviationThreshold
    }

    private fun add(sample: Double) {
        normalEstimator.handle(sample)
        snake.feed { removedSample ->
            normalEstimator.unHandle(removedSample)
            sample
        }
    }
}
