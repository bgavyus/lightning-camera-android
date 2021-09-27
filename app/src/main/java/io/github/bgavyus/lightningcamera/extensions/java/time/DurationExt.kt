package io.github.bgavyus.lightningcamera.extensions.java.time

import java.time.Duration

fun Duration.format(): String {
    val secondsPart = seconds % 60
    val minutesPart = toMinutes() % 60
    val hoursPart = toHours()

    val secondsString = secondsPart.toString().padStart(2, '0')

    if (hoursPart == 0L) {
        return "$minutesPart:$secondsString"
    }

    val minutesString = minutesPart.toString().padStart(2, '0')
    return "$hoursPart:$minutesString:$secondsString"
}
