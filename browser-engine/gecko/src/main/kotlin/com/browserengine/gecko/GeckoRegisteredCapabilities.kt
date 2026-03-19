package com.browserengine.gecko

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.browserengine.core.capabilities.ArchiveCapable
import com.browserengine.core.capabilities.ArchiveFormat
import com.browserengine.core.capabilities.BrowserCookie
import com.browserengine.core.capabilities.BrowserPermission
import com.browserengine.core.capabilities.CacheMode
import com.browserengine.core.capabilities.CookieCapable
import com.browserengine.core.capabilities.HistoryEntry
import com.browserengine.core.capabilities.ImageFormat
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MediaCapable
import com.browserengine.core.capabilities.MediaPolicy
import com.browserengine.core.capabilities.MediaSource
import com.browserengine.core.capabilities.MessagingBridgeCapable
import com.browserengine.core.capabilities.MixedContentMode
import com.browserengine.core.capabilities.NavigationCapable
import com.browserengine.core.capabilities.NavigationInterceptCapable
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.NetworkCapable
import com.browserengine.core.capabilities.PermissionCapable
import com.browserengine.core.capabilities.PopupCapable
import com.browserengine.core.capabilities.PopupCloseHandler
import com.browserengine.core.capabilities.PopupRequestHandler
import com.browserengine.core.capabilities.RequestInterceptor
import com.browserengine.core.capabilities.ScreenshotCapable
import com.browserengine.core.capabilities.StorageCapable
import com.browserengine.core.capabilities.UICapable
import com.browserengine.core.capabilities.UrlFilter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.StorageController
import java.io.File
import kotlin.coroutines.resume

internal class GeckoUiCapability(
    private val view: org.mozilla.geckoview.GeckoView
) : UICapable {
    @Composable
    override fun RenderUI(modifier: Modifier) {
        AndroidView(
            factory = { view },
            modifier = modifier
        )
    }
}

internal class GeckoBridgeCapability(
    private val messaging: GeckoMessagingController
) : JsCapable, MessagingBridgeCapable {
    override val bridgeName: String = "browser"

    override suspend fun evaluateScript(script: String): Result<String> = messaging.evaluateScript(script)

    override suspend fun injectScriptFromAssets(assetPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("GeckoView requires a WebExtension for asset script injection"))

    override suspend fun postMessageToJs(channel: String, data: String): Result<Unit> =
        messaging.postMessageToJs(channel, data)

    override fun setOnErrorHandler(handler: ((jsonMessage: String) -> Unit)?) {
        messaging.setOnErrorHandler(handler)
    }

    override fun setOnReadJsonHandler(handler: ((jsonMessage: String) -> Unit)?) {
        messaging.setOnReadJsonHandler(handler)
    }

    override fun addMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        messaging.addMessageListener(listener)
    }

    override fun removeMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        messaging.removeMessageListener(listener)
    }

    override fun postMessage(message: String) {
        messaging.postMessage(message)
    }
}

internal class GeckoPermissionCapability(
    private val delegates: GeckoDelegateHub
) : PermissionCapable {
    override val grantedPermissions: StateFlow<Set<BrowserPermission>> = delegates.grantedPermissions

    override fun setPermissionRequestHandler(handler: ((List<String>, () -> Unit, () -> Unit) -> Unit)?) {
        delegates.permissionRequestHandler = handler
    }

    override fun onPermissionRequested(
        permissions: List<String>,
        onGrant: () -> Unit,
        onDeny: () -> Unit
    ) {
        delegates.permissionRequestHandler?.invoke(permissions, onGrant, onDeny) ?: onDeny()
    }

    override fun grantContentPermission(permission: BrowserPermission) = Unit

    override fun denyContentPermission(permission: BrowserPermission) = Unit

    override fun selectMediaSource(
        videoSources: List<MediaSource>,
        audioSources: List<MediaSource>,
        onSelected: (video: MediaSource?, audio: MediaSource?) -> Unit
    ) {
        onSelected(videoSources.firstOrNull(), audioSources.firstOrNull())
    }
}

internal class GeckoCookieCapability(
    private val runtime: GeckoRuntime
) : CookieCapable {
    override suspend fun getCookies(url: String): List<BrowserCookie> = emptyList()

    override suspend fun setCookie(url: String, cookie: BrowserCookie) = Unit

    override suspend fun clearCookies() = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { cont ->
            runtime.storageController.clearData(StorageController.ClearFlags.COOKIES)
            cont.resume(Unit)
        }
    }

    override suspend fun clearCookiesFor(url: String) = withContext(Dispatchers.Main) {
        val host = runCatching { java.net.URL(url).host }.getOrNull() ?: return@withContext
        suspendCancellableCoroutine<Unit> { cont ->
            runtime.storageController.clearDataFromHost(host, StorageController.ClearFlags.COOKIES)
            cont.resume(Unit)
        }
    }

    override fun setThirdPartyCookiesEnabled(enabled: Boolean) = Unit
}

internal class GeckoStorageCapability(
    private val runtime: GeckoRuntime,
    private val session: GeckoSession
) : StorageCapable {
    override suspend fun clearCache() = withContext(Dispatchers.Main) { Unit }

    override suspend fun clearHistory() = withContext(Dispatchers.Main) {
        session.purgeHistory()
    }

    override suspend fun clearWebStorage() = withContext(Dispatchers.Main) { Unit }

    override suspend fun clearAll() = withContext(Dispatchers.Main) {
        session.purgeHistory()
        runtime.storageController.clearData(StorageController.ClearFlags.ALL_CACHES)
        Unit
    }

    override fun setCacheMode(mode: CacheMode) = Unit

    override fun setDomStorageEnabled(enabled: Boolean) = Unit
}

internal class GeckoScreenshotCapability : ScreenshotCapable {
    override suspend fun captureScreenshot(): Result<android.graphics.Bitmap> =
        Result.failure(UnsupportedOperationException("GeckoView capturePixels is not implemented in this project yet"))

    override suspend fun saveScreenshotTo(file: File, format: ImageFormat): Result<Unit> =
        Result.failure(UnsupportedOperationException("GeckoView screenshot saving is not implemented in this project yet"))
}

internal class GeckoArchiveCapability : ArchiveCapable {
    override val supportedFormats: List<ArchiveFormat> = listOf(ArchiveFormat.PDF, ArchiveFormat.HTML)

    override suspend fun savePage(destination: File, format: ArchiveFormat): Result<Unit> =
        Result.failure(UnsupportedOperationException("GeckoView page export is not implemented for this version yet"))
}

internal class GeckoNavigationCapability(
    private val session: GeckoSession,
    private val delegates: GeckoDelegateHub
) : NavigationCapable {
    override suspend fun goBack() = withContext(Dispatchers.Main) { session.goBack() }

    override suspend fun goForward() = withContext(Dispatchers.Main) { session.goForward() }

    override suspend fun goTo(historyIndex: Int) = Unit

    override suspend fun getHistory(): List<HistoryEntry> = emptyList()

    override suspend fun clearHistory() = withContext(Dispatchers.Main) { session.purgeHistory() }

    override fun setUrlFilter(filter: UrlFilter?) {
        delegates.urlFilter = filter
    }
}

internal class GeckoNetworkCapability(
    private val runtime: GeckoRuntime,
    config: com.browserengine.core.BrowserConfig,
    private val delegates: GeckoDelegateHub
) : NetworkCapable {
    var defaultHeaders: Map<String, String> = emptyMap()
        private set

    private var userAgentValue: String = config.userAgent.orEmpty()
    private var requestInterceptor: RequestInterceptor? = null

    override fun setUserAgent(userAgent: String) {
        userAgentValue = userAgent
        Log.w("GeckoEngine", "setUserAgent() after build is not fully applied yet; prefer BrowserConfig.userAgent")
    }

    override fun getUserAgent(): String = userAgentValue

    override fun setDefaultHeaders(headers: Map<String, String>) {
        defaultHeaders = headers
    }

    override fun setRequestInterceptor(interceptor: RequestInterceptor?) {
        requestInterceptor = interceptor
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        runtime.settings.javaScriptEnabled = enabled
    }

    override fun setMixedContentMode(mode: MixedContentMode) = Unit
}

internal class GeckoMediaCapability : MediaCapable {
    override fun setAutoplayEnabled(enabled: Boolean) = Unit

    override fun setMediaPolicy(policy: MediaPolicy) = Unit

    override suspend fun getAvailableCameras(): List<MediaSource> = emptyList()

    override suspend fun getAvailableMicrophones(): List<MediaSource> = emptyList()
}

internal class GeckoPopupCapability(
    private val delegates: GeckoDelegateHub
) : PopupCapable {
    override fun setPopupRequestHandler(handler: PopupRequestHandler?) {
        delegates.popupRequestHandler = handler
    }

    override fun setPopupCloseHandler(handler: PopupCloseHandler?) {
        delegates.popupCloseHandler = handler
    }

    override var allowBackgroundPopups: Boolean
        get() = delegates.allowBackgroundPopups
        set(value) {
            delegates.allowBackgroundPopups = value
        }
}

internal class GeckoNavigationInterceptCapability(
    private val delegates: GeckoDelegateHub
) : NavigationInterceptCapable {
    override fun setNavigationInterceptor(interceptor: NavigationInterceptor?) {
        delegates.navigationInterceptor = interceptor
    }

    override fun getNavigationInterceptor(): NavigationInterceptor? = delegates.navigationInterceptor
}
