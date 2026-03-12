package com.browserengine.core

sealed class BrowserEvent {
    data class PageStarted(val url: String) : BrowserEvent()
    data class PageFinished(val url: String, val success: Boolean) : BrowserEvent()
    data class ProgressChanged(val progress: Float) : BrowserEvent()
    data class TitleChanged(val title: String) : BrowserEvent()
    data class UrlChanged(val url: String) : BrowserEvent()
    data class ErrorReceived(val error: BrowserError) : BrowserEvent()
    data class ConsoleMessage(val message: String, val level: ConsoleLevel) : BrowserEvent()
    object PageScrolled : BrowserEvent()
}

enum class ConsoleLevel {
    DEBUG, INFO, WARNING, ERROR
}
