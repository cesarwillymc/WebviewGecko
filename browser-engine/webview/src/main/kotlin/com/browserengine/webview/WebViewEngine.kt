package com.browserengine.webview

import android.content.Context
import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import com.browserengine.core.CapabilityRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

class WebViewEngine private constructor(
    private val registry: CapabilityRegistry,
    private val stateStore: WebViewStateStore,
    private val components: WebViewRuntimeBundle
) : BrowserEngine {

    override val state: StateFlow<BrowserState> = stateStore.state
    override val events: Flow<BrowserEvent> = stateStore.events

    val webView: android.webkit.WebView
        get() = components.webView

    override fun loadUrl(url: String, headers: Map<String, String>) {
        val network = capability(com.browserengine.core.capabilities.NetworkCapable::class)
        val allHeaders = (network as? WebViewNetworkCapability)?.defaultHeaders.orEmpty() + headers
        if (allHeaders.isEmpty()) {
            webView.loadUrl(url)
        } else {
            webView.loadUrl(url, allHeaders)
        }
    }

    override fun loadHtml(html: String, baseUrl: String?) {
        webView.loadDataWithBaseURL(baseUrl, html, "text/html", "UTF-8", null)
    }

    override fun reload() = webView.reload()

    override fun stopLoading() = webView.stopLoading()

    override fun destroy() = components.destroy()

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? = registry.get(type)

    class Builder(
        private val context: Context
    ) {
        private var config: BrowserConfig = BrowserConfig()
        private val installers = mutableListOf<WebViewCapabilityInstaller>()

        fun settings(config: BrowserConfig): Builder = apply {
            this.config = config
        }

        fun addCapability(installer: WebViewCapabilityInstaller): Builder = apply {
            installers += installer
        }

        fun addCapabilities(items: Iterable<WebViewCapabilityInstaller>): Builder = apply {
            installers += items
        }

        fun addDefaultCapabilities(): Builder = apply {
            addCapabilities(WebViewCapabilityInstallers.defaults())
        }

        fun build(): WebViewEngine {
            val stateStore = WebViewStateStore()
            val delegates = WebViewDelegateHub(stateStore)
            val components = WebViewRuntimeBundle(
                context = context,
                config = config,
                delegates = delegates
            )
            val registry = CapabilityRegistry()
            val messaging = WebViewMessagingController(context, components.webView)
            val engine = WebViewEngine(
                registry = registry,
                stateStore = stateStore,
                components = components
            )

            delegates.attach(engine)

            val scope = WebViewCapabilityScope(
                engine = engine,
                config = config,
                registry = registry,
                components = components,
                delegates = delegates,
                messaging = messaging
            )

            installers.forEach { it.install(scope) }
            return engine
        }
    }
}
