package com.browserengine.core

import com.browserengine.core.capabilities.MixedContentMode

data class BrowserConfig(
    val javaScriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val cookiesEnabled: Boolean = true,
    val mixedContentMode: MixedContentMode = MixedContentMode.NEVER_ALLOW,
    val supportMultipleWindows: Boolean = false,
    val userAgent: String? = null
)

enum class EngineType {
    WEBVIEW,
    GECKO
}
