package com.example.webviewgecko.feature.gecko

import com.browserengine.core.EngineType
import com.browserengine.factory.EngineCreator
import com.browserengine.factory.EngineCreatorRegistry
import com.browserengine.gecko.GeckoEngine

object GeckoFeatureEntry {
    @JvmStatic
    fun register() {
        EngineCreatorRegistry.register(
            EngineType.GECKO,
            EngineCreator { context, config ->
                GeckoEngine.Builder(context)
                    .settings(config)
                    .addDefaultCapabilities()
                    .build()
            }
        )
    }
}
