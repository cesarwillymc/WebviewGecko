package com.example.webviewgecko.di

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
import com.example.webviewgecko.BrowserPermissionCoordinator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BrowserModule {

    @Provides
    @Singleton
    fun provideBrowserEngine(
        @ApplicationContext context: Context,
        permissionCoordinator: BrowserPermissionCoordinator
    ): BrowserEngine {
        return BrowserEngineFactory.Builder(
            context = context,
            type = EngineType.GECKO
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
                Log.e("provideBrowserEngine", "provideBrowserEngine: ${request.url}")
                return@NavigationInterceptor NavigationResult.Allow
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
