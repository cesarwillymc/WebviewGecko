package com.example.webviewgecko

import android.content.Context
import android.util.Log
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.EngineType
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.NavigationResult
import com.browserengine.factory.BrowserCapabilities
import com.browserengine.factory.BrowserEngineFactory
import com.browserengine.factory.DecoratorOptions
import com.browserengine.factory.EngineFeatureValidation
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrowserEngineProvider @Inject constructor(
    private val permissionCoordinator: BrowserPermissionCoordinator,
) {
    fun build(context: Context, type: EngineType): BrowserEngine {
        return BrowserEngineFactory.Builder(
            context = context,
            type = type
        )
            .settings(
                BrowserConfig(
                    javaScriptEnabled = true,
                    domStorageEnabled = true,
                    supportMultipleWindows = true
                )
            )
            .addCapability(BrowserCapabilities.navigation())
            .addCapability(BrowserCapabilities.javaScript())
            .addCapability(BrowserCapabilities.navigationOverride(NavigationInterceptor { request ->
                Log.e("BrowserEngineProvider", "Navigation request: ${request.url}")
                NavigationResult.Allow
            }))
            .addCapability(BrowserCapabilities.messaging())
            .addCapability(BrowserCapabilities.permissions(permissionCoordinator::onPermissionRequested))
            .decorators(
                DecoratorOptions(
                    logging = true,
                    loggingTag = "BrowserEngine"
                )
            )
            .build()
    }
}
