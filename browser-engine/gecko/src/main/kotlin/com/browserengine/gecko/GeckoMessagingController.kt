package com.browserengine.gecko

import android.annotation.SuppressLint
import android.util.Log
import com.browserengine.core.capabilities.MessagingBridgeCapable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.WebExtension

class GeckoMessagingController(
    private val runtime: GeckoRuntime,
    private val session: GeckoSession
) {
    private val messageListeners = mutableListOf<MessagingBridgeCapable.MessageListener>()
    private val messagingPorts = mutableListOf<WebExtension.Port>()
    private val pendingMessages = mutableListOf<JSONObject>()

    private var installed = false
    private var onErrorHandler: ((String) -> Unit)? = null
    private var onReadJsonHandler: ((String) -> Unit)? = null

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

    @SuppressLint("WrongThread")
    fun install() {
        if (installed) return
        installed = true

        runtime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/messaging/",
            "messaging@bautopilot.bridge.com"
        ).accept(
            { extension ->
                if (extension == null) return@accept
                session.webExtensionController.setMessageDelegate(
                    extension,
                    createMessageDelegate(),
                    "browser"
                )
            },
            { error -> Log.e("GeckoEngine", "Messaging extension install failed", error) }
        )
    }

    suspend fun evaluateScript(script: String): Result<String> = withContext(Dispatchers.Main) {
        enqueueOrSend(
            JSONObject().apply {
                put("action", "evaluate")
                put("script", replaceSendMessageToHost(script))
            }
        ).map { "queued" }
    }

    suspend fun postMessageToJs(channel: String, data: String): Result<Unit> = withContext(Dispatchers.Main) {
        enqueueOrSend(
            JSONObject().apply {
                put("action", "postMessage")
                put("channel", channel)
                put("data", data)
            }
        ).map { Unit }
    }

    fun postMessage(message: String) {
        enqueue(JSONObject().apply { put("text", message) })
    }

    private fun enqueueOrSend(payload: JSONObject): Result<Unit> {
        if (messagingPorts.isEmpty()) {
            pendingMessages += payload
            return Result.success(Unit)
        }

        return runCatching {
            messagingPorts.forEach { it.postMessage(payload) }
        }.onFailure {
            pendingMessages += payload
        }
    }

    private fun enqueue(payload: JSONObject) {
        if (messagingPorts.isEmpty()) {
            pendingMessages += payload
            return
        }

        messagingPorts.forEach { port ->
            runCatching { port.postMessage(payload) }
                .onFailure { Log.w("GeckoEngine", "postMessage failed", it) }
        }
    }

    private fun flushPendingMessages() {
        if (messagingPorts.isEmpty()) return
        val iterator = pendingMessages.iterator()
        while (iterator.hasNext()) {
            val payload = iterator.next()
            runCatching {
                messagingPorts.forEach { it.postMessage(payload) }
                iterator.remove()
            }.onFailure {
                Log.w("GeckoEngine", "Failed to flush queued message", it)
                return
            }
        }
    }

    private fun createMessageDelegate() = object : WebExtension.MessageDelegate {
        override fun onConnect(port: WebExtension.Port) {
            port.setDelegate(object : WebExtension.PortDelegate {
                override fun onPortMessage(message: Any, port: WebExtension.Port) {
                    Log.d("GeckoEngine", "Port message: $message")
                }

                override fun onDisconnect(port: WebExtension.Port) {
                    messagingPorts.remove(port)
                }
            })
            messagingPorts += port
            flushPendingMessages()
        }

        override fun onMessage(
            nativeApp: String,
            message: Any,
            sender: WebExtension.MessageSender
        ): GeckoResult<Any>? {
            val text = when (message) {
                is JSONObject -> message.optString("text", message.optString("message", message.toString()))
                is String -> message
                else -> message.toString()
            }

            when ((message as? JSONObject)?.optString("method", "onReadJson") ?: "onReadJson") {
                "onError" -> onErrorHandler?.invoke(text)
                else -> onReadJsonHandler?.invoke(text)
            }

            val response = messageListeners.firstNotNullOfOrNull { it.onMessage(text) }
            return GeckoResult.fromValue(response?.let { JSONObject().put("reply", it) })
        }
    }

    private fun replaceSendMessageToHost(script: String): String {
        val functionStart = Regex("""function\s+sendMessageToHost\s*\([^)]*\)\s*\{""")
        val replacement = """
function sendMessageToHost(message, isError = false) {
    window.postMessage({
      type: "FROM_PAGE",
      method: isError ? "onError" : "onReadJson",
      message: message
    }, "*");
}
""".trimIndent()

        val result = StringBuilder()
        var searchFrom = 0

        while (true) {
            val match = functionStart.find(script, searchFrom) ?: break
            result.append(script, searchFrom, match.range.first)
            result.append(replacement)

            var depth = 0
            var index = match.range.last
            while (index < script.length) {
                when (script[index]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            searchFrom = index + 1
                            break
                        }
                    }
                }
                index++
            }
        }

        result.append(script, searchFrom, script.length)
        return result.toString()
    }
}
