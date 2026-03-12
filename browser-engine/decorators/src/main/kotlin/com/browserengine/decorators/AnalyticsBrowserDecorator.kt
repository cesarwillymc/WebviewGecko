package com.browserengine.decorators

import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.reflect.KClass

/**
 * Analytics event for tracking browser behavior.
 */
sealed class AnalyticsEvent {
    data class PageStarted(val url: String) : AnalyticsEvent()
    data class PageFinished(val url: String, val success: Boolean) : AnalyticsEvent()
    data class UrlChanged(val url: String) : AnalyticsEvent()
    data class ErrorReceived(val code: Int, val message: String, val url: String) : AnalyticsEvent()
    data class LoadUrl(val url: String) : AnalyticsEvent()
    data class Reload(val url: String) : AnalyticsEvent()
}

/**
 * Callback for analytics events. Implement to send to your analytics backend.
 */
fun interface AnalyticsCallback {
    fun onEvent(event: AnalyticsEvent)
}

/**
 * Decorator that emits analytics events for page loads, errors, and navigation.
 * Use with Firebase, Mixpanel, or custom analytics.
 */
class AnalyticsBrowserDecorator(
    private val delegate: BrowserEngine,
    private val callback: AnalyticsCallback
) : BrowserEngine {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    init {
        scope.launch {
            delegate.events
                .onEach { event ->
                    when (event) {
                        is BrowserEvent.PageStarted -> callback.onEvent(AnalyticsEvent.PageStarted(event.url))
                        is BrowserEvent.PageFinished -> callback.onEvent(AnalyticsEvent.PageFinished(event.url, event.success))
                        is BrowserEvent.UrlChanged -> callback.onEvent(AnalyticsEvent.UrlChanged(event.url))
                        is BrowserEvent.ErrorReceived -> callback.onEvent(
                            AnalyticsEvent.ErrorReceived(event.error.code, event.error.message, event.error.url)
                        )
                        else -> { /* other events ignored for analytics */ }
                    }
                }
                .collect {}
        }
    }

    override val state: StateFlow<BrowserState> = delegate.state
    override val events: Flow<BrowserEvent> = delegate.events

    override fun loadUrl(url: String, headers: Map<String, String>) {
        callback.onEvent(AnalyticsEvent.LoadUrl(url))
        delegate.loadUrl(url, headers)
    }

    override fun loadHtml(html: String, baseUrl: String?) {
        delegate.loadHtml(html, baseUrl)
    }

    override fun reload() {
        callback.onEvent(AnalyticsEvent.Reload(state.value.url))
        delegate.reload()
    }

    override fun stopLoading() = delegate.stopLoading()
    override fun destroy() = delegate.destroy()

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? = delegate.capability(type)
}
