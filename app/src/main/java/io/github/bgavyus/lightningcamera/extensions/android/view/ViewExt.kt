package io.github.bgavyus.lightningcamera.extensions.android.view

import android.view.View
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import io.github.bgavyus.lightningcamera.extensions.androidx.constraintlayout.widget.rotate
import io.github.bgavyus.lightningcamera.utilities.Rotation

fun View.rotateLayout(rotation: Rotation) = updateConstraintLayoutParams { rotate(rotation) }

inline fun View.updateConstraintLayoutParams(block: ConstraintLayout.LayoutParams.() -> Unit) =
    updateLayoutParams(block)
