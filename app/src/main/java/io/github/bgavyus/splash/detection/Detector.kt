package io.github.bgavyus.splash.detection

abstract class Detector(private val listener: DetectionListener) :
    AutoCloseable {
    private var lastDetected = false

    private fun propagate(detected: Boolean) {
        if (detected == lastDetected) {
            return
        }

        lastDetected = detected

        if (detected) {
            listener.onDetectionStarted()
        } else {
            listener.onDetectionEnded()
        }
    }

    abstract fun detected(): Boolean

    open fun detect() {
        propagate(detected())
    }
}
