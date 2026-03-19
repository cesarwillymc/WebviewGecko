package com.browserengine.gecko

import com.browserengine.core.BrowserError
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GeckoStateStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(BrowserState())
    private val mutableEvents = MutableSharedFlow<BrowserEvent>()

    val state: StateFlow<BrowserState> = mutableState.asStateFlow()
    val events: Flow<BrowserEvent> = mutableEvents.asSharedFlow()

    fun updateUrl(url: String) {
        mutableState.value = mutableState.value.copy(url = url)
        scope.launch { mutableEvents.emit(BrowserEvent.UrlChanged(url)) }
    }

    fun updateTitle(title: String) {
        mutableState.value = mutableState.value.copy(title = title)
        scope.launch { mutableEvents.emit(BrowserEvent.TitleChanged(title)) }
    }

    fun onPageStarted(url: String) {
        mutableState.value = mutableState.value.copy(
            url = url,
            isLoading = true,
            progress = 0f,
            error = null
        )
        scope.launch { mutableEvents.emit(BrowserEvent.PageStarted(url)) }
    }

    fun onPageFinished(success: Boolean, canGoBack: Boolean, canGoForward: Boolean) {
        mutableState.value = mutableState.value.copy(
            isLoading = false,
            progress = 1f,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            error = if (!success) BrowserError(-1, "Load failed", mutableState.value.url) else null
        )
        scope.launch {
            mutableEvents.emit(BrowserEvent.PageFinished(mutableState.value.url, success))
        }
    }

    fun updateProgress(progress: Float) {
        mutableState.value = mutableState.value.copy(progress = progress)
        scope.launch { mutableEvents.emit(BrowserEvent.ProgressChanged(progress)) }
    }
}
