package com.browserengine.webview

import android.content.Context
import android.webkit.JavascriptInterface
import android.webkit.WebView
import com.browserengine.core.capabilities.MessagingBridgeCapable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class WebViewMessagingController(
    private val context: Context,
    private val webView: WebView
) {
    private val messageListeners = mutableListOf<MessagingBridgeCapable.MessageListener>()
    private var onErrorHandler: ((String) -> Unit)? = null
    private var onReadJsonHandler: ((String) -> Unit)? = null
    private var installed = false

    private val bridge = object {
        @JavascriptInterface
        fun onError(message: String) {
            onErrorHandler?.invoke(message)
            messageListeners.forEach { it.onMessage(message) }
        }

        @JavascriptInterface
        fun onReadJson(message: String) {
            onReadJsonHandler?.invoke(message)
            messageListeners.forEach { it.onMessage(message) }
        }
    }

    fun install() {
        if (installed) return
        installed = true
        webView.addJavascriptInterface(bridge, "Android")
    }

    fun setOnErrorHandler(handler: ((String) -> Unit)?) {
        onErrorHandler = handler
    }

    fun setOnReadJsonHandler(handler: ((String) -> Unit)?) {
        onReadJsonHandler = handler
    }

    fun addMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        messageListeners += listener
    }

    fun removeMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        messageListeners -= listener
    }

    suspend fun evaluateScript(script: String): Result<String> = withContext(Dispatchers.Main) {
        runCatching {
            suspendCancellableCoroutine<String> { cont ->
                webView.evaluateJavascript(script) { result ->
                    val value = when {
                        result == null -> "null"
                        result == "null" -> "null"
                        result.startsWith("\"") && result.endsWith("\"") -> result.drop(1).dropLast(1)
                        else -> result
                    }
                    cont.resume(value)
                }
            }
        }
    }

    suspend fun injectScriptFromAssets(assetPath: String): Result<Unit> = withContext(Dispatchers.Main) {
        runCatching {
            val script = context.assets.open(assetPath).bufferedReader().readText()
            webView.evaluateJavascript(script, null)
        }
    }

    suspend fun postMessageToJs(channel: String, data: String): Result<Unit> =
        evaluateScript("window.postMessage?.({channel:'$channel',data:'$data'},'*')").map { }

    fun postMessage(message: String) {
        val escaped = message
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace("\n", "\\n")
            .replace("\r", "\\r")

        webView.post {
            webView.evaluateJavascript(
                """
                (function() {
                    if (typeof window.onAndroidMessage === 'function') {
                        window.onAndroidMessage('$escaped');
                    }
                    window.postMessage({ type: 'FROM_ANDROID', data: '$escaped' }, '*');
                })();
                """.trimIndent(),
                null
            )
        }
    }
}
