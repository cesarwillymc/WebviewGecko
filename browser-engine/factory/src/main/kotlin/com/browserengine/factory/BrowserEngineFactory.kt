package com.browserengine.factory

import android.content.Context
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.EngineType
import com.browserengine.webview.WebViewEngine

object BrowserEngineFactory {

    fun create(
        context: Context,
        type: EngineType,
        config: BrowserConfig = BrowserConfig()
    ): BrowserEngine {
        return when (type) {
            EngineType.WEBVIEW -> WebViewEngine(
                context = context,
                config = config
            )
            EngineType.GECKO -> throw UnsupportedOperationException("GeckoView not yet implemented")
        }
    }
}
