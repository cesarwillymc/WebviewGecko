package com.browserengine.webview

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

class WebViewStateStore {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutableState = MutableStateFlow(BrowserState())
    private val mutableEvents = MutableSharedFlow<BrowserEvent>()

    val state: StateFlow<BrowserState> = mutableState.asStateFlow()
    val events: Flow<BrowserEvent> = mutableEvents.asSharedFlow()

    fun onPageStarted(url: String) {
        mutableState.value = mutableState.value.copy(
            url = url,
            isLoading = true,
            progress = 0f,
            error = null
        )
        scope.launch { mutableEvents.emit(BrowserEvent.PageStarted(url)) }
    }

    fun onPageFinished(url: String, title: String, canGoBack: Boolean, canGoForward: Boolean) {
        mutableState.value = mutableState.value.copy(
            url = url,
            title = title,
            isLoading = false,
            progress = 1f,
            canGoBack = canGoBack,
            canGoForward = canGoForward,
            error = null
        )
        scope.launch { mutableEvents.emit(BrowserEvent.PageFinished(url, true)) }
    }

    fun onReceivedError(errorCode: Int, description: String, failingUrl: String) {
        val error = BrowserError(errorCode, description, failingUrl)
        mutableState.value = mutableState.value.copy(isLoading = false, error = error)
        scope.launch { mutableEvents.emit(BrowserEvent.ErrorReceived(error)) }
    }

    fun updateProgress(progress: Float) {
        mutableState.value = mutableState.value.copy(progress = progress)
        scope.launch { mutableEvents.emit(BrowserEvent.ProgressChanged(progress)) }
    }

    fun updateTitle(title: String) {
        mutableState.value = mutableState.value.copy(title = title)
        scope.launch { mutableEvents.emit(BrowserEvent.TitleChanged(title)) }
    }
}
