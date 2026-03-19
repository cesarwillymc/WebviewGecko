package com.browserengine.gecko

import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserCapability
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

fun interface GeckoCapabilityInstaller {
    fun install(scope: GeckoCapabilityScope)
}

data class GeckoCapabilityScope(
    val engine: BrowserEngine,
    val config: BrowserConfig,
    val registry: CapabilityRegistry,
    val components: GeckoRuntimeBundle,
    val delegates: GeckoDelegateHub,
    val messaging: GeckoMessagingController
)

object GeckoCapabilityInstallers {
    fun defaults(): List<GeckoCapabilityInstaller> = listOf(
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

    fun ui(): GeckoCapabilityInstaller = register(UICapable::class) { scope ->
        GeckoUiCapability(scope.components.view)
    }

    fun scriptBridge(): GeckoCapabilityInstaller = GeckoCapabilityInstaller { scope ->
        scope.messaging.install()
        val bridge = GeckoBridgeCapability(scope.messaging)
        scope.registry.register(JsCapable::class, bridge)
        scope.registry.register(MessagingBridgeCapable::class, bridge)
    }

    fun permissions(): GeckoCapabilityInstaller = register(PermissionCapable::class) { scope ->
        GeckoPermissionCapability(scope.delegates)
    }

    fun cookies(): GeckoCapabilityInstaller = register(CookieCapable::class) { scope ->
        GeckoCookieCapability(scope.components.runtime)
    }

    fun storage(): GeckoCapabilityInstaller = register(StorageCapable::class) { scope ->
        GeckoStorageCapability(scope.components.runtime, scope.components.session)
    }

    fun screenshot(): GeckoCapabilityInstaller = register(ScreenshotCapable::class) {
        GeckoScreenshotCapability()
    }

    fun archive(): GeckoCapabilityInstaller = register(ArchiveCapable::class) {
        GeckoArchiveCapability()
    }

    fun navigation(): GeckoCapabilityInstaller = register(NavigationCapable::class) { scope ->
        GeckoNavigationCapability(scope.components.session, scope.delegates)
    }

    fun network(): GeckoCapabilityInstaller = register(NetworkCapable::class) { scope ->
        GeckoNetworkCapability(scope.components.runtime, scope.config, scope.delegates)
    }

    fun media(): GeckoCapabilityInstaller = register(MediaCapable::class) {
        GeckoMediaCapability()
    }

    fun popups(): GeckoCapabilityInstaller = register(PopupCapable::class) { scope ->
        GeckoPopupCapability(scope.delegates)
    }

    fun navigationInterception(): GeckoCapabilityInstaller =
        register(NavigationInterceptCapable::class) { scope ->
            GeckoNavigationInterceptCapability(scope.delegates)
        }

    private fun <T : BrowserCapability> register(
        type: kotlin.reflect.KClass<T>,
        factory: (GeckoCapabilityScope) -> T
    ): GeckoCapabilityInstaller = GeckoCapabilityInstaller { scope ->
        scope.registry.register(type, factory(scope))
    }
}
