package io.github.bgavyus.lightningcamera.common

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class MessageShower @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun show(@StringRes message: Int) = Toast.makeText(context, message, Toast.LENGTH_LONG).show()
}
