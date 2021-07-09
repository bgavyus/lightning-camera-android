package io.github.bgavyus.lightningcamera.extensions.android.database

import android.database.Cursor

fun Cursor.requireMoveToPosition(position: Int) = check(moveToPosition(position))
