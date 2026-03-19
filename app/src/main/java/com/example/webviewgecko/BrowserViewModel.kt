package com.example.webviewgecko

import android.Manifest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browserengine.core.BrowserEngine
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MessagingBridgeCapable
import com.browserengine.core.capabilities.PermissionCapable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOG_TAG = "ScriptInjection"

/** Maps engine permission strings to Android manifest permissions for runtime request. */
fun permissionStringsToAndroidManifest(permissions: List<String>): List<String> {
    val result = mutableSetOf<String>()
    for (p in permissions) {
        when (p) {
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION -> result.add(p)
            "android.webkit.resource.VIDEO_CAPTURE" -> result.add(Manifest.permission.CAMERA)
            "android.webkit.resource.AUDIO_CAPTURE" -> result.add(Manifest.permission.RECORD_AUDIO)
            "video" -> result.add(Manifest.permission.CAMERA)
            "audio" -> result.add(Manifest.permission.RECORD_AUDIO)
        }
    }
    return result.toList()
}

@HiltViewModel
class BrowserViewModel @Inject constructor(
    val engine: BrowserEngine
) : ViewModel() {

    private val _bridgeLogs = MutableStateFlow<List<String>>(emptyList())
    val bridgeLogs: StateFlow<List<String>> = _bridgeLogs.asStateFlow()

    data class PendingPermissionRequest(
        val permissions: List<String>,
        val onGrant: () -> Unit,
        val onDeny: () -> Unit
    )
    private val _pendingPermissionRequest = MutableStateFlow<PendingPermissionRequest?>(null)
    val pendingPermissionRequest: StateFlow<PendingPermissionRequest?> = _pendingPermissionRequest.asStateFlow()

    init {
        engine.capability(PermissionCapable::class)?.let {
            it.setPermissionRequestHandler { permissions, onGrant, onDeny ->
                Log.d(LOG_TAG, "Permission requested: $permissions")
                _pendingPermissionRequest.value = PendingPermissionRequest(permissions, onGrant, onDeny)
            }
        }
    }

    /** Call when user taps Grant and no Android runtime request is needed. */
    fun grantPermission() {
        _pendingPermissionRequest.value?.let { req ->
            req.onGrant()
            _pendingPermissionRequest.value = null
            Log.d(LOG_TAG, "Permission granted: ${req.permissions}")
        }
    }

    /** Call after Android runtime permission request completes. */
    fun completePermissionGrant(androidPermissionsGranted: Boolean) {
        _pendingPermissionRequest.value?.let { req ->
            if (androidPermissionsGranted) {
                req.onGrant()
                Log.d(LOG_TAG, "Permission granted (after Android request): ${req.permissions}")
            } else {
                req.onDeny()
                Log.d(LOG_TAG, "Permission denied (Android request failed): ${req.permissions}")
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
    fun startInjection() {
        val bridge = engine.capability(MessagingBridgeCapable::class) ?: return
        val js = engine.capability(JsCapable::class) ?: return

        fun log(source: String, message: String) {
            Log.d(LOG_TAG, "[$source] $message")
            _bridgeLogs.update { it + "[$source] $message" }
        }

        bridge.setOnErrorHandler { msg -> log("onError", msg) }
        bridge.setOnReadJsonHandler { msg -> log("onReadJson", msg) }

        viewModelScope.launch {
            js.evaluateScript(Script.robinhood)
                .onSuccess {
                    log("inject", "Script.ibkr injected successfully")
                }
                .onFailure { e ->
                    log("inject", "Failed: ${e.message}")
                }
        }
    }

    fun sendExampleMessage() {
        val js = engine.capability(JsCapable::class) ?: return
        val exampleData = """{"message":"Hello from Android","timestamp":${System.currentTimeMillis()}}"""
        viewModelScope.launch {
            js.postMessageToJs("example", exampleData)
                .onSuccess {
                    Log.d(LOG_TAG, "postMessageToJs sent: channel=example, data=$exampleData")
                }
                .onFailure { e ->
                    Log.e(LOG_TAG, "postMessageToJs failed: ${e.message}")
                }
        }
    }

    override fun onCleared() {
        engine.destroy()
        super.onCleared()
    }
}
