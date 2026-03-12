package com.browserengine.core.capabilities

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.browserengine.core.BrowserCapability

/**
 * Returns a @Composable that renders the browser.
 * The caller knows NOTHING about WebView or GeckoView.
 */
interface UICapable : BrowserCapability {
    @Composable
    fun RenderUI(modifier: Modifier)
}
