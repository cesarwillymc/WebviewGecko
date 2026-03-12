package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

/**
 * Capability for bidirectional messaging between native Android and web content.
 *
 * **WebView**: Uses [addJavascriptInterface][android.webkit.WebView.addJavascriptInterface].
 * The only ways for JS to send to Android are:
 * - `Android.onError(message)` — JSON error payload
 * - `Android.onReadJson(message)` — JSON data payload
 *
 * **GeckoView**: Uses WebExtension with content script and sendNativeMessage.
 */
interface MessagingBridgeCapable : BrowserCapability {

    /**
     * Object name exposed to JavaScript.
     * WebView: name passed to addJavascriptInterface (e.g. "Android").
     */
    val bridgeName: String
        get() = "Android"

    /**
     * Registers a handler for `Android.onError(message)`.
     * WebView: JS calls `Android.onError(json)` where json is a JSON string.
     */
    fun setOnErrorHandler(handler: ((jsonMessage: String) -> Unit)?)

    /**
     * Registers a handler for `Android.onReadJson(message)`.
     * WebView: JS calls `Android.onReadJson(json)` where json is a JSON string.
     */
    fun setOnReadJsonHandler(handler: ((jsonMessage: String) -> Unit)?)

    /**
     * Registers a generic listener (invoked for all messages; GeckoView uses this).
     */
    fun addMessageListener(listener: MessageListener)

    /**
     * Removes a previously registered message listener.
     */
    fun removeMessageListener(listener: MessageListener)

    /**
     * Listener for messages from JavaScript.
     */
    fun interface MessageListener {
        fun onMessage(message: String): String?
    }

    /**
     * Sends a message from Android to JavaScript.
     *
     * WebView: Injects JS that calls `window.onAndroidMessage?.(message)` if defined,
     * or posts to `window` via `postMessage({ type: 'FROM_ANDROID', data: message }, '*')`.
     * GeckoView: Content script receives and forwards to page via postMessage.
     *
     * @param message The string to send. Caller is responsible for escaping if needed.
     */
    fun postMessage(message: String)

    /**
     * Sends a message and optionally receives a response (GeckoView: via MessageDelegate return;
     * WebView: not supported, returns null).
     */
    suspend fun sendMessageAndReceive(message: String): String? = null
}
