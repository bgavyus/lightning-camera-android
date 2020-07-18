package io.github.bgavyus.splash.permissions

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.commit
import kotlinx.coroutines.CompletableDeferred

class PermissionsRequester(
    private val permissions: Collection<String>,
    private val deferred: CompletableDeferred<Unit>
) : Fragment() {
    companion object {
        private val TAG = PermissionsRequester::class.simpleName

        private const val REQUEST_PERMISSIONS_CODE = 0

        suspend fun request(permissions: Collection<String>, fragmentManager: FragmentManager) {
            val deferred = CompletableDeferred<Unit>()
            val fragment = PermissionsRequester(permissions, deferred)

            fragmentManager.run {
                commit { add(fragment, PermissionsRequester::class.qualifiedName) }

                try {
                    deferred.await()
                } finally {
                    if (!isDestroyed) {
                        commit { remove(fragment) }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        retainInstance = true
        // TODO: Replace with registerForActivityResult
        requestPermissions(permissions.toTypedArray(), REQUEST_PERMISSIONS_CODE)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode != REQUEST_PERMISSIONS_CODE) {
            Log.w(TAG, "Got unknown request permission result: $requestCode")
            return
        }

        deferred.complete(Unit)
    }
}
