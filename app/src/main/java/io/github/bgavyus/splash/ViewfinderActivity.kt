package io.github.bgavyus.splash

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaCodec
import android.media.MediaRecorder
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_viewfinder.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class ViewfinderActivity : Activity(), TextureView.SurfaceTextureListener, MediaRecorder.OnInfoListener,
	MediaRecorder.OnErrorListener {
	companion object {
		private val TAG = ViewfinderActivity::class.simpleName
		private val REQUEST_PERMISSIONS_CODE = 0
	}

	private lateinit var mVideoFile: MediaStoreFile
	private lateinit var mTextureView: TextureView
	private lateinit var mPreviewSurface: Surface
	private lateinit var mFpsRange: Range<Int>
	private lateinit var mVideoSize: Size
	private val mRecorder = StatefulMediaRecorder()
	private val mRecorderSurface = MediaCodec.createPersistentInputSurface()
	private lateinit var mDetector: LightningDetector
	private lateinit var mCaptureSession: CameraConstrainedHighSpeedCaptureSession

    override fun onCreate(savedInstanceState: Bundle?) {
		Log.d(TAG, "Activity.onCreate")
		super.onCreate(savedInstanceState)
		window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
		setContentView(R.layout.activity_viewfinder)
		requestCameraPermission()
	}

	private fun requestCameraPermission() {
		if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
			onCameraPermissionGranted()
			return
		}

		requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_PERMISSIONS_CODE)
	}

	override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
		Log.d(TAG, "onRequestPermissionsResult(requestCode = $requestCode, permissions = $permissions, grantResults = $grantResults)")
		assert(requestCode == REQUEST_PERMISSIONS_CODE)
		assert(permissions.contentEquals(arrayOf(Manifest.permission.CAMERA)))

		if (grantResults.isEmpty() || grantResults.first() != PackageManager.PERMISSION_GRANTED) {
			Toast.makeText(this, R.string.camera_permission_not_granted, Toast.LENGTH_LONG).show()
			finish()
			return
		}

		onCameraPermissionGranted()
	}

	private fun onCameraPermissionGranted() {
		Log.d(TAG, "onCameraPermissionGranted")

		mTextureView = camera_texture.apply {
			surfaceTextureListener = this@ViewfinderActivity
		}

		if (mTextureView.isAvailable) {
			Log.d(TAG, "SurfaceTexture.isAvailable")
			onSurfaceTextureAvailable(mTextureView.surfaceTexture, mTextureView.width, mTextureView.height)
		}
	}

	@SuppressLint("MissingPermission")
	override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
		Log.d(TAG, "onSurfaceTextureAvailable")

		mPreviewSurface = Surface(surfaceTexture!!)

		val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

		val cameraId = selectCamera(cameraManager)

		surfaceTexture.setDefaultBufferSize(mVideoSize.width, mVideoSize.height)

		mDetector = LightningDetector(this, mTextureView, mVideoSize)

		setupRecorder()

		configureTransform(width, height)
		cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null)
	}

	private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
		override fun onOpened(camera: CameraDevice) {
			Log.d(TAG, "CameraDevice.onOpened")
			camera.createConstrainedHighSpeedCaptureSession(listOf(mPreviewSurface, mRecorderSurface), cameraCaptureSessionStateCallback, null)
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
			mCaptureSession = (cameraCaptureSession as CameraConstrainedHighSpeedCaptureSession).apply {
				setRepeatingBurst(createHighSpeedRequestList(
					device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
						set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFpsRange)
						addTarget(mPreviewSurface)
						addTarget(mRecorderSurface)
					}.build()), null, null)
			}
		}

		override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
			Log.d(TAG, "CameraCaptureSession.onConfigureFailed")
		}
	}

	override fun onSurfaceTextureSizeChanged(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
		Log.d(TAG, "onSurfaceTextureSizeChanged(width = $width, height = $height)")
		if (width != mVideoSize.width || height != mVideoSize.height) {
			surfaceTexture!!.setDefaultBufferSize(mVideoSize.width, mVideoSize.height)
		}

		configureTransform(width, height)
	}

	override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture?) {
		if (mDetector.hasLightning()) {
			if (mRecorder.state == State.Paused) {
				Log.d(TAG, "lightning detected and recorder paused")
				Log.i(TAG, "Resuming")
				mRecorder.resume()
			}
		} else if (mRecorder.state == State.Recording) {
			Log.d(TAG, "no lightning detected and recorder recording")
			Log.i(TAG, "Pausing")
			mRecorder.pause()
		}
	}

	override fun onSurfaceTextureDestroyed(surface: SurfaceTexture?): Boolean {
		Log.d(TAG, "onSurfaceTextureDestroyed")
		return true
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

//		try {
//			mRecorder.stop()
//			mVideoFile.close()
//		}
//		catch (ex: RuntimeException) {
//			Log.d(TAG, "MediaRecorder RuntimeException")
//			mVideoFile.close()
//
//			Log.i(TAG, "Cleaning corrupted capture file")
//			mVideoFile.delete()
//		}
//
//		mRecorder.release()
//		mCaptureSession.close()
//		mPreviewSurface.release()
//		mRecorderSurface.release()
	}

	override fun onStop() {
		Log.d(TAG, "Activity.onStop")
		super.onStop()
	}

	override fun onDestroy() {
		Log.d(TAG, "Activity.onDestroy")
		super.onDestroy()
	}

	private fun selectCamera(cameraManager: CameraManager): String {
		val cameraId = cameraManager.cameraIdList.first {
			val characteristics = cameraManager.getCameraCharacteristics(it)
			val capabilities =
				characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]!!
			return@first CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities
		}
		val characteristics = cameraManager.getCameraCharacteristics(cameraId)
		val configs = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!

		mFpsRange = configs.highSpeedVideoFpsRanges.maxBy { it.lower + it.upper }!!
		Log.i(TAG, "FPS Range: $mFpsRange")

		mVideoSize = configs.getHighSpeedVideoSizesFor(mFpsRange).maxBy { it.width * it.height }!!
		Log.i(TAG, "Video Size: $mVideoSize")

		return cameraId
	}

	private fun configureTransform(viewWidth: Int, viewHeight: Int) {
		val rotation = windowManager.defaultDisplay.rotation
		val matrix = Matrix()
		val viewRect = RectF(0F, 0F, viewWidth.toFloat(), viewHeight.toFloat())
		val bufferRect = RectF(0F, 0F, mVideoSize.height.toFloat(), mVideoSize.width.toFloat())
		val centerX = viewRect.centerX()
		val centerY = viewRect.centerY()

		if (Surface.ROTATION_90 == rotation || Surface.ROTATION_270 == rotation) {
			bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
			matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)
			val scale: Float = max(viewHeight.toFloat() / mVideoSize.height, viewWidth.toFloat() / mVideoSize.width)
			matrix.postScale(scale, scale, centerX, centerY)
			matrix.postRotate(90F * (rotation - 2), centerX, centerY)
		} else if (Surface.ROTATION_180 == rotation) {
			matrix.postRotate(180F, centerX, centerY)
		}
		mTextureView.setTransform(matrix)
	}

	private fun setupRecorder() {
		val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
		mVideoFile = MediaStoreFile(contentResolver,
			mode ="w",
			mimeType = "video/hevc",
			parent = MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
			name = "${getString(R.string.video_file_prefix)}_$timestamp.mp4")

		mRecorder.apply {
			setOnInfoListener(this@ViewfinderActivity)
			setOnErrorListener(this@ViewfinderActivity)
			setVideoSource(MediaRecorder.VideoSource.SURFACE)
			setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
			setInputSurface(mRecorderSurface)
			setOutputFile(mVideoFile.fileDescriptor)
			setVideoSize(mVideoSize.width, mVideoSize.height)
			setVideoFrameRate(mFpsRange.upper)
			setVideoEncodingBitRate(Int.MAX_VALUE)
			setVideoEncoder(MediaRecorder.VideoEncoder.HEVC)
			setOrientationHint(getVideoOrientation())
			prepare()
			start()
			pause()
		}
	}

	private fun getVideoOrientation(): Int {
		return when (windowManager.defaultDisplay.rotation) {
			Surface.ROTATION_0   -> 90
			Surface.ROTATION_90  -> 0
			Surface.ROTATION_180 -> 270
			Surface.ROTATION_270 -> 180
			else -> throw RuntimeException("Invalid rotation")
		}
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

        Log.d(TAG, "MediaRecorder.onInfo(mr = $mr, what = $errorMessage, extra = $extra)")
    }

    override fun onError(mr: MediaRecorder?, what: Int, extra: Int) {
        Log.d(TAG, "MediaRecorder.onError(mr = $mr, what = $what, extra = $extra)")
    }
}
