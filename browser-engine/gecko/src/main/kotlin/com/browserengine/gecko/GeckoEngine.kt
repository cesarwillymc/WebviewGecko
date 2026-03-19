package com.browserengine.gecko

import android.content.Context
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import com.browserengine.core.BrowserCapability
import com.browserengine.core.CapabilityRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.Loader
import kotlin.reflect.KClass

class GeckoEngine private constructor(
    private val registry: CapabilityRegistry,
    private val stateStore: GeckoStateStore,
    private val components: GeckoRuntimeBundle
) : BrowserEngine {

    override val state: StateFlow<BrowserState> = stateStore.state
    override val events: Flow<BrowserEvent> = stateStore.events

    val session: GeckoSession
        get() = components.session

    fun open() {
        components.open()
    }

    override fun loadUrl(url: String, headers: Map<String, String>) {
        val network = capability(com.browserengine.core.capabilities.NetworkCapable::class)
        val allHeaders = (network as? GeckoNetworkCapability)?.defaultHeaders.orEmpty() + headers
        if (allHeaders.isEmpty()) {
            session.loadUri(url)
        } else {
            session.load(Loader().uri(url).additionalHeaders(allHeaders))
        }
    }

    override fun loadHtml(html: String, baseUrl: String?) {
        session.load(Loader().data(html.toByteArray(Charsets.UTF_8), "text/html; charset=utf-8"))
    }

    override fun reload() = session.reload()

    override fun stopLoading() = session.stop()

    override fun destroy() = components.destroy()

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? = registry.get(type)

    class Builder(
        private val context: Context
    ) {
        private var config: BrowserConfig = BrowserConfig()
        private val installers = mutableListOf<GeckoCapabilityInstaller>()

        fun settings(config: BrowserConfig): Builder = apply {
            this.config = config
        }

        fun addCapability(installer: GeckoCapabilityInstaller): Builder = apply {
            installers += installer
        }

        fun addCapabilities(items: Iterable<GeckoCapabilityInstaller>): Builder = apply {
            installers += items
        }

        fun addDefaultCapabilities(): Builder = apply {
            addCapabilities(GeckoCapabilityInstallers.defaults())
        }

        fun build(): GeckoEngine {
            val stateStore = GeckoStateStore()
            val delegateHub = GeckoDelegateHub(stateStore)
            val components = GeckoRuntimeBundle(
                context = context,
                config = config,
                delegates = delegateHub
            )
            val registry = CapabilityRegistry()
            val messaging = GeckoMessagingController(components.runtime, components.session)
            val engine = GeckoEngine(
                registry = registry,
                stateStore = stateStore,
                components = components
            )

            delegateHub.attach(engine)

            val scope = GeckoCapabilityScope(
                engine = engine,
                config = config,
                registry = registry,
                components = components,
                delegates = delegateHub,
                messaging = messaging
            )

            installers.forEach { installer ->
                installer.install(scope)
            }

            engine.open()
            return engine
        }
    }
}
