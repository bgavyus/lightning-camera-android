package io.github.bgavyus.splash.detection

abstract class Detector(private val listener: DetectionListener) {
    private var lastDetected = false

    internal fun propagate(detected: Boolean) {
        if (detected == lastDetected) {
            return
        }

        lastDetected = detected

        if (detected) {
            listener.onSubjectEntered()
        } else {
            listener.onSubjectLeft()
        }
    }
}
