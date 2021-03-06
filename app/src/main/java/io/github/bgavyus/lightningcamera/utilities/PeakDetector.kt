package io.github.bgavyus.lightningcamera.utilities

import com.google.common.collect.EvictingQueue
import io.github.bgavyus.lightningcamera.extensions.distance

@Suppress("UnstableApiUsage")
class PeakDetector(
    private val windowSize: Int,
    private val deviationThreshold: Double,
    private val detectionWeight: Double,
) {
    private val queue = EvictingQueue.create<Double>(windowSize)
    private var sum = 0.0

    private val mean get() = sum / windowSize

    fun getDetectingAndAdd(sample: Double): Boolean {
//        Logger.log("${100 * sample}")
        val detecting = detecting(sample)
//        Logger.log("${detecting.toInt()}")
        val sampleWeight = if (detecting) detectionWeight else 1.0
        val weightedSample = (1 - sampleWeight) * mean + sampleWeight * sample
//        Logger.log("${100 * weightedSample}")
        add(weightedSample)
        return detecting
    }

    private fun detecting(sample: Double): Boolean {
//        Logger.log("${100 * mean}")
        val deviation = sample distance mean
//        Logger.log("${100 * deviation}")
        return deviation > deviationThreshold
    }

    private fun add(sample: Double) {
        sum += sample - (queue.peek() ?: 0.0)
        queue.add(sample)
    }
}
