package io.github.bgavyus.lightningcamera.extensions.android.content.res

import android.annotation.SuppressLint
import android.content.res.Resources

@SuppressLint("DiscouragedApi")
fun Resources.identifier(name: String, defType: String? = null, defPackage: String? = null) =
    getIdentifier(name, defType, defPackage).let { if (it == Resources.ID_NULL) null else it }
