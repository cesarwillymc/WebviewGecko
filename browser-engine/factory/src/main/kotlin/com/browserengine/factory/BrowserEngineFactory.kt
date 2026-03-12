package com.browserengine.factory

import android.content.Context
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.EngineType
import com.browserengine.decorators.AnalyticsBrowserDecorator
import com.browserengine.decorators.AnalyticsCallback
import com.browserengine.decorators.LoggingBrowserDecorator
import com.browserengine.decorators.SecurityBrowserDecorator
import com.browserengine.decorators.SecurityMode
import com.browserengine.gecko.GeckoEngine
import com.browserengine.webview.WebViewEngine

data class DecoratorOptions(
    val logging: Boolean = false,
    val loggingTag: String = "BrowserEngine",
    val analytics: AnalyticsCallback? = null,
    val securityMode: SecurityMode? = null,
    val allowedDomains: Set<String> = emptySet(),
    val blockedDomains: Set<String> = emptySet()
)

object BrowserEngineFactory {

    fun create(
        context: Context,
        type: EngineType,
        config: BrowserConfig = BrowserConfig(),
        decoratorOptions: DecoratorOptions = DecoratorOptions()
    ): BrowserEngine {
        var engine: BrowserEngine = when (type) {
            EngineType.WEBVIEW -> WebViewEngine(
                context = context,
                config = config
            )
            EngineType.GECKO -> GeckoEngine(
                context = context,
                config = config
            )
        }

        if (decoratorOptions.securityMode != null) {
            engine = SecurityBrowserDecorator(
                delegate = engine,
                mode = decoratorOptions.securityMode,
                allowedDomains = decoratorOptions.allowedDomains,
                blockedDomains = decoratorOptions.blockedDomains
            )
        }

        decoratorOptions.analytics?.let { callback ->
            engine = AnalyticsBrowserDecorator(delegate = engine, callback = callback)
        }

        if (decoratorOptions.logging) {
            engine = LoggingBrowserDecorator(delegate = engine, tag = decoratorOptions.loggingTag)
        }

        return engine
    }
}
