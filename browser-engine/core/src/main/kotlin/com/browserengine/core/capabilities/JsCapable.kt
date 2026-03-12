package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

interface JsCapable : BrowserCapability {
    /**
     * Injects and evaluates a JS script.
     * WebView: evaluateJavascript()
     * Gecko: evaluateJavascript() + WebExtension for large scripts
     */
    suspend fun evaluateScript(script: String): Result<String>

    /**
     * Injects a script file from assets.
     * WebView: loads + evaluates
     * Gecko: installs as built-in WebExtension content script
     */
    suspend fun injectScriptFromAssets(assetPath: String): Result<Unit>

    /**
     * Registers a native function callable from JS.
     * WebView: addJavascriptInterface(@JavascriptInterface)
     * Gecko: WebExtension MessageDelegate + sendNativeMessage
     */
    fun registerNativeFunction(name: String, handler: (args: String) -> String)

    /**
     * Sends a message to already-injected JS.
     */
    suspend fun postMessageToJs(channel: String, data: String): Result<Unit>
}
