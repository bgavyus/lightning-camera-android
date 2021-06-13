package io.github.bgavyus.lightningcamera.extensions.androidx.constraintlayout.widget

import android.util.LayoutDirection
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateMarginsRelative
import io.github.bgavyus.lightningcamera.extensions.rotate
import io.github.bgavyus.lightningcamera.utilities.Rotation

fun ConstraintLayout.LayoutParams.rotate(rotation: Rotation) {
    val factor = if (layoutDirection == LayoutDirection.RTL) -1 else 1
    val distance = factor * rotation.ordinal
    rotateInnerConstrains(distance)
    rotateOuterConstrains(distance)
    rotateMarginsRelative(distance)
}

fun ConstraintLayout.LayoutParams.rotateInnerConstrains(distance: Int) {
    val (start, top, end, bottom) = listOf(startToStart, topToTop, endToEnd, bottomToBottom)
        .apply { rotate(distance) }

    startToStart = start
    topToTop = top
    endToEnd = end
    bottomToBottom = bottom
}

fun ConstraintLayout.LayoutParams.rotateOuterConstrains(distance: Int) {
    val (start, top, end, bottom) = listOf(startToEnd, topToBottom, endToStart, bottomToTop)
        .apply { rotate(distance) }

    startToEnd = start
    topToBottom = top
    endToStart = end
    bottomToTop = bottom
}

fun ConstraintLayout.LayoutParams.rotateMarginsRelative(distance: Int) {
    val (start, top, end, bottom) = listOf(marginStart, topMargin, marginEnd, bottomMargin)
        .apply { rotate(distance) }

    updateMarginsRelative(start, top, end, bottom)
}
