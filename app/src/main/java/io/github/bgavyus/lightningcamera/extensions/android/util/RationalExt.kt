package io.github.bgavyus.lightningcamera.extensions.android.util

import android.util.Rational

operator fun Rational.times(other: Rational) =
    Rational(numerator * other.numerator, denominator * other.denominator)

val Rational.reciprocal get() = Rational(denominator, numerator)
