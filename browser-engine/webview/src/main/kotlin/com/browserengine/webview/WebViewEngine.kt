package com.browserengine.webview

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.CookieManager
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserError
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import com.browserengine.core.SecurityInfo
import com.browserengine.core.capabilities.ArchiveCapable
import com.browserengine.core.capabilities.ArchiveFormat
import com.browserengine.core.capabilities.CacheMode
import com.browserengine.core.capabilities.CookieCapable
import com.browserengine.core.capabilities.HistoryEntry
import com.browserengine.core.capabilities.InterceptResult
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MediaCapable
import com.browserengine.core.capabilities.MessagingBridgeCapable
import com.browserengine.core.capabilities.MediaPolicy
import com.browserengine.core.capabilities.MediaSource
import com.browserengine.core.capabilities.MixedContentMode
import com.browserengine.core.capabilities.NavigationCapable
import com.browserengine.core.capabilities.NavigationInterceptCapable
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.NavigationRequest
import com.browserengine.core.capabilities.NavigationResult
import com.browserengine.core.capabilities.NavigationTrigger
import com.browserengine.core.capabilities.NetworkCapable
import com.browserengine.core.capabilities.PermissionCapable
import com.browserengine.core.capabilities.PopupCapable
import com.browserengine.core.capabilities.PopupCloseHandler
import com.browserengine.core.capabilities.PopupRequestHandler
import com.browserengine.core.capabilities.RequestInterceptor
import com.browserengine.core.capabilities.ScreenshotCapable
import com.browserengine.core.capabilities.SourceType
import com.browserengine.core.capabilities.StorageCapable
import com.browserengine.core.capabilities.UICapable
import com.browserengine.core.capabilities.UrlFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.reflect.KClass

@SuppressLint("SetJavaScriptEnabled")
class WebViewEngine(
    private val context: Context,
    config: BrowserConfig = BrowserConfig()
) : BrowserEngine,
    UICapable,
    JsCapable,
    PermissionCapable,
    CookieCapable,
    StorageCapable,
    ScreenshotCapable,
    ArchiveCapable,
    NavigationCapable,
    NetworkCapable,
    MediaCapable,
    PopupCapable,
    NavigationInterceptCapable,
    MessagingBridgeCapable {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _state = MutableStateFlow(BrowserState())
    override val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<BrowserEvent>()
    override val events: Flow<BrowserEvent> = _events.asSharedFlow()

    private val _grantedPermissions = MutableStateFlow<Set<com.browserengine.core.capabilities.BrowserPermission>>(emptySet())
    override val grantedPermissions: StateFlow<Set<com.browserengine.core.capabilities.BrowserPermission>> =
        _grantedPermissions.asStateFlow()

    private val messageListeners = mutableListOf<MessagingBridgeCapable.MessageListener>()
    private var onErrorHandler: ((String) -> Unit)? = null
    private var onReadJsonHandler: ((String) -> Unit)? = null

    private val webViewBridge = object {
        @JavascriptInterface
        fun onError(message: String) {
            this@WebViewEngine.onErrorHandler?.invoke(message)
            this@WebViewEngine.messageListeners.forEach { it.onMessage(message) }
        }

        @JavascriptInterface
        fun onReadJson(message: String) {
            this@WebViewEngine.onReadJsonHandler?.invoke(message)
            this@WebViewEngine.messageListeners.forEach { it.onMessage(message) }
        }
    }

    val webView: WebView = WebView(context).apply {
        settings.apply {
            javaScriptEnabled = config.javaScriptEnabled
            domStorageEnabled = config.domStorageEnabled
            cacheMode = when (config.mixedContentMode) {
                MixedContentMode.NEVER_ALLOW, MixedContentMode.COMPATIBILITY_MODE ->
                    WebSettings.LOAD_DEFAULT
                MixedContentMode.ALWAYS_ALLOW -> WebSettings.LOAD_DEFAULT
            }
            mixedContentMode = when (config.mixedContentMode) {
                MixedContentMode.NEVER_ALLOW -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
                MixedContentMode.COMPATIBILITY_MODE -> WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                MixedContentMode.ALWAYS_ALLOW -> WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            }
            javaScriptCanOpenWindowsAutomatically = config.supportMultipleWindows
            config.userAgent?.let { userAgentString = it }
        }
        webViewClient = createWebViewClient()
        webChromeClient = createWebChromeClient()
        addJavascriptInterface(webViewBridge, bridgeName)
    }

    private var defaultHeaders: Map<String, String> = emptyMap()
    private var requestInterceptor: RequestInterceptor? = null
    private var navigationInterceptor: NavigationInterceptor? = null
    private var urlFilter: UrlFilter? = null
    private var popupRequestHandler: PopupRequestHandler? = null
    private var popupCloseHandler: PopupCloseHandler? = null
    override var allowBackgroundPopups: Boolean = false

    private var permissionRequestHandler: ((List<com.browserengine.core.capabilities.BrowserPermission>, () -> Unit, () -> Unit) -> Unit)? = null
    private var pendingPermissionGrant: (() -> Unit)? = null
    private var pendingPermissionDeny: (() -> Unit)? = null

    private fun createWebViewClient() = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: android.webkit.WebResourceRequest): Boolean {
            val interceptor = navigationInterceptor ?: return false
            val result = interceptor.intercept(
                NavigationRequest(
                    url = request.url.toString(),
                    method = request.method,
                    isRedirect = false,
                    isUserInitiated = request.isRedirect,
                    trigger = NavigationTrigger.LINK_CLICK
                )
            )
            return when (result) {
                is NavigationResult.Allow -> false
                is NavigationResult.Block -> true
                is NavigationResult.ConsumedByApp -> true
                is NavigationResult.Redirect -> {
                    view.loadUrl(result.newUrl)
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            _state.value = _state.value.copy(
                url = url ?: "",
                isLoading = true,
                progress = 0f,
                error = null
            )
            scope.launch { _events.emit(BrowserEvent.PageStarted(url ?: "")) }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            _state.value = _state.value.copy(
                url = url ?: "",
                title = view?.title ?: "",
                isLoading = false,
                progress = 1f,
                canGoBack = webView.canGoBack(),
                canGoForward = webView.canGoForward(),
                error = null
            )
        }

        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = BrowserError(errorCode, description ?: "", failingUrl ?: "")
            )
            scope.launch { _events.emit(BrowserEvent.ErrorReceived(BrowserError(errorCode, description ?: "", failingUrl ?: ""))) }
        }
    }

    private fun createWebChromeClient() = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            _state.value = _state.value.copy(progress = newProgress / 100f)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            _state.value = _state.value.copy(title = title ?: "")
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: android.os.Message?
        ): Boolean {
            if (!allowBackgroundPopups && !isUserGesture) return false
            val handler = popupRequestHandler ?: return false
            handler.onPopupRequested(
                opener = this@WebViewEngine,
                uri = null,
                isUserGesture = isUserGesture,
                onAllow = { newEngine ->
                    val newWebView = (newEngine as? WebViewEngine)?.webView
                    if (newWebView != null && resultMsg != null) {
                        (resultMsg.obj as? WebView.WebViewTransport)?.let { transport ->
                            transport.webView = newWebView
                            resultMsg.sendToTarget()
                            synchronized(popupEngines) { popupEngines.add(newEngine) }
                        }
                    }
                },
                onBlock = { }
            )
            return true
        }

        override fun onCloseWindow(window: WebView?) {
            val popupEngine = synchronized(popupEngines) {
                popupEngines.find { (it as? WebViewEngine)?.webView == window }?.also {
                    popupEngines.remove(it)
                }
            }
            popupEngine?.let { popupCloseHandler?.onPopupCloseRequested(it) }
        }
    }

    private val popupEngines = mutableListOf<BrowserEngine>()

    override fun loadUrl(url: String, headers: Map<String, String>) {
        val allHeaders = defaultHeaders + headers
        if (allHeaders.isEmpty()) {
            webView.loadUrl(url)
        } else {
            webView.loadUrl(url, allHeaders)
        }
    }

    override fun loadHtml(html: String, baseUrl: String?) {
        webView.loadDataWithBaseURL(
            baseUrl,
            html,
            "text/html",
            "UTF-8",
            null
        )
    }

    override fun reload() = webView.reload()
    override fun stopLoading() = webView.stopLoading()
    override fun destroy() {
        webView.destroy()
    }

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? {
        return when (type) {
            UICapable::class -> this as T
            JsCapable::class -> this as T
            PermissionCapable::class -> this as T
            CookieCapable::class -> this as T
            StorageCapable::class -> this as T
            ScreenshotCapable::class -> this as T
            ArchiveCapable::class -> this as T
            NavigationCapable::class -> this as T
            NetworkCapable::class -> this as T
            MediaCapable::class -> this as T
            PopupCapable::class -> this as T
            NavigationInterceptCapable::class -> this as T
            MessagingBridgeCapable::class -> this as T
            else -> null
        }
    }

    // UICapable
    @androidx.compose.runtime.Composable
    override fun RenderUI(modifier: androidx.compose.ui.Modifier) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { webView },
            modifier = modifier
        )
    }

    // JsCapable
    override suspend fun evaluateScript(script: String): Result<String> = withContext(Dispatchers.Main) {
        kotlin.runCatching {
            suspendCancellableCoroutine { cont ->
                webView.evaluateJavascript(script) { result ->
                    val value = when {
                        result == null -> "null"
                        result == "null" -> "null"
                        result.startsWith("\"") && result.endsWith("\"") -> result.drop(1).dropLast(1)
                        else -> result
                    }
                    cont.resumeWith(Result.success(value))
                }
            }
        }
    }

    override suspend fun injectScriptFromAssets(assetPath: String): Result<Unit> = withContext(Dispatchers.Main) {
        kotlin.runCatching {
            val html = context.assets.open(assetPath).bufferedReader().readText()
            webView.evaluateJavascript(html) { }
        }
    }

    override fun registerNativeFunction(name: String, handler: (args: String) -> String) {
        webView.addJavascriptInterface(object {
            @android.webkit.JavascriptInterface
            fun invoke(args: String) = handler(args)
        }, name)
    }

    override suspend fun postMessageToJs(channel: String, data: String): Result<Unit> =
        evaluateScript("window.postMessage?.({channel:'$channel',data:'$data'},'*')").map { }

    // MessagingBridgeCapable
    override val bridgeName: String get() = "Android"

    override fun setOnErrorHandler(handler: ((jsonMessage: String) -> Unit)?) {
        onErrorHandler = handler
    }

    override fun setOnReadJsonHandler(handler: ((jsonMessage: String) -> Unit)?) {
        onReadJsonHandler = handler
    }

    override fun addMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        messageListeners.add(listener)
    }

    override fun removeMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        messageListeners.remove(listener)
    }

    override fun postMessage(message: String) {
        val escaped = message.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
        scope.launch {
            evaluateScript(
                """
                (function() {
                    if (typeof window.onAndroidMessage === 'function') {
                        window.onAndroidMessage('$escaped');
                    }
                    window.postMessage({ type: 'FROM_ANDROID', data: '$escaped' }, '*');
                })();
                """.trimIndent()
            )
        }
    }

    // PermissionCapable
    fun setPermissionRequestHandler(handler: ((List<com.browserengine.core.capabilities.BrowserPermission>, () -> Unit, () -> Unit) -> Unit)?) {
        permissionRequestHandler = handler
    }

    override fun onPermissionRequested(
        permissions: List<com.browserengine.core.capabilities.BrowserPermission>,
        onGrant: () -> Unit,
        onDeny: () -> Unit
    ) {
        permissionRequestHandler?.invoke(permissions, onGrant, onDeny)
            ?: onDeny()
    }

    override fun grantContentPermission(permission: com.browserengine.core.capabilities.BrowserPermission) {
        pendingPermissionGrant?.invoke()
    }

    override fun denyContentPermission(permission: com.browserengine.core.capabilities.BrowserPermission) {
        pendingPermissionDeny?.invoke()
    }

    override fun selectMediaSource(
        videoSources: List<MediaSource>,
        audioSources: List<MediaSource>,
        onSelected: (video: MediaSource?, audio: MediaSource?) -> Unit
    ) {
        onSelected(videoSources.firstOrNull(), audioSources.firstOrNull())
    }

    // CookieCapable
    override suspend fun getCookies(url: String): List<com.browserengine.core.capabilities.BrowserCookie> =
        withContext(Dispatchers.Main) {
            CookieManager.getInstance().getCookie(url)?.split(";")?.mapNotNull { part ->
                val (name, value) = part.trim().split("=", limit = 2)
                com.browserengine.core.capabilities.BrowserCookie(name, value, java.net.URL(url).host)
            } ?: emptyList()
        }

    override suspend fun setCookie(url: String, cookie: com.browserengine.core.capabilities.BrowserCookie) =
        withContext(Dispatchers.Main) {
            CookieManager.getInstance().setCookie(url, "${cookie.name}=${cookie.value}")
        }

    override suspend fun clearCookies() = withContext(Dispatchers.Main) {
        CookieManager.getInstance().removeAllCookies(null)
    }

    override suspend fun clearCookiesFor(url: String) = withContext(Dispatchers.Main) {
        CookieManager.getInstance().removeSessionCookies(null)
    }

    override fun setThirdPartyCookiesEnabled(enabled: Boolean) {
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, enabled)
    }

    // StorageCapable
    override suspend fun clearCache() = withContext(Dispatchers.Main) {
        webView.clearCache(true)
    }

    override suspend fun clearWebStorage() = withContext(Dispatchers.Main) {
        android.webkit.WebStorage.getInstance().deleteAllData()
    }

    override suspend fun clearAll() = withContext(Dispatchers.Main) {
        clearCache()
        clearHistory()
        clearWebStorage()
        CookieManager.getInstance().removeAllCookies(null)
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

    // ScreenshotCapable
    override suspend fun captureScreenshot(): Result<android.graphics.Bitmap> = withContext(Dispatchers.Main) {
        kotlin.runCatching {
            android.graphics.Bitmap.createBitmap(webView.width, webView.height, android.graphics.Bitmap.Config.ARGB_8888).apply {
                webView.draw(android.graphics.Canvas(this))
            }
        }
    }

    override suspend fun saveScreenshotTo(file: java.io.File, format: com.browserengine.core.capabilities.ImageFormat): Result<Unit> =
        withContext(Dispatchers.Main) {
            captureScreenshot().mapCatching { bitmap ->
                java.io.FileOutputStream(file).use { out ->
                    bitmap.compress(
                        when (format) {
                            com.browserengine.core.capabilities.ImageFormat.PNG -> android.graphics.Bitmap.CompressFormat.PNG
                            com.browserengine.core.capabilities.ImageFormat.JPEG -> android.graphics.Bitmap.CompressFormat.JPEG
                            com.browserengine.core.capabilities.ImageFormat.WEBP -> android.graphics.Bitmap.CompressFormat.WEBP
                        },
                        100,
                        out
                    )
                }
                Unit
            }
        }

    // ArchiveCapable
    override val supportedFormats: List<ArchiveFormat> = listOf(ArchiveFormat.MHTML, ArchiveFormat.HTML)

    override suspend fun savePage(destination: java.io.File, format: ArchiveFormat): Result<Unit> =
        withContext(Dispatchers.Main) {
            when (format) {
                ArchiveFormat.MHTML -> kotlin.runCatching {
                    suspendCancellableCoroutine { cont ->
                        webView.saveWebArchive(destination.absolutePath, false) { path ->
                            if (path != null) cont.resumeWith(Result.success(Unit))
                            else cont.resumeWith(Result.failure(Exception("Save failed")))
                        }
                    }
                }
                ArchiveFormat.HTML -> kotlin.runCatching {
                    suspendCancellableCoroutine { cont ->
                        webView.evaluateJavascript("document.documentElement.outerHTML") { result ->
                            val content = if (result == null || result == "null") ""
                            else if (result.startsWith("\"") && result.endsWith("\"")) result.drop(1).dropLast(1)
                            else result
                            destination.writeText(content)
                            cont.resumeWith(Result.success(Unit))
                        }
                    }
                }
                ArchiveFormat.PDF -> Result.failure(UnsupportedOperationException("WebView does not support PDF"))
            }
        }

    // NavigationCapable
    override suspend fun goBack() = withContext(Dispatchers.Main) { webView.goBack() }
    override suspend fun goForward() = withContext(Dispatchers.Main) { webView.goForward() }
    override suspend fun goTo(historyIndex: Int) = withContext(Dispatchers.Main) {
        webView.goBackOrForward(historyIndex - webView.copyBackForwardList().currentIndex)
    }

    override suspend fun getHistory(): List<HistoryEntry> = withContext(Dispatchers.Main) {
        webView.copyBackForwardList().let { list ->
            (0 until list.size).map { i ->
                HistoryEntry(
                    url = list.getItemAtIndex(i).url,
                    title = list.getItemAtIndex(i).title,
                    visitedAt = System.currentTimeMillis()
                )
            }
        }
    }

    override suspend fun clearHistory() = withContext(Dispatchers.Main) { webView.clearHistory() }

    override fun setUrlFilter(filter: UrlFilter?) { urlFilter = filter }

    // NetworkCapable
    override fun setUserAgent(userAgent: String) { webView.settings.userAgentString = userAgent }
    override fun getUserAgent(): String = webView.settings.userAgentString
    override fun setDefaultHeaders(headers: Map<String, String>) { defaultHeaders = headers }
    override fun setRequestInterceptor(interceptor: RequestInterceptor?) { requestInterceptor = interceptor }
    override fun setJavaScriptEnabled(enabled: Boolean) { webView.settings.javaScriptEnabled = enabled }
    override fun setMixedContentMode(mode: MixedContentMode) {
        webView.settings.mixedContentMode = when (mode) {
            MixedContentMode.NEVER_ALLOW -> WebSettings.MIXED_CONTENT_NEVER_ALLOW
            MixedContentMode.COMPATIBILITY_MODE -> WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            MixedContentMode.ALWAYS_ALLOW -> WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        }
    }

    // MediaCapable
    override fun setAutoplayEnabled(enabled: Boolean) {
        webView.settings.mediaPlaybackRequiresUserGesture = !enabled
    }

    override fun setMediaPolicy(policy: MediaPolicy) {
        webView.settings.mediaPlaybackRequiresUserGesture = policy == MediaPolicy.USER_GESTURE_REQUIRED
    }

    override suspend fun getAvailableCameras(): List<MediaSource> = emptyList()
    override suspend fun getAvailableMicrophones(): List<MediaSource> = emptyList()

    // PopupCapable
    override fun setPopupRequestHandler(handler: PopupRequestHandler?) { popupRequestHandler = handler }
    override fun setPopupCloseHandler(handler: PopupCloseHandler?) { popupCloseHandler = handler }

    // NavigationInterceptCapable
    override fun setNavigationInterceptor(interceptor: NavigationInterceptor?) { navigationInterceptor = interceptor }
    override fun getNavigationInterceptor(): NavigationInterceptor? = navigationInterceptor
}
