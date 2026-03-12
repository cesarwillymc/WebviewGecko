package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

interface NavigationCapable : BrowserCapability {
    suspend fun goBack()
    suspend fun goForward()
    suspend fun goTo(historyIndex: Int)

    suspend fun getHistory(): List<HistoryEntry>
    suspend fun clearHistory()

    /** Pass null to allow all URLs. */
    fun setUrlFilter(filter: UrlFilter?)
}

data class HistoryEntry(
    val url: String,
    val title: String,
    val visitedAt: Long
)

fun interface UrlFilter {
    fun shouldAllow(url: String): Boolean
}
