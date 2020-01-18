package io.github.bgavyus.splash

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.CamcorderProfile
import android.media.MediaCodec
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import kotlinx.android.synthetic.main.activity_viewfinder.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class ViewfinderActivity : Activity(), TextureView.SurfaceTextureListener, MediaRecorder.OnInfoListener,
	MediaRecorder.OnErrorListener {
	companion object {
		private val TAG = ViewfinderActivity::class.simpleName
		private const val VIDEO_MEDIA_TYPE = "video/hevc"
	}

	private lateinit var mVideoFile: ParcelFileDescriptor
	private lateinit var mTextureView: TextureView
	private lateinit var mPreviewSurface: Surface
	private lateinit var mFpsRange: Range<Int>
	private lateinit var mVideoSize: Size
	private val mRecorder = StatefulMediaRecorder()
	private lateinit var mRecorderSurface: Surface
	private lateinit var mVideoUri: Uri
	private lateinit var mDetector: LightningDetector

    override fun onCreate(savedInstanceState: Bundle?) {
		Log.d(TAG, "Activity.onCreate")
		super.onCreate(savedInstanceState)
		window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
		setContentView(R.layout.activity_viewfinder)
		mTextureView = camera_texture.apply {
			surfaceTextureListener = this@ViewfinderActivity
		}
	}

	override fun onStart() {
		Log.d(TAG, "Activity.onStart")
		super.onStart()
	}

	override fun onResume() {
		Log.d(TAG, "Activity.onResume")
		super.onResume()

		if (::mVideoSize.isInitialized) {
			setupRecorder()
		}
	}

	override fun onPause() {
        Log.d(TAG, "Activity.onPause")
        super.onPause()

		try {
			mRecorder.stop()
			mVideoFile.checkError()
			mVideoFile.close()
		}
		catch (ex: RuntimeException) {
			Log.e(TAG, "MediaRecorder RuntimeException", ex)
			mRecorder.reset()
			mVideoFile.close()
			contentResolver.delete(mVideoUri, null, null)
		}

		mRecorder.release()
		mPreviewSurface.release()
		mRecorderSurface.release()
	}

	override fun onStop() {
        Log.d(TAG, "Activity.onStop")
        super.onStop()
    }

	override fun onDestroy() {
		Log.d(TAG, "Activity.onDestroy")
		super.onDestroy()
	}

	@SuppressLint("MissingPermission")
	override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture?, width: Int, height: Int) {
		Log.d(TAG, "onSurfaceTextureAvailable")

		mPreviewSurface = Surface(surfaceTexture!!)
		val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
		val cameraId = cameraManager.cameraIdList.first {
			val characteristics = cameraManager.getCameraCharacteristics(it)
			val lensFacing = characteristics[CameraCharacteristics.LENS_FACING]!!
			return@first lensFacing == CameraMetadata.LENS_FACING_BACK
		}
		val characteristics = cameraManager.getCameraCharacteristics(cameraId)
		val configs = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!

        mFpsRange = configs.highSpeedVideoFpsRanges.maxBy { it.lower + it.upper }!!
		Log.i(TAG, "FPS Range: $mFpsRange")

        mVideoSize = configs.getHighSpeedVideoSizesFor(mFpsRange).maxBy { it.width * it.height }!!
		Log.i(TAG, "Video Size: $mVideoSize")

        surfaceTexture.setDefaultBufferSize(mVideoSize.width, mVideoSize.height)
		mRecorderSurface = MediaCodec.createPersistentInputSurface()

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

			val session = cameraCaptureSession as CameraConstrainedHighSpeedCaptureSession

			session.setRepeatingBurst(session.createHighSpeedRequestList(
				session.device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
					set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, mFpsRange)
					addTarget(mPreviewSurface)
					addTarget(mRecorderSurface)
				}.build()), null, null)
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
		mVideoUri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
			put(MediaStore.MediaColumns.MIME_TYPE, VIDEO_MEDIA_TYPE)
			put(MediaStore.MediaColumns.DISPLAY_NAME, "lightning_$timestamp.mp4")
		})!!

		mVideoFile = contentResolver.openFile(mVideoUri, "w", null)!!

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
			prepare()
			start()
			pause()
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

	private fun logCamcoderProfiles() {
		data class Quality(val speed: String, val name: String, val id: Int)

		val qualities = listOf(
            Quality("Normal Speed", "Low"  , CamcorderProfile.QUALITY_LOW),
            Quality("Normal Speed", "High" , CamcorderProfile.QUALITY_HIGH),
            Quality("Normal Speed", "QCIF" , CamcorderProfile.QUALITY_QCIF),
            Quality("Normal Speed", "CIF"  , CamcorderProfile.QUALITY_CIF),
            Quality("Normal Speed", "480p" , CamcorderProfile.QUALITY_480P),
            Quality("Normal Speed", "720p" , CamcorderProfile.QUALITY_720P),
            Quality("Normal Speed", "1080p", CamcorderProfile.QUALITY_1080P),
            Quality("Normal Speed", "QVGA" , CamcorderProfile.QUALITY_QVGA),
            Quality("Normal Speed", "2160p", CamcorderProfile.QUALITY_2160P),

			Quality("Time Lapse", "Low"  , CamcorderProfile.QUALITY_TIME_LAPSE_LOW),
            Quality("Time Lapse", "High" , CamcorderProfile.QUALITY_TIME_LAPSE_HIGH),
            Quality("Time Lapse", "QCIF" , CamcorderProfile.QUALITY_TIME_LAPSE_QCIF),
            Quality("Time Lapse", "CIF"  , CamcorderProfile.QUALITY_TIME_LAPSE_CIF),
            Quality("Time Lapse", "480p" , CamcorderProfile.QUALITY_TIME_LAPSE_480P),
            Quality("Time Lapse", "720p" , CamcorderProfile.QUALITY_TIME_LAPSE_720P),
            Quality("Time Lapse", "1080p", CamcorderProfile.QUALITY_TIME_LAPSE_1080P),
            Quality("Time Lapse", "QVGA" , CamcorderProfile.QUALITY_TIME_LAPSE_QVGA),
            Quality("Time Lapse", "2160p", CamcorderProfile.QUALITY_TIME_LAPSE_2160P),

			Quality("High Speed", "Low"  , CamcorderProfile.QUALITY_HIGH_SPEED_LOW),
            Quality("High Speed", "High" , CamcorderProfile.QUALITY_HIGH_SPEED_HIGH),
            Quality("High Speed", "480p" , CamcorderProfile.QUALITY_HIGH_SPEED_480P),
            Quality("High Speed", "720p" , CamcorderProfile.QUALITY_HIGH_SPEED_720P),
            Quality("High Speed", "1080p", CamcorderProfile.QUALITY_HIGH_SPEED_1080P),
            Quality("High Speed", "2160p", CamcorderProfile.QUALITY_HIGH_SPEED_2160P)
		)

        val outputFormats = mapOf(
            MediaRecorder.OutputFormat.DEFAULT to "Default",
            MediaRecorder.OutputFormat.THREE_GPP to "3GPP",
            MediaRecorder.OutputFormat.MPEG_4 to "MPEG4"
        )

        val videoEncoders = mapOf(
            MediaRecorder.VideoEncoder.DEFAULT to "Default",
            MediaRecorder.VideoEncoder.H263 to "H263",
            MediaRecorder.VideoEncoder.H264 to "H264",
            MediaRecorder.VideoEncoder.MPEG_4_SP to "MPEG4-SP",
            MediaRecorder.VideoEncoder.VP8 to "VP8",
            MediaRecorder.VideoEncoder.HEVC to "HEVC"
        )

        val audioEncoders = mapOf(
            MediaRecorder.AudioEncoder.DEFAULT to "Default",
            MediaRecorder.AudioEncoder.AMR_NB to "AMR-NB",
            MediaRecorder.AudioEncoder.AMR_WB to "AMR-WB",
            MediaRecorder.AudioEncoder.AAC to "AAC",
            MediaRecorder.AudioEncoder.HE_AAC to "HE-AAC",
            MediaRecorder.AudioEncoder.AAC_ELD to "AAC-ELD",
            MediaRecorder.AudioEncoder.VORBIS to "Ogg Vorbis",
            MediaRecorder.AudioEncoder.OPUS to "Opus"
        )

        val cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        Log.d(TAG, listOf(
            "Camera ID",
            "Speed",
            "Quality",
            "Duration",
            "File Format",
            "Video Codec",
            "Video Bit Rate",
            "Video Frame Rate",
            "Video Frame Width",
            "Video Frame Height",
            "Audio Codec",
            "Audio Bit Rate",
            "Audio Sample Rate",
            "Audio Channels"
		).joinToString(","))

        for (cameraId in cameraManager.cameraIdList.map(String::toInt)) {
            for (quality in qualities) {
                try {
                    val profile = CamcorderProfile.get(cameraId, quality.id)

                    Log.d(TAG, listOf(
                        cameraId,
                        quality.speed,
                        quality.name,
                        profile.duration,
                        outputFormats[profile.fileFormat],
                        videoEncoders[profile.videoCodec],
                        profile.videoBitRate,
                        profile.videoFrameRate,
                        profile.videoFrameWidth,
                        profile.videoFrameHeight,
                        audioEncoders[profile.audioCodec],
                        profile.audioBitRate,
                        profile.audioSampleRate,
                        profile.audioChannels
					).joinToString(","))
                }

                catch (ex: RuntimeException) { }
            }
        }
	}
}
