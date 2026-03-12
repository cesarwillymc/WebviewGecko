package com.browserengine.decorators

import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import com.browserengine.core.capabilities.NavigationInterceptCapable
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.NavigationRequest
import com.browserengine.core.capabilities.NavigationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass
import java.net.URL

/**
 * URL filter mode for SecurityBrowserDecorator.
 */
enum class SecurityMode {
    /** Only URLs whose host is in [allowedDomains] are allowed. */
    WHITELIST,

    /** URLs whose host is in [blockedDomains] are blocked. */
    BLACKLIST
}

/**
 * Decorator that enforces URL whitelist or blacklist via NavigationInterceptCapable.
 * Blocks navigations to disallowed domains before they load.
 */
class SecurityBrowserDecorator(
    private val delegate: BrowserEngine,
    private val mode: SecurityMode = SecurityMode.WHITELIST,
    private val allowedDomains: Set<String> = emptySet(),
    private val blockedDomains: Set<String> = emptySet()
) : BrowserEngine {

    init {
        delegate.capability(NavigationInterceptCapable::class)?.setNavigationInterceptor { request ->
            val host = runCatching { URL(request.url).host }.getOrNull() ?: return@setNavigationInterceptor NavigationResult.Block
            val normalizedHost = host.lowercase().removePrefix("www.")

            when (mode) {
                SecurityMode.WHITELIST -> {
                    val allowed = allowedDomains.any { normalizedHost == it.lowercase().removePrefix("www.") || normalizedHost.endsWith("." + it.lowercase().removePrefix("www.")) }
                    if (allowed) NavigationResult.Allow else NavigationResult.Block
                }
                SecurityMode.BLACKLIST -> {
                    val blocked = blockedDomains.any { normalizedHost == it.lowercase().removePrefix("www.") || normalizedHost.endsWith("." + it.lowercase().removePrefix("www.")) }
                    if (blocked) NavigationResult.Block else NavigationResult.Allow
                }
            }
        }
    }

    override val state: StateFlow<BrowserState> = delegate.state
    override val events: Flow<BrowserEvent> = delegate.events

    override fun loadUrl(url: String, headers: Map<String, String>) {
        val host = runCatching { URL(url).host }.getOrNull()
        if (host != null && !isUrlAllowed(host)) return
        delegate.loadUrl(url, headers)
    }

    override fun loadHtml(html: String, baseUrl: String?) = delegate.loadHtml(html, baseUrl)
    override fun reload() = delegate.reload()
    override fun stopLoading() = delegate.stopLoading()
    override fun destroy() = delegate.destroy()

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? = delegate.capability(type)

    private fun isUrlAllowed(host: String): Boolean {
        val normalized = host.lowercase().removePrefix("www.")
        return when (mode) {
            SecurityMode.WHITELIST -> allowedDomains.any { normalized == it.lowercase().removePrefix("www.") || normalized.endsWith("." + it.lowercase().removePrefix("www.")) }
            SecurityMode.BLACKLIST -> !blockedDomains.any { normalized == it.lowercase().removePrefix("www.") || normalized.endsWith("." + it.lowercase().removePrefix("www.")) }
        }
    }
}
