package io.github.bgavyus.splash.permissions

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.util.Log
import io.github.bgavyus.splash.storage.Storage
import java.util.*

abstract class PermissionsActivity : Activity() {
    companion object {
        private val TAG = PermissionsActivity::class.simpleName

        private const val REQUEST_PERMISSIONS_CODE = 0
    }

    fun allPermissionsGranted(): Boolean {
        return cameraPermissionsGranted() && storagePermissionsGranted()
    }

    fun requestNonGrantedPermissions() {
        val permissions = ArrayList<String>()

        if (!cameraPermissionsGranted()) {
            permissions.add(Manifest.permission.CAMERA)
        }

        if (!storagePermissionsGranted()) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        requestPermissions(
            permissions.toTypedArray(),
            REQUEST_PERMISSIONS_CODE
        )
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
            return onPermissionDenied(PermissionGroup.Camera)
        }

        if (!storagePermissionsGranted()) {
            return onPermissionDenied(PermissionGroup.Storage)
        }

        onAllPermissionsGranted()
    }

    private fun cameraPermissionsGranted(): Boolean {
        return checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun storagePermissionsGranted(): Boolean {
        if (Storage.scoped) {
            return true
        }

        return checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    abstract fun onPermissionDenied(group: PermissionGroup)
    abstract fun onAllPermissionsGranted()
}
