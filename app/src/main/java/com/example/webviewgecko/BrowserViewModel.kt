package com.example.webviewgecko

import androidx.lifecycle.ViewModel
import com.browserengine.core.BrowserEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowserViewModel @Inject constructor(
    val engine: BrowserEngine
) : ViewModel() {

    override fun onCleared() {
        super.onCleared()
        engine.destroy()
    }
}
