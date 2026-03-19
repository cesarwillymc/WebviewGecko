package com.example.webviewgecko

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val LOG_TAG = "BrowserPermission"

@Singleton
class BrowserPermissionCoordinator @Inject constructor() {

    data class PendingPermissionRequest(
        val permissions: List<String>,
        val onGrant: () -> Unit,
        val onDeny: () -> Unit
    )

    private val _pendingPermissionRequest = MutableStateFlow<PendingPermissionRequest?>(null)
    val pendingPermissionRequest: StateFlow<PendingPermissionRequest?> =
        _pendingPermissionRequest.asStateFlow()

    fun onPermissionRequested(
        permissions: List<String>,
        onGrant: () -> Unit,
        onDeny: () -> Unit
    ) {
        Log.d(LOG_TAG, "Permission requested: $permissions")
        _pendingPermissionRequest.value = PendingPermissionRequest(permissions, onGrant, onDeny)
    }

    fun grantPermission() {
        _pendingPermissionRequest.value?.let { req ->
            req.onGrant()
            _pendingPermissionRequest.value = null
            Log.d(LOG_TAG, "Permission granted: ${req.permissions}")
        }
    }

    fun completePermissionGrant(androidPermissionsGranted: Boolean) {
        _pendingPermissionRequest.value?.let { req ->
            if (androidPermissionsGranted) {
                req.onGrant()
                Log.d(LOG_TAG, "Permission granted after Android request: ${req.permissions}")
            } else {
                req.onDeny()
                Log.d(LOG_TAG, "Permission denied after Android request: ${req.permissions}")
            }
            _pendingPermissionRequest.value = null
        }
    }

    fun denyPermission() {
        _pendingPermissionRequest.value?.let { req ->
            req.onDeny()
            _pendingPermissionRequest.value = null
            Log.d(LOG_TAG, "Permission denied: ${req.permissions}")
        }
    }
}
