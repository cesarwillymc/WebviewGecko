package com.example.webviewgecko

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browserengine.core.BrowserEngine
import com.browserengine.core.EngineType
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
    private val engineProvider: BrowserEngineProvider,
    private val permissionCoordinator: BrowserPermissionCoordinator,
    private val featureManager: BrowserFeatureOnDemandManager
) : ViewModel() {
    private val _engine = MutableStateFlow<BrowserEngine?>(null)
    val engine: StateFlow<BrowserEngine?> = _engine.asStateFlow()

    private val _bridgeLogs = MutableStateFlow<List<String>>(emptyList())
    val bridgeLogs: StateFlow<List<String>> = _bridgeLogs.asStateFlow()

    val pendingPermissionRequest: StateFlow<BrowserPermissionCoordinator.PendingPermissionRequest?> =
        permissionCoordinator.pendingPermissionRequest
    val featureSheetState: StateFlow<BrowserFeatureSheetState> = featureManager.sheetState

    init {
    }

    fun ensureEngine(type: EngineType, context: Context) {
        if (_engine.value != null) return

        viewModelScope.launch {
            runCatching {
                featureManager.downloadAndShowModal(type) {
                    _engine.value = engineProvider.build(context, type)
                }
            }.onFailure { error ->
                Log.e(LOG_TAG, "Failed to build engine", error)
            }
        }
    }

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
        val currentEngine = engine.value ?: return
        fun log(source: String, message: String) {
            Log.d(LOG_TAG, "[$source] $message")
            _bridgeLogs.update { it + "[$source] $message" }
        }

        currentEngine.setOnErrorHandler { msg -> log("onError", msg) }
        currentEngine.setOnReadJsonHandler { msg -> log("onReadJson", msg) }

        viewModelScope.launch {
            currentEngine.injectScript(Script.robinhood)
                .onSuccess {
                    log("inject", "Script.ibkr injected successfully")
                }
                .onFailure { e ->
                    log("inject", "Failed: ${e.message}")
                }
        }
    }

    fun sendExampleMessage() {
        val currentEngine = engine.value ?: return
        val exampleData = """{"message":"Hello from Android","timestamp":${System.currentTimeMillis()}}"""
        viewModelScope.launch {
            currentEngine.postMessageToJs("example", exampleData)
                .onSuccess {
                    Log.d(LOG_TAG, "postMessageToJs sent: channel=example, data=$exampleData")
                }
                .onFailure { e ->
                    Log.e(LOG_TAG, "postMessageToJs failed: ${e.message}")
                }
        }
    }

    override fun onCleared() {
        _engine.value?.destroy()
        super.onCleared()
    }
}
