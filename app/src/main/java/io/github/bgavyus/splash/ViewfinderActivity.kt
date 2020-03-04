package io.github.bgavyus.splash

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.util.Range
import android.util.Size
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_viewfinder.*
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.max


class ViewfinderActivity : Activity(), TextureView.SurfaceTextureListener,
    MediaRecorder.OnErrorListener, Thread.UncaughtExceptionHandler {

    companion object {
        private val TAG = ViewfinderActivity::class.simpleName

        private const val REQUEST_PERMISSIONS_CODE = 0
        private const val VIDEO_ENCODER = MediaRecorder.VideoEncoder.H264
        private const val VIDEO_MIME_TYPE = MediaFormat.MIMETYPE_VIDEO_AVC
        private const val VIDEO_OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4
        private const val VIDEO_FILE_EXTENSION = "mp4"
        private const val VIDEO_PLAYBACK_FPS = 5
        private const val BIT_RATE_FACTOR = 0.2
    }

    private val onReleaseCallbacks = ArrayDeque<() -> Unit>()
    private var videoFileHasValidContent = false

    private lateinit var recorder: StatefulMediaRecorder
    private lateinit var videoFile: PendingFile
    private lateinit var textureView: TextureView
    private lateinit var surface: Surface
    private lateinit var fpsRange: Range<Int>
    private lateinit var videoSize: Size
    private lateinit var detector: Detector

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "Activity.onCreate(savedInstanceState = $savedInstanceState)")
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility =
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
        setContentView(R.layout.activity_viewfinder)
        Thread.setDefaultUncaughtExceptionHandler(this)

        if (isStorageScoped()) {
            Log.d(TAG, "Running in scoped storage mode")
        } else {
            Log.d(TAG, "Running in legacy storage mode")
        }
    }

    override fun onResume() {
        Log.d(TAG, "Activity.onResume")
        super.onResume()
        initPermissions()
    }

    private fun initPermissions() {
        if (!allPermissionsGranted()) {
            Log.i(TAG, "Requesting permissions")
            requestNonGrantedPermissions()
            return
        }

        initSurfaceTexture()
    }

    private fun allPermissionsGranted(): Boolean {
        return cameraPermissionsGranted() && storagePermissionsGranted()
    }

    private fun requestNonGrantedPermissions() {
        val permissions = ArrayList<String>()

        if (!cameraPermissionsGranted()) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (!storagePermissionsGranted()) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        Log.d(
            TAG,
            "onRequestPermissionsResult(requestCode = $requestCode, permissions = ${permissions.joinToString()}, grantResults = ${grantResults.joinToString()})"
        )
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode != REQUEST_PERMISSIONS_CODE) {
            Log.w(TAG, "Got unknown request permission result: $requestCode")
            return
        }

        if (!cameraPermissionsGranted()) {
            return finishWithMessage(R.string.error_camera_permission_not_granted)
        }

        if (!storagePermissionsGranted()) {
            return finishWithMessage(R.string.error_storage_permission_not_granted)
        }

        onAllPermissionGranted()
    }

    private fun cameraPermissionsGranted(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun storagePermissionsGranted(): Boolean {
        if (isStorageScoped()) {
            return true
        }

        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun onAllPermissionGranted() {
        Log.d(TAG, "onAllPermissionGranted")
        recreate()
    }

    private fun initSurfaceTexture() {
        texture_view.run {
            textureView = this

            if (isAvailable) {
                Log.d(TAG, "TextureView.isAvailable")
                initSurface()
            } else {
                surfaceTextureListener = this@ViewfinderActivity
            }
        }
    }

    override fun onSurfaceTextureAvailable(
        surfaceTexture: SurfaceTexture?,
        width: Int,
        height: Int
    ) {
        Log.d(TAG, "onSurfaceTextureAvailable(width = $width, height = $height)")
        assert(surfaceTexture == textureView.surfaceTexture)
        textureView.surfaceTextureListener = null
        initSurface()
    }

    private fun initSurface() {
        try {
            surface = Surface(textureView.surfaceTexture)
        } catch (_: Surface.OutOfResourcesException) {
            return finishWithMessage(R.string.error_out_of_resources)
        }

        initCamera()
    }

    @SuppressLint("MissingPermission")
    private fun initCamera() {
        val cameraManager = getSystemService(CameraManager::class.java)
            ?: return finishWithMessage(R.string.error_camera_generic)

        try {
            val cameraId = getHighSpeedCameraId(cameraManager)
                ?: return finishWithMessage(R.string.error_high_speed_camera_not_available)

            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            val configs = characteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]
                ?: return finishWithMessage(R.string.error_camera_generic)

            fpsRange = configs.highSpeedVideoFpsRanges.maxBy { it.lower + it.upper }
                ?: return finishWithMessage(R.string.error_high_speed_camera_not_available)

            Log.i(TAG, "FPS Range: $fpsRange")

            videoSize = configs.getHighSpeedVideoSizesFor(fpsRange).maxBy { it.width * it.height }
                ?: return finishWithMessage(R.string.error_high_speed_camera_not_available)

            Log.i(TAG, "Video Size: $videoSize")

            cameraManager.openCamera(cameraId, cameraDeviceStateCallback, null)
        } catch (error: CameraAccessException) {
            return finishWithMessage(cameraAccessExceptionToResourceId(error))
        }
    }

    private fun getHighSpeedCameraId(cameraManager: CameraManager): String? {
        return cameraManager.cameraIdList.firstOrNull {
            val characteristics = cameraManager.getCameraCharacteristics(it)
            val capabilities = characteristics[CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES]
                ?: return@firstOrNull false

            return@firstOrNull CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO in capabilities
        }
    }

    private val cameraDeviceStateCallback = object : CameraDevice.StateCallback() {
        override fun onOpened(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.onOpened")
            registerOnReleaseCallback(camera::close)

            if (!textureView.isShown) {
                Log.d(TAG, "Not shown while in onOpened")
                return
            }

            initDetector()
            initSurfaceTextureListener()
            initVideoFile()
            initRecorder()
            setSurfaceTextureSize()

            try {
                camera.createConstrainedHighSpeedCaptureSession(
                    listOf(surface, recorder.surface),
                    cameraCaptureSessionStateCallback,
                    null
                )
            } catch (error: CameraAccessException) {
                return finishWithMessage(cameraAccessExceptionToResourceId(error))
            }
        }

        override fun onDisconnected(camera: CameraDevice) {
            Log.d(TAG, "CameraDevice.onDisconnected")
            camera.close()
        }

        override fun onError(camera: CameraDevice, error: Int) {
            Log.d(TAG, "CameraDevice.onError(error = $error)")

            val resourceId = when (error) {
                ERROR_CAMERA_IN_USE -> R.string.error_camera_in_use
                ERROR_MAX_CAMERAS_IN_USE -> R.string.error_max_cameras_in_use
                ERROR_CAMERA_DISABLED -> R.string.error_camera_disabled
                ERROR_CAMERA_DEVICE -> R.string.error_camera_device
                else -> R.string.error_camera_generic
            }

            finishWithMessage(resourceId)
        }
    }

    private fun initDetector() {
        detector = LightningDetector(
            this,
            textureView,
            videoSize
        ).apply { registerOnReleaseCallback(::release) }
    }

    private fun initSurfaceTextureListener() {
        textureView.run {
            surfaceTextureListener = this@ViewfinderActivity
            registerOnReleaseCallback {
                Log.d(TAG, "Removing surfaceTextureListener")
                surfaceTextureListener = null
            }
        }
    }

    private fun initVideoFile() {
        val standardDirectory = StandardDirectory.Movies
        val appDirName = getString(R.string.video_folder_name)
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val fileName = "${getString(R.string.video_file_prefix)}_$timestamp.$VIDEO_FILE_EXTENSION"
        videoFileHasValidContent = false

        try {
            videoFile = if (isStorageScoped()) {
                PendingScopedStorageFile(
                    context = this,
                    mimeType = VIDEO_MIME_TYPE,
                    standardDirectory = standardDirectory,
                    appDirName = appDirName,
                    name = fileName
                )
            } else {
                PendingLegacyStorageFile(
                    context = this,
                    mimeType = VIDEO_MIME_TYPE,
                    standardDirectory = standardDirectory,
                    appDirName = appDirName,
                    name = fileName
                )
            }
        } catch (_: IOException) {
            return finishWithMessage(R.string.error_io)
        }

        registerOnReleaseCallback {
            Log.d(TAG, "Save or discard video")

            if (videoFileHasValidContent) {
                Log.i(TAG, "Saving video")
                videoFile.save()
            } else {
                videoFile.discard()
            }
        }
    }

    private fun isStorageScoped(): Boolean {
        if (Build.VERSION.SDK_INT < 29) {
            return false
        }

        return !Environment.isExternalStorageLegacy()
    }

    private fun initRecorder() {
        recorder = StatefulMediaRecorder().apply {
            setOnErrorListener(this@ViewfinderActivity)

            assert(state == RecorderState.Initial)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)

            assert(state == RecorderState.Initialized)
            setOutputFormat(VIDEO_OUTPUT_FORMAT)
            setVideoFrameRate(VIDEO_PLAYBACK_FPS)
            setVideoSize(videoSize.width, videoSize.height)

            val encodingBitRate =
                (BIT_RATE_FACTOR * fpsRange.upper * videoSize.width * videoSize.height).toInt()
            Log.d(TAG, "Encoding Bit Rate: $encodingBitRate")
            setVideoEncodingBitRate(encodingBitRate)

            setCaptureRate(fpsRange.upper.toDouble())
            setOrientationHint(
                surfaceRotationToDegrees(
                    displayRotationToRecorderRotation(
                        windowManager.defaultDisplay.rotation
                    )
                )
            )

            assert(state == RecorderState.DataSourceConfigured)
            setVideoEncoder(VIDEO_ENCODER)
            setOutputFile(videoFile.descriptor)
            prepare()

            registerOnReleaseCallback {
                Log.d(TAG, "Releasing MediaRecorder")

                if (state == RecorderState.Recording || state == RecorderState.Paused) {
                    Log.d(TAG, "Recording or Paused")

                    try {
                        stop()
                    } catch (_: RuntimeException) {
                        Log.d(TAG, "MediaRecorder.stop RuntimeException")
                        videoFileHasValidContent = false
                    }
                }

                release()
            }
        }
    }

    private fun displayRotationToRecorderRotation(displayRotation: Int): Int {
        return when (displayRotation) {
            Surface.ROTATION_0 -> Surface.ROTATION_90
            Surface.ROTATION_90 -> Surface.ROTATION_0
            Surface.ROTATION_180 -> Surface.ROTATION_270
            Surface.ROTATION_270 -> Surface.ROTATION_180
            else -> throw IllegalArgumentException("Invalid display rotation")
        }
    }

    private fun surfaceRotationToDegrees(rotation: Int): Int {
        return when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> throw IllegalArgumentException("Invalid surface rotation")
        }
    }

    private val cameraCaptureSessionStateCallback = object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.onConfigured")

            if (!textureView.isShown) {
                Log.d(TAG, "Not shown while in onConfigured")
                return
            }

            setSurfaceTextureSize()
            applyTransform()
            startCaptureSession(cameraCaptureSession as CameraConstrainedHighSpeedCaptureSession)
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {
            Log.d(TAG, "CameraCaptureSession.onConfigureFailed")
        }
    }

    private fun startCaptureSession(captureSession: CameraConstrainedHighSpeedCaptureSession) {
        try {
            captureSession.run {
                setRepeatingBurst(
                    createHighSpeedRequestList(
                        device.createCaptureRequest(CameraDevice.TEMPLATE_RECORD).apply {
                            set(CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE, fpsRange)
                            addTarget(surface)
                            addTarget(recorder.surface)
                        }.build()
                    ), null, null
                )

                registerOnReleaseCallback(::close)
            }
        } catch (error: CameraAccessException) {
            return finishWithMessage(cameraAccessExceptionToResourceId(error))
        } catch (_: IllegalStateException) {
            return finishWithMessage(R.string.error_camera_generic)
        }
    }

    private fun cameraAccessExceptionToResourceId(error: CameraAccessException): Int {
        return when (error.reason) {
            CameraAccessException.CAMERA_IN_USE -> R.string.error_camera_in_use
            CameraAccessException.MAX_CAMERAS_IN_USE -> R.string.error_max_cameras_in_use
            CameraAccessException.CAMERA_DISABLED -> R.string.error_camera_disabled
            CameraAccessException.CAMERA_ERROR -> R.string.error_camera_device
            else -> R.string.error_camera_generic
        }
    }

    private fun registerOnReleaseCallback(cb: () -> Unit) {
        onReleaseCallbacks.push(cb)
    }

    override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture?) {
        handleFrame()
    }

    private fun handleFrame() {
        val detected = detector.detected()
        val recording = recorder.state == RecorderState.Recording

        if (detected && !recording) {
            record()
        }

        if (recording && !detected) {
            loss()
        }
    }

    private fun record() {
        Log.i(TAG, "Recording")

        if (recorder.state == RecorderState.Prepared) {
            Log.d(TAG, "Recorder state is Prepared")
            recorder.start()
            videoFileHasValidContent = true
        } else {
            recorder.resume()
        }
    }

    private fun loss() {
        Log.i(TAG, "Losing")
        recorder.pause()
    }

    override fun onSurfaceTextureSizeChanged(
        surfaceTexture: SurfaceTexture?,
        width: Int,
        height: Int
    ) {
        Log.d(TAG, "onSurfaceTextureSizeChanged(width = $width, height = $height)")
        assert(surfaceTexture == textureView.surfaceTexture)
        setSurfaceTextureSize()
        applyTransform()
    }

    private fun setSurfaceTextureSize() {
        textureView.surfaceTexture.setDefaultBufferSize(videoSize.width, videoSize.height)
    }

    private fun applyTransform() {
        val viewSize = Size(textureView.width, textureView.height)
        val matrix = getTransformMatrix(viewSize, videoSize, windowManager.defaultDisplay.rotation)
        textureView.setTransform(matrix)
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
            val scale = max(
                viewSize.height.toFloat() / bufferSize.height,
                viewSize.width.toFloat() / bufferSize.width
            )
            matrix.postScale(scale, scale, centerX, centerY)
            matrix.postRotate(90f * (rotation - 2), centerX, centerY)
        } else if (Surface.ROTATION_180 == rotation) {
            matrix.postRotate(180f, centerX, centerY)
        }

        return matrix
    }

    override fun onError(mr: MediaRecorder?, what: Int, extra: Int) {
        Log.d(TAG, "MediaRecorder.onError(what = $what, extra = $extra)")
        finishWithMessage(R.string.error_recorder)
    }

    override fun onSurfaceTextureDestroyed(surfaceTexture: SurfaceTexture?): Boolean {
        Log.d(TAG, "onSurfaceTextureDestroyed")
        assert(surfaceTexture == textureView.surfaceTexture)
        return true
    }

    override fun onPause() {
        Log.d(TAG, "Activity.onPause")
        super.onPause()
        release()
    }

    override fun uncaughtException(t: Thread, e: Throwable) {
        Log.e(TAG, "Uncaught Exception from $t", e)
        finishWithMessage(R.string.error_uncaught)
    }

    private fun finishWithMessage(resourceId: Int) {
        Log.d(TAG, "finishWithMessage: ${getDefaultString(resourceId)}")
        showMessage(resourceId)
        finish()
    }

    private fun getDefaultString(resourceId: Int): String {
        return createConfigurationContext(Configuration().apply { setLocale(Locale.ROOT) }).getString(
            resourceId
        )
    }

    private fun showMessage(resourceId: Int) {
        Thread {
            Looper.prepare()
            Toast.makeText(applicationContext, resourceId, Toast.LENGTH_LONG).run {
                setGravity(Gravity.CENTER, 0, 0)
                show()
            }
            Looper.loop()
        }.start()
    }

    override fun finish() {
        Log.d(TAG, "finish")
        super.finish()
        release()
    }

    private fun release() {
        while (!onReleaseCallbacks.isEmpty()) {
            val callback = onReleaseCallbacks.pop()
            Log.d(TAG, "ReleaseCallback: $callback")

            try {
                callback.invoke()
            } catch (error: Throwable) {
                Log.w(TAG, "Exception while calling release callback", error)
            }
        }
    }
}
