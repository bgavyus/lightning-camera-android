package io.github.bgavyus.splash.common

import android.content.Context
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.os.Looper
import android.util.Size
import android.view.Gravity
import android.widget.Toast
import java.util.*
import kotlin.math.min

fun getDefaultString(context: Context, resourceId: Int): String {
	val config = Configuration().apply { setLocale(Locale.ROOT) }
	return context.createConfigurationContext(config).getString(resourceId)
}

fun showMessage(context: Context, resourceId: Int) {
	Thread {
		Looper.prepare()
		Toast.makeText(context, resourceId, Toast.LENGTH_LONG).run {
			setGravity(Gravity.CENTER, 0, 0)
			show()
		}
		Looper.loop()
	}.start()
}

fun getTransformMatrix(viewSize: Size, bufferSize: Size, rotation: Rotation): Matrix {
	val matrix = Matrix()
	val viewRect = RectF(0f, 0f, viewSize.width.toFloat(), viewSize.height.toFloat())
	val bufferRect = RectF(0f, 0f, bufferSize.height.toFloat(), bufferSize.width.toFloat())
	val centerX = viewRect.centerX()
	val centerY = viewRect.centerY()
	val scale = min(viewSize.width, viewSize.height).toFloat() / bufferSize.height

	bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
	matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
	matrix.postScale(scale, scale, centerX, centerY)
	matrix.postRotate(rotation.degrees.toFloat(), centerX, centerY)

	return matrix
}
