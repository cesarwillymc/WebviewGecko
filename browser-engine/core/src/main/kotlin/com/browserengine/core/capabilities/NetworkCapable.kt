package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

interface NetworkCapable : BrowserCapability {
    /** Override User-Agent string. */
    fun setUserAgent(userAgent: String)
    fun getUserAgent(): String

    /** Set custom headers for all requests. */
    fun setDefaultHeaders(headers: Map<String, String>)

    /**
     * Intercept and optionally block/modify requests.
     * WebView: shouldInterceptRequest
     * Gecko: GeckoSession.ContentDelegate
     * Pass null to clear and allow all requests.
     */
    fun setRequestInterceptor(interceptor: RequestInterceptor?)

    /** Enable/disable JavaScript. */
    fun setJavaScriptEnabled(enabled: Boolean)

    /** Set mixed content policy. */
    fun setMixedContentMode(mode: MixedContentMode)
}

fun interface RequestInterceptor {
    fun intercept(request: BrowserRequest): InterceptResult
}

data class BrowserRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>
)

sealed class InterceptResult {
    object Allow : InterceptResult()
    object Block : InterceptResult()
    data class Redirect(val newUrl: String) : InterceptResult()
    data class Respond(val data: ByteArray, val mimeType: String) : InterceptResult() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Respond
            if (!data.contentEquals(other.data)) return false
            if (mimeType != other.mimeType) return false
            return true
        }

        override fun hashCode(): Int {
            var result = data.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            return result
        }
    }
}

enum class MixedContentMode {
    NEVER_ALLOW,
    COMPATIBILITY_MODE,
    ALWAYS_ALLOW
}
