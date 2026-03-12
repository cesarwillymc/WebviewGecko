package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

interface StorageCapable : BrowserCapability {
    /** Clear all cached resources. */
    suspend fun clearCache()

    /** Clear browsing history. */
    suspend fun clearHistory()

    /** Clear all web storage (localStorage, sessionStorage, IndexedDB). */
    suspend fun clearWebStorage()

    /** Clear all data (cache + cookies + history + storage). */
    suspend fun clearAll()

    /** Set cache mode. */
    fun setCacheMode(mode: CacheMode)

    /** Enable/disable DOM storage. */
    fun setDomStorageEnabled(enabled: Boolean)
}

enum class CacheMode {
    DEFAULT,
    NO_CACHE,
    CACHE_ONLY,
    CACHE_ELSE_NETWORK
}
