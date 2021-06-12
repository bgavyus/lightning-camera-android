package io.github.bgavyus.lightningcamera.extensions.androidx.constraintlayout.widget

import android.util.LayoutDirection
import androidx.constraintlayout.widget.ConstraintLayout
import io.github.bgavyus.lightningcamera.utilities.Rotation

fun ConstraintLayout.LayoutParams.rotate(rotation: Rotation) {
    val directionFactor = if (layoutDirection == LayoutDirection.RTL) -1 else 1
    val directionalRotation = rotation * directionFactor
    repeat(directionalRotation.ordinal) { rotateClockwise() }
}

fun ConstraintLayout.LayoutParams.rotateClockwise() = run {
    var temp = topToTop
    topToTop = endToEnd
    endToEnd = bottomToBottom
    bottomToBottom = startToStart
    startToStart = temp

    temp = topToBottom
    topToBottom = endToStart
    endToStart = bottomToTop
    bottomToTop = startToEnd
    startToEnd = temp

    temp = topMargin
    topMargin = marginEnd
    marginEnd = bottomMargin
    bottomMargin = marginStart
    marginStart = temp
}
