package com.example.webviewgecko

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.browserengine.core.BrowserEngine
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MessagingBridgeCapable
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val LOG_TAG = "ScriptInjection"

@HiltViewModel
class BrowserViewModel @Inject constructor(
    val engine: BrowserEngine
) : ViewModel() {

    private val _bridgeLogs = MutableStateFlow<List<String>>(emptyList())
    val bridgeLogs: StateFlow<List<String>> = _bridgeLogs.asStateFlow()

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
            js.evaluateScript(Script.ibkr)
                .onSuccess {
                    log("inject", "Script.ibkr injected successfully")
                }
                .onFailure { e ->
                    log("inject", "Failed: ${e.message}")
                }
        }
    }

    override fun onCleared() {
        engine.destroy()
        super.onCleared()
    }
}
