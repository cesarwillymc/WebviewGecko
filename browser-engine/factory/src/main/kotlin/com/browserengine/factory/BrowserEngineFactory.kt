package com.browserengine.factory

import android.content.Context
import android.util.Log
import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.EngineType
import com.browserengine.core.capabilities.NavigationCapable
import com.browserengine.core.capabilities.UICapable
import com.browserengine.decorators.AnalyticsBrowserDecorator
import com.browserengine.decorators.AnalyticsCallback
import com.browserengine.decorators.LoggingBrowserDecorator
import com.browserengine.decorators.SecurityBrowserDecorator
import com.browserengine.decorators.SecurityMode
import com.browserengine.webview.WebViewEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

private val defaultEngineCreators = mapOf<EngineType, EngineCreator>(
    EngineType.WEBVIEW to EngineCreator { context, config ->
        WebViewEngine.Builder(context)
            .settings(config)
            .addDefaultCapabilities()
            .build()
    }
)

data class DecoratorOptions(
    val logging: Boolean = false,
    val loggingTag: String = "BrowserEngine",
    val analytics: AnalyticsCallback? = null,
    val securityMode: SecurityMode? = null,
    val allowedDomains: Set<String> = emptySet(),
    val blockedDomains: Set<String> = emptySet()
)

object BrowserEngineFactory {

    class Builder(
        private val context: Context,
        private val type: EngineType
    ) {
        private var config: BrowserConfig = BrowserConfig()
        private var decoratorOptions: DecoratorOptions = DecoratorOptions()
        private val capabilities = mutableListOf<BrowserEngineCapability>()
        private var exposeAllCapabilities: Boolean = false
        private var featureValidator: EngineFeatureValidator? = null

        fun settings(): Builder = this

        fun settings(config: BrowserConfig): Builder = apply {
            this.config = config
        }

        fun decorators(options: DecoratorOptions): Builder = apply {
            this.decoratorOptions = options
        }

        fun featureValidator(validator: EngineFeatureValidator): Builder = apply {
            this.featureValidator = validator
        }

        fun addCapability(capability: BrowserEngineCapability): Builder = apply {
            capabilities += capability
        }

        fun exposeAllCapabilities(): Builder = apply {
            exposeAllCapabilities = true
        }

        fun build(): BrowserEngine {
            featureValidator?.validate(type)?.let { validation ->
                check(validation.isValid) {
                    validation.reason ?: "Engine feature validation failed for $type"
                }
            }

            val rawEngine = createBaseEngine(context, type, config)
            val enabledCapabilities = linkedSetOf<KClass<out BrowserCapability>>(
                UICapable::class,
                NavigationCapable::class
            )

            capabilities.forEach { capability ->
                enabledCapabilities += capability.apply(rawEngine)
            }

            val decoratedEngine = applyDecorators(rawEngine, decoratorOptions)
            return if (exposeAllCapabilities) {
                decoratedEngine
            } else {
                ConfiguredBrowserEngine(
                    delegate = decoratedEngine,
                    enabledCapabilities = enabledCapabilities
                )
            }
        }
    }

    fun create(
        context: Context,
        type: EngineType,
        config: BrowserConfig = BrowserConfig(),
        decoratorOptions: DecoratorOptions = DecoratorOptions()
    ): BrowserEngine =
        Builder(context = context, type = type)
            .settings(config)
            .decorators(decoratorOptions)
            .exposeAllCapabilities()
            .build()

    private fun createBaseEngine(
        context: Context,
        type: EngineType,
        config: BrowserConfig
    ): BrowserEngine {
        val creator = EngineCreatorRegistry.get(type) ?: defaultEngineCreators[type]
        checkNotNull(creator) { "No engine creator registered for $type" }
        return creator.create(context, config)
    }

    private fun applyDecorators(
        engine: BrowserEngine,
        decoratorOptions: DecoratorOptions
    ): BrowserEngine {
        var decorated = engine
        if (decoratorOptions.securityMode != null) {
            decorated = SecurityBrowserDecorator(
                delegate = decorated,
                mode = decoratorOptions.securityMode,
                allowedDomains = decoratorOptions.allowedDomains,
                blockedDomains = decoratorOptions.blockedDomains
            )
        }

        decoratorOptions.analytics?.let { callback ->
            decorated = AnalyticsBrowserDecorator(delegate = decorated, callback = callback)
        }

        if (decoratorOptions.logging) {
            decorated = LoggingBrowserDecorator(delegate = decorated, tag = decoratorOptions.loggingTag)
        }

        return decorated
    }
}

private class ConfiguredBrowserEngine(
    private val delegate: BrowserEngine,
    private val enabledCapabilities: Set<KClass<out BrowserCapability>>
) : BrowserEngine {

    override val state: StateFlow<com.browserengine.core.BrowserState> = delegate.state
    override val events: Flow<com.browserengine.core.BrowserEvent> = delegate.events

    override fun loadUrl(url: String, headers: Map<String, String>) = delegate.loadUrl(url, headers)

    override fun loadHtml(html: String, baseUrl: String?) = delegate.loadHtml(html, baseUrl)

    override fun reload() = delegate.reload()

    override fun stopLoading() = delegate.stopLoading()

    override fun destroy() = delegate.destroy()

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? {
        if (type !in enabledCapabilities) {
            Log.v(
                "BrowserEngineFactory",
                "${type.simpleName} was not registered in the builder; returning null"
            )
            return null
        }
        return delegate.capability(type)
    }
}
