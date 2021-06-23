package io.github.bgavyus.lightningcamera.extensions.android.content.res

import android.content.res.Resources

fun Resources.identifier(name: String, defType: String? = null, defPackage: String? = null): Int? {
    val id = getIdentifier(name, defType, defPackage)
    return if (id == Resources.ID_NULL) null else id
}
