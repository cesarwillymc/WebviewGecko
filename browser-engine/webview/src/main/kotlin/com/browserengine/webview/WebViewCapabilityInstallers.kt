package com.browserengine.webview

import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.CapabilityRegistry
import com.browserengine.core.capabilities.ArchiveCapable
import com.browserengine.core.capabilities.CookieCapable
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MediaCapable
import com.browserengine.core.capabilities.MessagingBridgeCapable
import com.browserengine.core.capabilities.NavigationCapable
import com.browserengine.core.capabilities.NavigationInterceptCapable
import com.browserengine.core.capabilities.NetworkCapable
import com.browserengine.core.capabilities.PermissionCapable
import com.browserengine.core.capabilities.PopupCapable
import com.browserengine.core.capabilities.ScreenshotCapable
import com.browserengine.core.capabilities.StorageCapable
import com.browserengine.core.capabilities.UICapable

fun interface WebViewCapabilityInstaller {
    fun install(scope: WebViewCapabilityScope)
}

data class WebViewCapabilityScope(
    val engine: BrowserEngine,
    val config: BrowserConfig,
    val registry: CapabilityRegistry,
    val components: WebViewRuntimeBundle,
    val delegates: WebViewDelegateHub,
    val messaging: WebViewMessagingController
)

object WebViewCapabilityInstallers {
    fun defaults(): List<WebViewCapabilityInstaller> = listOf(
        ui(),
        scriptBridge(),
        permissions(),
        cookies(),
        storage(),
        screenshot(),
        archive(),
        navigation(),
        network(),
        media(),
        popups(),
        navigationInterception()
    )

    fun ui(): WebViewCapabilityInstaller = register(UICapable::class) { scope ->
        WebViewUiCapability(scope.components.webView)
    }

    fun scriptBridge(): WebViewCapabilityInstaller = WebViewCapabilityInstaller { scope ->
        scope.messaging.install()
        val bridge = WebViewBridgeCapability(scope.messaging)
        scope.registry.register(JsCapable::class, bridge)
        scope.registry.register(MessagingBridgeCapable::class, bridge)
    }

    fun permissions(): WebViewCapabilityInstaller = register(PermissionCapable::class) { scope ->
        WebViewPermissionCapability(scope.delegates)
    }

    fun cookies(): WebViewCapabilityInstaller = register(CookieCapable::class) { scope ->
        WebViewCookieCapability(scope.components.webView)
    }

    fun storage(): WebViewCapabilityInstaller = register(StorageCapable::class) { scope ->
        WebViewStorageCapability(scope.components.webView)
    }

    fun screenshot(): WebViewCapabilityInstaller = register(ScreenshotCapable::class) { scope ->
        WebViewScreenshotCapability(scope.components.webView)
    }

    fun archive(): WebViewCapabilityInstaller = register(ArchiveCapable::class) { scope ->
        WebViewArchiveCapability(scope.components.webView)
    }

    fun navigation(): WebViewCapabilityInstaller = register(NavigationCapable::class) { scope ->
        WebViewNavigationCapability(scope.components.webView, scope.delegates)
    }

    fun network(): WebViewCapabilityInstaller = register(NetworkCapable::class) { scope ->
        WebViewNetworkCapability(scope.components.webView, scope.delegates)
    }

    fun media(): WebViewCapabilityInstaller = register(MediaCapable::class) { scope ->
        WebViewMediaCapability(scope.components.webView)
    }

    fun popups(): WebViewCapabilityInstaller = register(PopupCapable::class) { scope ->
        WebViewPopupCapability(scope.delegates)
    }

    fun navigationInterception(): WebViewCapabilityInstaller =
        register(NavigationInterceptCapable::class) { scope ->
            WebViewNavigationInterceptCapability(scope.delegates)
        }

    private fun <T : BrowserCapability> register(
        type: kotlin.reflect.KClass<T>,
        factory: (WebViewCapabilityScope) -> T
    ): WebViewCapabilityInstaller = WebViewCapabilityInstaller { scope ->
        scope.registry.register(type, factory(scope))
    }
}
