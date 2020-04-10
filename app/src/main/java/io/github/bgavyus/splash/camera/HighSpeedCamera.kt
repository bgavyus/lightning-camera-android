package io.github.bgavyus.splash.camera

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.camera2.*
import android.util.Log
import android.util.Range
import android.util.Size
import io.github.bgavyus.splash.common.ReleaseQueue
import io.github.bgavyus.splash.common.Rotation


@SuppressLint("MissingPermission")
class HighSpeedCamera(context: Context, val listener: CameraEventListener) {
	companion object {
		private val TAG = HighSpeedCamera::class.simpleName
	}

	private val releaseQueue = ReleaseQueue()
	private val cameraManager: CameraManager
	private val cameraId: String

	val sensorOrientation: Rotation
	val fpsRange: Range<Int>
	val videoSize: Size

	init {
		try {
			cameraManager = context.getSystemService(CameraManager::class.java)
				?: throw CameraError(CameraErrorType.Generic)

			cameraId = cameraManager.cameraIdList.firstOrNull {
				val characteristics = cameraManager.getCameraCharacteristics(it)
				val capabilities = characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
					?: return@firstOrNull false

				return@firstOrNull CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities
			} ?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)

			val characteristics = cameraManager.getCameraCharacteristics(cameraId)

			val configs = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
				?: throw CameraError(CameraErrorType.Generic)

			sensorOrientation = characteristics[CameraCharacteristics.SENSOR_ORIENTATION]
				?.let { Rotation.fromDegrees(it) }
				?.also { Log.d(TAG, "Camera Orientation: $it") }
				?: throw CameraError(CameraErrorType.Generic)

			fpsRange = configs.highSpeedVideoFpsRanges.maxBy { it.lower + it.upper }
				?.also { Log.d(TAG, "FPS Range: $it") }
				?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)

			videoSize = configs.getHighSpeedVideoSizesFor(fpsRange).maxBy { it.width * it.height }
				?.also { Log.d(TAG, "Video Size: $it") }
				?: throw CameraError(CameraErrorType.HighSpeedNotAvailable)
		} catch (error: CameraAccessException) {
			throw CameraError(accessExceptionToErrorType(error))
		}
	}

	fun stream() {
		try {
			cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null)
		} catch (error: CameraAccessException) {
			throw CameraError(accessExceptionToErrorType(error))
		}
	}

	private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
		override fun onOpened(camera: CameraDevice) {
			Log.d(TAG, "CameraDevice.onOpened")
			releaseQueue.push(camera::close)
			val surfaces = listener.onCameraSurfacesNeeded()

			try {
				camera.createConstrainedHighSpeedCaptureSession(surfaces, cameraCaptureSessionStateCallback, null)
			} catch (error: CameraAccessException) {
				onError(accessExceptionToErrorType(error))
			}
		}

		override fun onDisconnected(camera: CameraDevice) {
			Log.d(TAG, "CameraDevice.onDisconnected")
			onError(CameraErrorType.Disconnected)
		}

		override fun onError(camera: CameraDevice, error: Int) {
			Log.d(TAG, "CameraDevice.onError(error = $error)")

			onError(when (error) {
				ERROR_CAMERA_IN_USE -> CameraErrorType.InUse
				ERROR_MAX_CAMERAS_IN_USE -> CameraErrorType.MaxInUse
				ERROR_CAMERA_DISABLED -> CameraErrorType.Disabled
				ERROR_CAMERA_DEVICE -> CameraErrorType.Device
				else -> CameraErrorType.Generic
			})
		}
	}

	private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
		override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
			Log.d(TAG, "CameraCaptureSession.onConfigured")
			startCaptureSession(cameraCaptureSession as CameraConstrainedHighSpeedCaptureSession)
			listener.onCameraStreaming()
		}

		override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
			Log.d(TAG, "CameraCaptureSession.onConfigureFailed")
			onError(CameraErrorType.ConfigureFailed)
		}
	}

	private fun startCaptureSession(captureSession: CameraConstrainedHighSpeedCaptureSession) {
		val surfaces = listener.onCameraSurfacesNeeded()

		try {
			captureSession.run {
				setRepeatingBurst(
					createHighSpeedRequestList(
						device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
							set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
							surfaces.forEach(::addTarget)
						}.build()
					), null, null
				)

				releaseQueue.push(::close)
			}
		} catch (error: CameraAccessException) {
			onError(accessExceptionToErrorType(error))
		} catch (_: IllegalStateException) {
			onError(CameraErrorType.Generic)
		}
	}

	private fun accessExceptionToErrorType(error: CameraAccessException): CameraErrorType {
		return when (error.reason) {
			CameraAccessException.CAMERA_IN_USE -> CameraErrorType.InUse
			CameraAccessException.MAX_CAMERAS_IN_USE -> CameraErrorType.MaxInUse
			CameraAccessException.CAMERA_DISABLED -> CameraErrorType.Disabled
			CameraAccessException.CAMERA_ERROR -> CameraErrorType.Device
			else -> CameraErrorType.Generic
		}
	}

	private fun onError(type: CameraErrorType) {
		release()
		listener.onCameraError(type)
	}

	fun release() {
		releaseQueue.releaseAll()
	}
}
