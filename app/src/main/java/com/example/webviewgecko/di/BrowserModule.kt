package com.example.webviewgecko.di

import android.content.Context
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.EngineType
import com.browserengine.factory.BrowserEngineFactory
import com.browserengine.factory.DecoratorOptions
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
        @ApplicationContext context: Context
    ): BrowserEngine {
        return BrowserEngineFactory.create(
            context = context,
            type = EngineType.GECKO,
            config = BrowserConfig(
                javaScriptEnabled = true,
                domStorageEnabled = true,
                supportMultipleWindows = true
            ),
            decoratorOptions = DecoratorOptions(
                logging = true,
                loggingTag = "BrowserEngine"
            )
        )
    }
}
