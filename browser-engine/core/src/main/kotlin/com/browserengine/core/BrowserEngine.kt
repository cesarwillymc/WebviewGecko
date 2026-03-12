package com.browserengine.core

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlin.reflect.KClass

/**
 * Root contract. Exposes NOTHING engine-specific.
 * Every feature lives in a capability interface.
 */
interface BrowserEngine {
    val state: StateFlow<BrowserState>
    val events: Flow<BrowserEvent>

    fun loadUrl(url: String, headers: Map<String, String> = emptyMap())
    fun loadHtml(html: String, baseUrl: String? = null)
    fun reload()
    fun stopLoading()
    fun destroy()

    /** Capability discovery (type-safe) */
    fun <T : BrowserCapability> capability(type: KClass<T>): T?
}

/** Convenience inline extension */
inline fun <reified T : BrowserCapability> BrowserEngine.capability(): T? =
    capability(T::class)
