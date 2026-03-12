package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

interface CookieCapable : BrowserCapability {
    /**
     * Get all cookies for a URL.
     * WebView: CookieManager.getCookie(url)
     * Gecko: GeckoRuntime storageController
     */
    suspend fun getCookies(url: String): List<BrowserCookie>

    /** Set a cookie for a URL. */
    suspend fun setCookie(url: String, cookie: BrowserCookie)

    /** Remove all cookies. */
    suspend fun clearCookies()

    /** Remove cookies for a specific URL. */
    suspend fun clearCookiesFor(url: String)

    /** Enable/disable third-party cookies. */
    fun setThirdPartyCookiesEnabled(enabled: Boolean)
}

data class BrowserCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String = "/",
    val isSecure: Boolean = false,
    val isHttpOnly: Boolean = false,
    val expiresAt: Long? = null
)
