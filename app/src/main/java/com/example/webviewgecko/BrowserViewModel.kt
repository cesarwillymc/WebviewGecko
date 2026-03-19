package com.example.webviewgecko

import android.Manifest
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browserengine.core.BrowserEngine
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
    val engine: BrowserEngine,
    private val permissionCoordinator: BrowserPermissionCoordinator
) : ViewModel() {

    private val _bridgeLogs = MutableStateFlow<List<String>>(emptyList())
    val bridgeLogs: StateFlow<List<String>> = _bridgeLogs.asStateFlow()

    val pendingPermissionRequest: StateFlow<BrowserPermissionCoordinator.PendingPermissionRequest?> =
        permissionCoordinator.pendingPermissionRequest

    /** Call when user taps Grant and no Android runtime request is needed. */
    fun grantPermission() {
        permissionCoordinator.grantPermission()
    }

    /** Call after Android runtime permission request completes. */
    fun completePermissionGrant(androidPermissionsGranted: Boolean) {
        permissionCoordinator.completePermissionGrant(androidPermissionsGranted)
    }

    fun denyPermission() {
        permissionCoordinator.denyPermission()
    }
    fun startInjection() {
        fun log(source: String, message: String) {
            Log.d(LOG_TAG, "[$source] $message")
            _bridgeLogs.update { it + "[$source] $message" }
        }

        engine.setOnErrorHandler { msg -> log("onError", msg) }
        engine.setOnReadJsonHandler { msg -> log("onReadJson", msg) }

        viewModelScope.launch {
            engine.injectScript(Script.robinhood)
                .onSuccess {
                    log("inject", "Script.ibkr injected successfully")
                }
                .onFailure { e ->
                    log("inject", "Failed: ${e.message}")
                }
        }
    }

    fun sendExampleMessage() {
        val exampleData = """{"message":"Hello from Android","timestamp":${System.currentTimeMillis()}}"""
        viewModelScope.launch {
            engine.postMessageToJs("example", exampleData)
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
