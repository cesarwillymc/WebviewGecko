package com.browserengine.webview

import android.graphics.Bitmap
import android.graphics.Canvas
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.createBitmap
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.coroutines.resume

internal class WebViewUiCapability(
    private val webView: WebView
) : UICapable {
    @Composable
    override fun RenderUI(modifier: Modifier) {
        AndroidView(
            factory = { webView },
            modifier = modifier
        )
    }
}

internal class WebViewBridgeCapability(
    private val messaging: WebViewMessagingController
) : JsCapable, MessagingBridgeCapable {
    override val bridgeName: String = "Android"

    override suspend fun evaluateScript(script: String): Result<String> = messaging.evaluateScript(script)

    override suspend fun injectScriptFromAssets(assetPath: String): Result<Unit> =
        messaging.injectScriptFromAssets(assetPath)

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

internal class WebViewPermissionCapability(
    private val delegates: WebViewDelegateHub
) : PermissionCapable {
    override val grantedPermissions = delegates.grantedPermissions

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

    override fun grantContentPermission(permission: BrowserPermission) {
        delegates.pendingPermissionGrant?.invoke()
    }

    override fun denyContentPermission(permission: BrowserPermission) {
        delegates.pendingPermissionDeny?.invoke()
    }

    override fun selectMediaSource(
        videoSources: List<MediaSource>,
        audioSources: List<MediaSource>,
        onSelected: (video: MediaSource?, audio: MediaSource?) -> Unit
    ) {
        onSelected(videoSources.firstOrNull(), audioSources.firstOrNull())
    }
}

internal class WebViewCookieCapability(
    private val webView: WebView
) : CookieCapable {
    override suspend fun getCookies(url: String): List<BrowserCookie> = withContext(Dispatchers.Main) {
        CookieManager.getInstance().getCookie(url)?.split(";")?.mapNotNull { part ->
            val pieces = part.trim().split("=", limit = 2)
            val name = pieces.getOrNull(0) ?: return@mapNotNull null
            val value = pieces.getOrNull(1).orEmpty()
            BrowserCookie(name, value, java.net.URL(url).host)
        } ?: emptyList()
    }

    override suspend fun setCookie(url: String, cookie: BrowserCookie) = withContext(Dispatchers.Main) {
        CookieManager.getInstance().setCookie(url, "${cookie.name}=${cookie.value}")
    }

    override suspend fun clearCookies() = withContext(Dispatchers.Main) {
        CookieManager.getInstance().removeAllCookies(null)
        Unit
    }

    override suspend fun clearCookiesFor(url: String) = withContext(Dispatchers.Main) {
        CookieManager.getInstance().removeSessionCookies(null)
        Unit
    }

    override fun setThirdPartyCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, enabled)
    }
}

internal class WebViewStorageCapability(
    private val webView: WebView
) : StorageCapable {
    override suspend fun clearCache() = withContext(Dispatchers.Main) {
        webView.clearCache(true)
    }

    override suspend fun clearHistory() = withContext(Dispatchers.Main) {
        webView.clearHistory()
    }

    override suspend fun clearWebStorage() = withContext(Dispatchers.Main) {
        WebStorage.getInstance().deleteAllData()
    }

    override suspend fun clearAll() = withContext(Dispatchers.Main) {
        webView.clearCache(true)
        webView.clearHistory()
        WebStorage.getInstance().deleteAllData()
        CookieManager.getInstance().removeAllCookies(null)
        Unit
    }

    override fun setCacheMode(mode: CacheMode) {
        webView.settings.cacheMode = when (mode) {
            CacheMode.DEFAULT -> WebSettings.LOAD_DEFAULT
            CacheMode.NO_CACHE -> WebSettings.LOAD_NO_CACHE
            CacheMode.CACHE_ONLY -> WebSettings.LOAD_CACHE_ONLY
            CacheMode.CACHE_ELSE_NETWORK -> WebSettings.LOAD_CACHE_ELSE_NETWORK
        }
    }

    override fun setDomStorageEnabled(enabled: Boolean) {
        webView.settings.domStorageEnabled = enabled
    }
}

internal class WebViewScreenshotCapability(
    private val webView: WebView
) : ScreenshotCapable {
    override suspend fun captureScreenshot(): Result<Bitmap> = withContext(Dispatchers.Main) {
        runCatching {
            createBitmap(webView.width, webView.height).apply {
                webView.draw(Canvas(this))
            }
        }
    }

    override suspend fun saveScreenshotTo(file: File, format: ImageFormat): Result<Unit> = withContext(Dispatchers.Main) {
        captureScreenshot().mapCatching { bitmap ->
            FileOutputStream(file).use { out ->
                bitmap.compress(
                    when (format) {
                        ImageFormat.PNG -> Bitmap.CompressFormat.PNG
                        ImageFormat.JPEG -> Bitmap.CompressFormat.JPEG
                        ImageFormat.WEBP -> Bitmap.CompressFormat.WEBP
                    },
                    100,
                    out
                )
            }
            Unit
        }
    }
}

internal class WebViewArchiveCapability(
    private val webView: WebView
) : ArchiveCapable {
    override val supportedFormats: List<ArchiveFormat> = listOf(ArchiveFormat.MHTML, ArchiveFormat.HTML)

    override suspend fun savePage(destination: File, format: ArchiveFormat): Result<Unit> = withContext(Dispatchers.Main) {
        when (format) {
            ArchiveFormat.MHTML -> runCatching {
                suspendCancellableCoroutine<Unit> { cont ->
                    webView.saveWebArchive(destination.absolutePath, false) { path ->
                        if (path != null) cont.resume(Unit) else cont.cancel(Exception("Save failed"))
                    }
                }
            }
            ArchiveFormat.HTML -> runCatching {
                suspendCancellableCoroutine<Unit> { cont ->
                    webView.evaluateJavascript("document.documentElement.outerHTML") { result ->
                        val content = when {
                            result == null || result == "null" -> ""
                            result.startsWith("\"") && result.endsWith("\"") -> result.drop(1).dropLast(1)
                            else -> result
                        }
                        destination.writeText(content)
                        cont.resume(Unit)
                    }
                }
            }
            ArchiveFormat.PDF -> Result.failure(UnsupportedOperationException("WebView does not support PDF"))
        }
    }
}

internal class WebViewNavigationCapability(
    private val webView: WebView,
    private val delegates: WebViewDelegateHub
) : NavigationCapable {
    override suspend fun goBack() = withContext(Dispatchers.Main) { webView.goBack() }

    override suspend fun goForward() = withContext(Dispatchers.Main) { webView.goForward() }

    override suspend fun goTo(historyIndex: Int) = withContext(Dispatchers.Main) {
        webView.goBackOrForward(historyIndex - webView.copyBackForwardList().currentIndex)
    }

    override suspend fun getHistory(): List<HistoryEntry> = withContext(Dispatchers.Main) {
        val list = webView.copyBackForwardList()
        (0 until list.size).map { index ->
            HistoryEntry(
                url = list.getItemAtIndex(index).url,
                title = list.getItemAtIndex(index).title,
                visitedAt = System.currentTimeMillis()
            )
        }
    }

    override suspend fun clearHistory() = withContext(Dispatchers.Main) {
        webView.clearHistory()
    }

    override fun setUrlFilter(filter: UrlFilter?) {
        delegates.urlFilter = filter
    }
}

internal class WebViewNetworkCapability(
    private val webView: WebView,
    private val delegates: WebViewDelegateHub
) : NetworkCapable {
    var defaultHeaders: Map<String, String> = emptyMap()
        private set

    override fun setUserAgent(userAgent: String) {
        webView.settings.userAgentString = userAgent
    }

    override fun getUserAgent(): String = webView.settings.userAgentString

    override fun setDefaultHeaders(headers: Map<String, String>) {
        defaultHeaders = headers
    }

    override fun setRequestInterceptor(interceptor: RequestInterceptor?) {
        delegates.requestInterceptor = interceptor
    }

    override fun setJavaScriptEnabled(enabled: Boolean) {
        webView.settings.javaScriptEnabled = enabled
    }

    override fun setMixedContentMode(mode: MixedContentMode) {
        webView.settings.mixedContentMode = when (mode) {
            MixedContentMode.NEVER_ALLOW -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
            MixedContentMode.COMPATIBILITY_MODE -> WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            MixedContentMode.ALWAYS_ALLOW -> WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }
}

internal class WebViewMediaCapability(
    private val webView: WebView
) : MediaCapable {
    override fun setAutoplayEnabled(enabled: Boolean) {
        webView.settings.mediaPlaybackRequiresUserGesture = !enabled
    }

    override fun setMediaPolicy(policy: MediaPolicy) {
        webView.settings.mediaPlaybackRequiresUserGesture = policy == MediaPolicy.USER_GESTURE_REQUIRED
    }

    override suspend fun getAvailableCameras(): List<MediaSource> = emptyList()

    override suspend fun getAvailableMicrophones(): List<MediaSource> = emptyList()
}

internal class WebViewPopupCapability(
    private val delegates: WebViewDelegateHub
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

internal class WebViewNavigationInterceptCapability(
    private val delegates: WebViewDelegateHub
) : NavigationInterceptCapable {
    override fun setNavigationInterceptor(interceptor: NavigationInterceptor?) {
        delegates.navigationInterceptor = interceptor
    }

    override fun getNavigationInterceptor(): NavigationInterceptor? = delegates.navigationInterceptor
}
