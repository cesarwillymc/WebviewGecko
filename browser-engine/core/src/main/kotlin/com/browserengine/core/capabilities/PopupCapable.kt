package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserEngine

/**
 * Handles window.open() / window.close() lifecycle.
 *
 * Engine behavior differences:
 *
 * WebView:
 *   - onCreateWindow() fires → you must create a new WebView,
 *     attach a WebViewTransport to resultMsg, and send() it back.
 *     Without this the popup is silently suppressed.
 *   - onCloseWindow() fires when JS calls window.close() or the
 *     page navigates away from a popup it created.
 *   - Requires WebSettings.setSupportMultipleWindows(true).
 *
 * GeckoView:
 *   - ContentDelegate.onOpenWindow() fires with the target URI.
 *   - Return a new GeckoSession to allow the popup; return null
 *     to block it. GeckoView wires the session automatically —
 *     no manual transport like WebView.
 *   - onCloseWindow() fires when the popup calls window.close().
 *   - No extra settings flag needed; controlled purely via delegate.
 */
/** Handler invoked by the engine when a page calls window.open() */
fun interface PopupRequestHandler {
    fun onPopupRequested(
        opener: BrowserEngine,
        uri: String?,
        isUserGesture: Boolean,
        onAllow: (newEngine: BrowserEngine) -> Unit,
        onBlock: () -> Unit
    )
}

/** Handler invoked when a popup calls window.close() */
fun interface PopupCloseHandler {
    fun onPopupCloseRequested(engine: BrowserEngine)
}

interface PopupCapable : BrowserCapability {

    /**
     * Set the handler called when a page calls window.open().
     * The engine invokes this with:
     * - [opener] The engine that triggered the popup
     * - [uri] Requested URL, null if about:blank or JS-driven
     * - [isUserGesture] True if triggered by a real user action
     * - [onAllow] Call with a new [BrowserEngine] to allow the popup
     * - [onBlock] Call to suppress the popup entirely
     */
    fun setPopupRequestHandler(handler: PopupRequestHandler?)

    /**
     * Set the handler called when a popup calls window.close().
     */
    fun setPopupCloseHandler(handler: PopupCloseHandler?)

    /**
     * Whether to allow popups that were NOT triggered by a user
     * gesture (e.g. auto-opening ads). Defaults to false.
     */
    var allowBackgroundPopups: Boolean
}
