package com.browserengine.decorators

import android.util.Log
import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Decorator that logs all events and method calls to Android Log.
 * Useful for debugging and tracing browser behavior.
 */
class LoggingBrowserDecorator(
    private val delegate: BrowserEngine,
    private val tag: String = "BrowserEngine"
) : BrowserEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            delegate.events
                .onEach { event -> Log.d(tag, "Event: $event") }
                .catch { e -> Log.e(tag, "Events flow error", e) }
                .collect {}
        }
    }

    override val state: StateFlow<BrowserState> = delegate.state
    override val events: Flow<BrowserEvent> = delegate.events

    override fun loadUrl(url: String, headers: Map<String, String>) {
        Log.d(tag, "loadUrl: $url, headers: $headers")
        delegate.loadUrl(url, headers)
    }

    override fun loadHtml(html: String, baseUrl: String?) {
        Log.d(tag, "loadHtml: baseUrl=$baseUrl, htmlLength=${html.length}")
        delegate.loadHtml(html, baseUrl)
    }

    override fun reload() {
        Log.d(tag, "reload")
        delegate.reload()
    }

    override fun stopLoading() {
        Log.d(tag, "stopLoading")
        delegate.stopLoading()
    }

    override fun destroy() {
        Log.d(tag, "destroy")
        delegate.destroy()
    }

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? {
        val cap = delegate.capability(type)
        Log.v(tag, "capability(${type.simpleName}): ${if (cap != null) "available" else "null"}")
        return cap
    }
}
