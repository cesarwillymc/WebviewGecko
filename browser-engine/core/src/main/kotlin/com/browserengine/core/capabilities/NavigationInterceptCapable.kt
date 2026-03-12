package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

/**
 * Controls whether a top-level navigation is handled by the engine
 * or overridden by the host app.
 *
 * This is SEPARATE from NetworkCapable.RequestInterceptor, which
 * operates at the sub-resource level (images, XHR, fonts, etc.).
 * This interface is only about main-frame navigations — link clicks,
 * form submissions, JS-driven location changes, redirects, etc.
 *
 * Engine behavior differences:
 *
 * WebView:
 *   - WebViewClient.shouldOverrideUrlLoading(view, request)
 *   - Return true  → WebView does nothing; host app handles it.
 *   - Return false → WebView loads the URL normally.
 *
 * GeckoView:
 *   - NavigationDelegate.onLoadRequest(session, request)
 *   - Return LOAD_REQUEST_HANDLED → Gecko stops; host handles it.
 *   - Return null                 → Gecko loads normally.
 *   - request.isRedirect and request.isDirectNavigation available.
 */
interface NavigationInterceptCapable : BrowserCapability {

    /**
     * Set the interceptor that decides how each navigation is handled.
     * Replace with null to clear and allow all navigations.
     */
    fun setNavigationInterceptor(interceptor: NavigationInterceptor?)

    /** Returns the currently active interceptor, or null if none set. */
    fun getNavigationInterceptor(): NavigationInterceptor?
}

/**
 * Decision maker for top-level navigations.
 */
fun interface NavigationInterceptor {
    fun intercept(request: NavigationRequest): NavigationResult
}

data class NavigationRequest(
    val url: String,
    val method: String,
    val isRedirect: Boolean,
    val isUserInitiated: Boolean,
    val trigger: NavigationTrigger
)

enum class NavigationTrigger {
    LINK_CLICK,
    FORM_SUBMISSION,
    JS_LOCATION,
    META_REFRESH,
    RELOAD,
    BACK_FORWARD,
    OTHER
}

sealed class NavigationResult {
    /** Let the engine load the URL. Default behavior. */
    object Allow : NavigationResult()

    /**
     * Block the navigation entirely. Engine stays on current page.
     * Use for blacklisted domains, ad URLs, etc.
     */
    object Block : NavigationResult()

    /**
     * Host app takes ownership. Engine does NOT load the URL.
     * Use for: deep links, custom schemes (myapp://), external
     * browser hand-off, Play Store links, tel://, mailto://, etc.
     */
    object ConsumedByApp : NavigationResult()

    /**
     * Redirect the engine to a different URL instead.
     * Useful for URL rewriting, forced HTTPS, canonical URL mapping.
     */
    data class Redirect(val newUrl: String) : NavigationResult()
}
