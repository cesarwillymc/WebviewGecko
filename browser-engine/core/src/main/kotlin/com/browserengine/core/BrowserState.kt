package com.browserengine.core

data class BrowserState(
    val url: String = "",
    val title: String = "",
    val isLoading: Boolean = false,
    val progress: Float = 0f,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val error: BrowserError? = null,
    val securityInfo: SecurityInfo = SecurityInfo()
)

data class SecurityInfo(
    val isSecure: Boolean = false,
    val host: String = "",
    val issuer: String = ""
)

data class BrowserError(
    val code: Int,
    val message: String,
    val url: String
)
