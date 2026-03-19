package com.browserengine.core

import kotlin.reflect.KClass

class CapabilityRegistry {
    private val capabilities = linkedMapOf<KClass<out BrowserCapability>, BrowserCapability>()

    fun <T : BrowserCapability> register(type: KClass<T>, capability: T) {
        capabilities[type] = capability
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : BrowserCapability> get(type: KClass<T>): T? = capabilities[type] as? T
}
