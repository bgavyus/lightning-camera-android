package io.github.bgavyus.splash

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_viewfinder.*
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class ViewfinderActivity : Activity(), TextureView.SurfaceTextureListener, MediaRecorder.OnInfoListener,
	MediaRecorder.OnErrorListener {
	companion object {
		private val TAG = ViewfinderActivity::class.simpleName
		private const val REQUEST_PERMISSIONS_CODE = 0
		private const val VIDEO_ENCODER = MediaRecorder.VideoEncoder.H264
		private const val VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
		private const val VIDEO_OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
		private const val VIDEO_FILE_EXTENSION = "mp4"
	}

	private val mRecorder = StatefulMediaRecorder()
	private val mRecorderSurface = MediaCodec.createPersistentInputSurface()
	private val mReleaseCallbacks = arrayListOf<()->Unit>(mRecorder::release, mRecorderSurface::release)

	private lateinit var mVideoFile: MediaStoreFile
	private lateinit var mViewfinderTextureView: TextureView
	private lateinit var mViewfinderSurface: Surface
	private lateinit var mFpsRange: Range<Int>
	private lateinit var mVideoSize: Size
	private lateinit var mDetector: LightningDetector

    override fun onCreate(savedInstanceState: Bundle?) {
		Log.d(TAG, "Activity.onCreate")
		super.onCreate(savedInstanceState)

		window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
		setContentView(R.layout.activity_viewfinder)

		prepareViewfinderTextureView()
		permissionCallback()
	}

	private fun registerOnReleaseCallback(cb: ()->Unit) {
		mReleaseCallbacks.add(cb)
	}

	private fun prepareViewfinderTextureView() {
		mViewfinderTextureView = viewfinder_texture_view
		mViewfinderTextureView.surfaceTexture = SurfaceTexture(false)
		registerOnReleaseCallback(mViewfinderTextureView.surfaceTexture::release)

		mViewfinderSurface = Surface(mViewfinderTextureView.surfaceTexture)
		registerOnReleaseCallback(mViewfinderSurface::release)
	}

	private fun permissionCallback() {
		if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			Log.i(TAG, "Camera permission granted")
			val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

			try {
				val cameraId = getHighSpeedCameraId(cameraManager)
				populateCameraProperties(cameraManager, cameraId)
				cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null)
			}

			catch (ex: NoSuchElementException) {
				return exitWithMessage(R.string.high_speed_camera_not_available)
			}
		}

		else {
			Log.i(TAG, "Requesting camera permission")
			requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSIONS_CODE)
		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		Log.d(TAG, "onRequestPermissionsResult(requestCode = $requestCode, permissions = $permissions, grantResults = $grantResults)")
		assert(requestCode == REQUEST_PERMISSIONS_CODE)
		assert(permissions.contentEquals(arrayOf(Manifest.permission.CAMERA)))

		if (grantResults.isEmpty() || grantResults.first() != PackageManager.PERMISSION_GRANTED) {
			return exitWithMessage(R.string.camera_permission_not_granted)
		}

		permissionCallback()
	}

	private fun exitWithMessage(resourceId: Int) {
		Toast.makeText(this, resourceId, Toast.LENGTH_LONG).show()
		finish()
	}

	private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
		override fun onOpened(camera: CameraDevice) {
			Log.d(TAG, "CameraDevice.onOpened")
			registerOnReleaseCallback(camera::close)

			mDetector = LightningDetector(this@ViewfinderActivity, mViewfinderTextureView, mVideoSize)
			registerOnReleaseCallback(mDetector::release)

			mViewfinderTextureView.surfaceTextureListener = this@ViewfinderActivity
			registerOnReleaseCallback {
				mViewfinderTextureView.surfaceTextureListener = null
			}

			setupRecorder()
			setViewfinderSize()
			camera.createConstrainedHighSpeedCaptureSession(listOf(mViewfinderSurface, mRecorderSurface), cameraCaptureSessionStateCallback, null)
		}

		override fun onDisconnected(camera: CameraDevice) {
			Log.d(TAG, "CameraDevice.onDisconnected")
			camera.close()
		}

		override fun onError(camera: CameraDevice, error: Int) {
			val errorMessage = when (error) {
				ERROR_CAMERA_IN_USE -> "Camera in use"
				ERROR_MAX_CAMERAS_IN_USE -> "Max cameras in use"
				ERROR_CAMERA_DISABLED -> "Camera disabled"
				ERROR_CAMERA_DEVICE -> "Camera device"
				else -> error.toString()
			}

			Log.d(TAG, "CameraDevice.onError(error = $errorMessage)")
			camera.close()
		}
	}

	private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
		override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
			Log.d(TAG, "CameraCaptureSession.onConfigured")
			setViewfinderSize()
			applyTransform()

			(cameraCaptureSession as CameraConstrainedHighSpeedCaptureSession).apply {
				setRepeatingBurst(createHighSpeedRequestList(
					device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
						set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFpsRange)
						addTarget(mViewfinderSurface)
						addTarget(mRecorderSurface)
					}.build()), null, null)

				registerOnReleaseCallback(::close)
			}
		}

		override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
			Log.d(TAG, "CameraCaptureSession.onConfigureFailed")
		}
	}

	override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
		Log.d(TAG, "onSurfaceTextureSizeChanged(width = $width, height = $height)")
		assert(surfaceTexture == mViewfinderTextureView.surfaceTexture)
		setViewfinderSize()
		applyTransform()
	}

	private fun applyTransform() {
		val matrix = getTransformMatrix(Size(mViewfinderTextureView.width, mViewfinderTextureView.height), mVideoSize, windowManager.defaultDisplay.rotation)
		mViewfinderTextureView.setTransform(matrix)
	}

	override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture?) {
		if (mDetector.hasLightning()) {
			if (mRecorder.state == State.Prepared || mRecorder.state == State.Paused) {
				Log.i(TAG, "Recording")

				if (mRecorder.state == State.Prepared) {
					mRecorder.start()
				} else {
					mRecorder.resume()
				}
			}
		} else if (mRecorder.state == State.Recording) {
			Log.i(TAG, "Pausing")
			mRecorder.pause()
		}
	}

	override fun onSurfaceTextureAvailable(surface: SurfaceTexture?, width: Int, height: Int) {
		Log.d(TAG, "onSurfaceTextureSizeChanged(width = $width, height = $height)")
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
		Log.d(TAG, "onSurfaceTextureDestroyed")
		return true
	}

	private fun getHighSpeedCameraId(cameraManager: CameraManager): String {
		return cameraManager.cameraIdList.first {
			val characteristics = cameraManager.getCameraCharacteristics(it)
			val capabilities = characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]!!
			return@first CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities
		}
	}

	private fun populateCameraProperties(cameraManager: CameraManager, cameraId: String) {
		val characteristics = cameraManager.getCameraCharacteristics(cameraId)
		val configs = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!

		mFpsRange = configs.highSpeedVideoFpsRanges.maxBy { it.lower + it.upper }!!
		Log.i(TAG, "FPS Range: $mFpsRange")

		mVideoSize = configs.getHighSpeedVideoSizesFor(mFpsRange).maxBy { it.width * it.height }!!
		Log.i(TAG, "Video Size: $mVideoSize")
	}

	private fun setViewfinderSize() {
		mViewfinderTextureView.surfaceTexture.setDefaultBufferSize(mVideoSize.width, mVideoSize.height)
	}

	private fun setupRecorder() {
		val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
		mVideoFile = MediaStoreFile(contentResolver,
			mode = "w",
			mimeType = VIDEO_MIME_TYPE,
			baseUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
//			relativePath = Paths.get(Environment.DIRECTORY_DCIM, getString(R.string.video_folder_name)).toString(),
			relativePath = Paths.get(Environment.DIRECTORY_MOVIES, getString(R.string.video_folder_name)).toString(),
			name = "${getString(R.string.video_file_prefix)}_$timestamp.$VIDEO_FILE_EXTENSION")

		mRecorder.apply {
			setOnInfoListener(this@ViewfinderActivity)
			setOnErrorListener(this@ViewfinderActivity)
			setVideoSource(MediaRecorder.VideoSource.SURFACE)
			setOutputFormat(VIDEO_OUTPUT_FORMAT)
			setInputSurface(mRecorderSurface)
			setOutputFile(mVideoFile.fileDescriptor)
			setVideoSize(mVideoSize.width, mVideoSize.height)
			setVideoFrameRate(29)
			setVideoEncodingBitRate(Int.MAX_VALUE)
			setVideoEncoder(VIDEO_ENCODER)
			setOrientationHint(displayToVideoRotation(windowManager.defaultDisplay.rotation))
			prepare()
		}

		registerOnReleaseCallback {
			if (mRecorder.state == State.Prepared) {
				Log.d(TAG, "MediaRecorder.state == Prepared")
				mVideoFile.close()

				Log.i(TAG, "Deleting empty capture file")
				mVideoFile.delete()
			} else {
				try {
					mRecorder.stop()
					mVideoFile.close()
				} catch (ex: RuntimeException) {
					Log.d(TAG, "MediaRecorder.stop RuntimeException")
					mVideoFile.close()

					Log.i(TAG, "Deleting corrupted capture file")
					mVideoFile.delete()
				}
			}

			mRecorder.release()
		}
	}

	override fun onStart() {
		Log.d(TAG, "Activity.onStart")
		super.onStart()
	}

	override fun onResume() {
		Log.d(TAG, "Activity.onResume")
		super.onResume()
	}

	override fun onPause() {
		Log.d(TAG, "Activity.onPause")
		super.onPause()
	}

	override fun onStop() {
		Log.d(TAG, "Activity.onStop")
		super.onStop()

		for (func in mReleaseCallbacks.asReversed()) {
			try {
				func.invoke()
			}

			catch (ex: Throwable) {
				Log.w(TAG, "Exception while destroying view", ex)
			}
		}

		finish()
	}

	override fun onDestroy() {
		Log.d(TAG, "Activity.onDestroy")
		super.onDestroy()
	}

	override fun onInfo(mr: MediaRecorder?, what: Int, extra: Int) {
        val errorMessage = when (what) {
            MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN -> "Unknown"
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED -> "Max Duration reached"
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED -> "Max file size reached"
            MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_APPROACHING -> "Max file size approaching"
            MediaRecorder.MEDIA_RECORDER_INFO_NEXT_OUTPUT_FILE_STARTED -> "Next output file started"
            else -> what.toString()
        }

        Log.d(TAG, "MediaRecorder.onInfo(what = $errorMessage, extra = $extra)")
    }

    override fun onError(mr: MediaRecorder?, what: Int, extra: Int) {
        Log.d(TAG, "MediaRecorder.onError(what = $what, extra = $extra)")
    }
}

private fun displayToVideoRotation(displayRotation: Int): Int {
	return when (displayRotation) {
		Surface.ROTATION_0   -> 90
		Surface.ROTATION_90  -> 0
		Surface.ROTATION_180 -> 270
		Surface.ROTATION_270 -> 180
		else -> throw RuntimeException("Invalid display rotation")
	}
}

private fun getTransformMatrix(viewSize: Size, bufferSize: Size, rotation: Int): Matrix {
	val matrix = Matrix()
	val viewRect = RectF(0f, 0f, viewSize.width.toFloat(), viewSize.height.toFloat())
	val bufferRect = RectF(0f, 0f, bufferSize.height.toFloat(), bufferSize.width.toFloat())
	val centerX = viewRect.centerX()
	val centerY = viewRect.centerY()

	if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
		bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
		matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
		val scale: Float = max(viewSize.height.toFloat() / bufferSize.height, viewSize.width.toFloat() / bufferSize.width)
		matrix.postScale(scale, scale, centerX, centerY)
		matrix.postRotate(90f * (rotation - 2), centerX, centerY)
	} else if (Surface.ROTATION_180 == rotation) {
		matrix.postRotate(180f, centerX, centerY)
	}

	return matrix
}
