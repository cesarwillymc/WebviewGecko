package com.browserengine.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import com.browserengine.core.BrowserConfig
import com.browserengine.core.capabilities.MixedContentMode

@SuppressLint("SetJavaScriptEnabled")
class WebViewRuntimeBundle(
    context: Context,
    config: BrowserConfig,
    delegates: WebViewDelegateHub
) {
    val webView: WebView = WebView(context).apply {
        CookieManager.getInstance().setAcceptCookie(config.cookiesEnabled)
        settings.apply {
            javaScriptEnabled = config.javaScriptEnabled
            domStorageEnabled = config.domStorageEnabled
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = when (config.mixedContentMode) {
                MixedContentMode.NEVER_ALLOW -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
                MixedContentMode.COMPATIBILITY_MODE -> WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                MixedContentMode.ALWAYS_ALLOW -> WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            javaScriptCanOpenWindowsAutomatically = config.supportMultipleWindows
            setSupportMultipleWindows(config.supportMultipleWindows)
            config.userAgent?.let { userAgentString = it }
        }
        webViewClient = delegates.webViewClient
        webChromeClient = delegates.webChromeClient
    }

    fun destroy() {
        webView.destroy()
    }
}
