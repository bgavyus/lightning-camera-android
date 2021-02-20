package io.github.bgavyus.lightningcamera.ui

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import javax.inject.Inject

class MessageShower @Inject constructor(
    private val context: Context,
) {
    fun show(@StringRes message: Int) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
