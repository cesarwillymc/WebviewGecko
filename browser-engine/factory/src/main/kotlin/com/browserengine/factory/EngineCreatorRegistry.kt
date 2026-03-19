package com.browserengine.factory

import android.content.Context
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.EngineType

fun interface EngineCreator {
    fun create(context: Context, config: BrowserConfig): BrowserEngine
}

object EngineCreatorRegistry {
    private val creators = linkedMapOf<EngineType, EngineCreator>()

    fun register(type: EngineType, creator: EngineCreator) {
        creators[type] = creator
    }

    fun get(type: EngineType): EngineCreator? = creators[type]
}
