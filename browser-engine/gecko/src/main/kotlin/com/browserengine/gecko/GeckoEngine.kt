package com.browserengine.gecko

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserConfig
import com.browserengine.core.BrowserEngine
import com.browserengine.core.BrowserError
import com.browserengine.core.BrowserEvent
import com.browserengine.core.BrowserState
import com.browserengine.core.capabilities.ArchiveCapable
import com.browserengine.core.capabilities.ArchiveFormat
import com.browserengine.core.capabilities.CacheMode
import com.browserengine.core.capabilities.CookieCapable
import com.browserengine.core.capabilities.HistoryEntry
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MediaCapable
import com.browserengine.core.capabilities.MessagingBridgeCapable
import com.browserengine.core.capabilities.MediaPolicy
import com.browserengine.core.capabilities.MediaSource
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
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.StorageController
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.GeckoSession.Loader
import org.mozilla.geckoview.WebExtension
import org.json.JSONObject
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_MOBILE
import kotlin.reflect.KClass

/**
 * GeckoView implementation of BrowserEngine.
 * Note: GeckoView API varies by version. Some features may need adjustment for your GeckoView build.
 */
class GeckoEngine(
    private val context: Context,
    private val config: BrowserConfig = BrowserConfig()
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

    private val runtime: GeckoRuntime = GeckoRuntime.create(
        context,
        GeckoRuntimeSettings.Builder()
            .remoteDebuggingEnabled(true)
            .contentBlocking(
                ContentBlocking.Settings.Builder()
                    .cookieBehavior(if (config.cookiesEnabled) ContentBlocking.CookieBehavior.ACCEPT_ALL else ContentBlocking.CookieBehavior.ACCEPT_NONE)
                    .build()
            )
            .build()
    )

    val session: GeckoSession = GeckoSession(

        GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .userAgentMode(USER_AGENT_MODE_MOBILE)
            .apply {
                config.userAgent?.let { userAgent->
                    userAgentOverride(      userAgent)
                }
            }
            .suspendMediaWhenInactive(true)
            .allowJavascript(config.javaScriptEnabled)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .displayMode(GeckoSessionSettings.DISPLAY_MODE_FULLSCREEN)
            .build()
    ).apply {

        navigationDelegate = createNavigationDelegate()
        contentDelegate = createContentDelegate()
        progressDelegate = createProgressDelegate()
        permissionDelegate = createPermissionDelegate()
    }

    val geckoView: GeckoView = GeckoView(context).apply {
        setSession(this@GeckoEngine.session)
    }

    private val messageListeners = mutableListOf<MessagingBridgeCapable.MessageListener>()
    private var onErrorHandler: ((String) -> Unit)? = null
    private var onReadJsonHandler: ((String) -> Unit)? = null
    private val messagingPorts = mutableListOf<WebExtension.Port>()
    private val pendingMessages = mutableListOf<JSONObject>()  // queue for when port is gone
    private var isPortReady = false

    init {
        session.open(runtime)
        installMessagingExtension()
    }

    @SuppressLint("WrongThread")
    private fun installMessagingExtension() {
        runtime.webExtensionController.ensureBuiltIn(
            "resource://android/assets/messaging/",
            "messaging@bautopilot.bridge.com"
        ).accept(

            { extension ->
                if (extension == null) return@accept
                val delegate = object : WebExtension.MessageDelegate {
                    override fun onConnect(port: WebExtension.Port) {
                        Log.d("ScriptInjection", "onConnect ${port.name} ${port.sender.environmentType} ${port.sender.url} ${port.sender.session?.userAgent}")
                        port.setDelegate(object : WebExtension.PortDelegate {
                            override fun onPortMessage(message: Any, port: WebExtension.Port) {
                                Log.d("ScriptInjection", "onPortMessage: $message")
                            }

                            override fun onDisconnect(port: WebExtension.Port) {
                                Log.d("ScriptInjection", "onDisconnect ${port.name}")
                                messagingPorts.remove(port)
                                isPortReady = messagingPorts.isNotEmpty() // still ready if other ports exist
                                Log.d("ScriptInjection", "Remaining ports: ${messagingPorts.size}")
                            }
                        })
                        messagingPorts.add(port)
                        isPortReady = true

// Flush any messages that were queued while port was gone
                        flushPendingMessages()
                    }

                    override fun onMessage(
                        nativeApp: String,
                        message: Any,
                        sender: WebExtension.MessageSender
                    ): GeckoResult<Any>? {
                        Log.d("ScriptInjection", "onMessage: $message")
                        val text = when (message) {
                            is JSONObject -> message.optString("text", message.optString("message", message.toString()))
                            is String -> message
                            else -> message.toString()
                        }
                        val method = (message as? JSONObject)?.optString("method", "onReadJson") ?: "onReadJson"
                        when (method) {
                            "onError" -> onErrorHandler?.invoke(text)
                            "onReadJson" -> onReadJsonHandler?.invoke(text)
                        }
                        val response = messageListeners
                            .firstNotNullOfOrNull { it.onMessage(text) }
                        return if (response != null) {
                            GeckoResult.fromValue(JSONObject().apply { put("reply", response) })
                        } else {
                            GeckoResult.fromValue(null)
                        }
                    }
                }
                session.webExtensionController.setMessageDelegate(extension, delegate, "browser")
            },
            { e -> Log.e("GeckoEngine", "Messaging extension install failed", e) }
        )
    }
    // Add this helper
    private fun flushPendingMessages() {
        if (messagingPorts.isEmpty()) return
        val iterator = pendingMessages.iterator()
        while (iterator.hasNext()) {
            val payload = iterator.next()
            try {
                messagingPorts.forEach { it.postMessage(payload) }
                iterator.remove()
                Log.d("ScriptInjection", "Flushed queued message: $payload")
            } catch (e: Exception) {
                Log.w("GeckoEngine", "Failed to flush message", e)
                break // stop flushing, port may be broken
            }
        }
    }
    private var defaultHeaders: Map<String, String> = emptyMap()
    private var requestInterceptor: RequestInterceptor? = null
    private var navigationInterceptor: NavigationInterceptor? = null
    private var urlFilter: UrlFilter? = null
    private var popupRequestHandler: PopupRequestHandler? = null
    private var popupCloseHandler: PopupCloseHandler? = null
    override var allowBackgroundPopups: Boolean = false

    private var permissionRequestHandler: ((List<String>, () -> Unit, () -> Unit) -> Unit)? = null

    private fun createNavigationDelegate() = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(session: GeckoSession, url: String?, perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>, hasUserGesture: Boolean) {
            _state.value = _state.value.copy(url = url ?: "")
            scope.launch { _events.emit(BrowserEvent.UrlChanged(url ?: "")) }
        }

        override fun onLoadRequest(session: GeckoSession, request: GeckoSession.NavigationDelegate.LoadRequest): GeckoResult<AllowOrDeny>? {
            val interceptor = navigationInterceptor ?: return null
            val result = interceptor.intercept(
                com.browserengine.core.capabilities.NavigationRequest(
                    url = request.uri,
                    method = "GET",
                    isRedirect = false,
                    isUserInitiated = request.isDirectNavigation,
                    com.browserengine.core.capabilities.NavigationTrigger.LINK_CLICK
                )
            )
            return when (result) {
                is com.browserengine.core.capabilities.NavigationResult.Allow -> GeckoResult.fromValue(AllowOrDeny.ALLOW)
                is com.browserengine.core.capabilities.NavigationResult.Block -> GeckoResult.fromValue(AllowOrDeny.DENY)
                is com.browserengine.core.capabilities.NavigationResult.ConsumedByApp -> GeckoResult.fromValue(AllowOrDeny.DENY)
                is com.browserengine.core.capabilities.NavigationResult.Redirect -> {
                    session.load(Loader().uri(result.newUrl))
                    GeckoResult.fromValue(AllowOrDeny.DENY)
                }
            }
        }

        override fun onNewSession(session: GeckoSession, uri: String): GeckoResult<GeckoSession>? {
            if (!allowBackgroundPopups) return GeckoResult.fromValue(null)
            val handler = popupRequestHandler ?: return GeckoResult.fromValue(null)
            val result = GeckoResult<GeckoSession>()
            handler.onPopupRequested(
                opener = this@GeckoEngine,
                uri = uri,
                isUserGesture = true,
                onAllow = { newEngine ->
                    (newEngine as? GeckoEngine)?.session?.let { result.complete(it) }
                        ?: result.complete(null)
                },
                onBlock = { result.complete(null) }
            )
            return result
        }
    }

    private fun createContentDelegate() = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            _state.value = _state.value.copy(title = title ?: "")
            scope.launch { _events.emit(BrowserEvent.TitleChanged(title ?: "")) }
        }

        override fun onCloseRequest(session: GeckoSession) {
            val popupEngine = popupEngines.find { (it as? GeckoEngine)?.session == session }
            popupEngine?.let { popupCloseHandler?.onPopupCloseRequested(it) }
        }
    }

    private fun createProgressDelegate() = object : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            _state.value = _state.value.copy(url = url, isLoading = true, progress = 0f, error = null)
            scope.launch { _events.emit(BrowserEvent.PageStarted(url)) }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            _state.value = _state.value.copy(
                isLoading = false,
                progress = 1f,
                canGoBack = false,
                canGoForward = false,
                error = if (!success) BrowserError(-1, "Load failed", _state.value.url) else null
            )
            scope.launch { _events.emit(BrowserEvent.PageFinished(_state.value.url, success)) }
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            _state.value = _state.value.copy(progress = progress / 100f)
        }
    }

    private fun createPermissionDelegate() = object : GeckoSession.PermissionDelegate {
        override fun onAndroidPermissionsRequest(
            session: GeckoSession,
            permissions: Array<out String>?,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            Log.e(
                "createPermissionDelegate",
                "onAndroidPermissionsRequest frommm android requesttt: ${permissions}"
            )
            permissionRequestHandler?.invoke(
                permissions.orEmpty().toList(),
                { callback.grant() },
                { callback.reject() }
            ) ?: callback.reject()
        }
    }

    private val popupEngines = mutableListOf<BrowserEngine>()

    override fun setPermissionRequestHandler(handler: ((List<String>, () -> Unit, () -> Unit) -> Unit)?) {
        permissionRequestHandler = handler
    }

    override fun loadUrl(url: String, headers: Map<String, String>) {
        val allHeaders = defaultHeaders + headers
        if (allHeaders.isEmpty()) {
            session.loadUri(url)
        } else {
            session.load(Loader().uri(url).additionalHeaders(allHeaders))
        }
    }

    override fun loadHtml(html: String, baseUrl: String?) {
        session.load(Loader().data(html.toByteArray(Charsets.UTF_8), "text/html; charset=utf-8"))
    }

    override fun reload() = session.reload()
    override fun stopLoading() = session.stop()
    override fun destroy() {
        session.close()
        runtime.shutdown()
    }

    override fun <T : BrowserCapability> capability(type: KClass<T>): T? = when (type) {
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

    @androidx.compose.runtime.Composable
    override fun RenderUI(modifier: androidx.compose.ui.Modifier) {
        androidx.compose.ui.viewinterop.AndroidView(
            factory = { geckoView },
            modifier = modifier
        )
    }
    private val SEND_MESSAGE_REPLACEMENT = """
function sendMessageToHost(message, isError = false) {
    window.postMessage({
      type: "FROM_PAGE",
      method: isError ? "onError" : "onReadJson",
      message: message
    }, "*");
}
""".trimIndent()

    private val FUNCTION_START = Regex("""function\s+sendMessageToHost\s*\([^)]*\)\s*\{""")


    private fun replaceSendMessageToHost(script: String): String {
        val result = StringBuilder()
        var searchFrom = 0

        while (true) {
            val match = FUNCTION_START.find(script, searchFrom) ?: break

            // Append everything before this match
            result.append(script, searchFrom, match.range.first)
            result.append(SEND_MESSAGE_REPLACEMENT)

            // Walk forward counting braces to find the matching closing }
            var depth = 0
            var i = match.range.last // points at the opening {

            while (i < script.length) {
                when (script[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            // Skip past this closing brace and continue scanning
                            searchFrom = i + 1
                            break
                        }
                    }
                }
                i++
            }

            if (depth != 0) {
                // Unbalanced braces — bail out, append the rest as-is
                result.append(script, match.range.first, script.length)
                return result.toString()
            }
        }

        // Append whatever remains after the last replacement
        result.append(script, searchFrom, script.length)
        return result.toString()
    }
    // Update evaluateScript to queue instead of fail:
    override suspend fun evaluateScript(script: String): Result<String> =
        withContext(Dispatchers.Main) {
            val patched = replaceSendMessageToHost(script)
            val payload = JSONObject().apply {
                put("action", "evaluate")
                put("script", patched)
            }
            if (messagingPorts.isEmpty()) {
                pendingMessages.add(payload)  // ← queue it, don't fail
                return@withContext Result.success("queued")
            }
            try {
                messagingPorts.forEach { it.postMessage(payload) }
                Result.success("")
            } catch (e: Exception) {
                pendingMessages.add(payload) // ← queue on send failure too
                Result.failure(e)
            }
        }

    // Same pattern for postMessageToJs:
    override suspend fun postMessageToJs(channel: String, data: String): Result<Unit> =
        withContext(Dispatchers.Main) {
            val payload = JSONObject().apply {
                put("action", "postMessage")
                put("channel", channel)
                put("data", data)
            }
            if (messagingPorts.isEmpty()) {
                pendingMessages.add(payload)
                return@withContext Result.success(Unit) // don't crash callers
            }
            try {
                messagingPorts.forEach { it.postMessage(payload) }
                Result.success(Unit)
            } catch (e: Exception) {
                pendingMessages.add(payload)
                Result.failure(e)
            }
        }

    override suspend fun injectScriptFromAssets(assetPath: String): Result<Unit> =
        Result.failure(UnsupportedOperationException("GeckoView requires WebExtension for script injection"))



    // MessagingBridgeCapable
    override val bridgeName: String get() = "browser"

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
        val payload = JSONObject().apply { put("text", message) }
        messagingPorts.forEach { port ->
            try {
                port.postMessage(payload)
            } catch (e: Exception) {
                Log.w("GeckoEngine", "postMessage to port failed", e)
            }
        }
    }

    override fun onPermissionRequested(
        permissions: List<String>,
        onGrant: () -> Unit,
        onDeny: () -> Unit
    ) {
        Log.w("GeckoEngine", "onPermissionRequested permissions ${permissions}")
        permissionRequestHandler?.invoke(permissions, onGrant, onDeny) ?: onDeny()
    }

    override fun grantContentPermission(permission: com.browserengine.core.capabilities.BrowserPermission) {}
    override fun denyContentPermission(permission: com.browserengine.core.capabilities.BrowserPermission) {}
    override fun selectMediaSource(
        videoSources: List<MediaSource>,
        audioSources: List<MediaSource>,
        onSelected: (video: MediaSource?, audio: MediaSource?) -> Unit
    ) {
        onSelected(videoSources.firstOrNull(), audioSources.firstOrNull())
    }


    override suspend fun getCookies(url: String): List<com.browserengine.core.capabilities.BrowserCookie> = emptyList()

    override suspend fun setCookie(url: String, cookie: com.browserengine.core.capabilities.BrowserCookie) {
        // GeckoView has no public API to set individual cookies.
    }

    override suspend fun clearCookies() = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine<Unit> { cont ->
            runtime.storageController.clearData(StorageController.ClearFlags.COOKIES)
        }
    }

    override suspend fun clearCookiesFor(url: String) = withContext(Dispatchers.Main) {
        val host = try { java.net.URL(url).host } catch (_: Exception) { return@withContext }
        suspendCancellableCoroutine { _ ->
            runtime.storageController.clearDataFromHost(host, StorageController.ClearFlags.COOKIES)
        }
    }

    override fun setThirdPartyCookiesEnabled(enabled: Boolean) {
        // Gecko: use ContentBlocking.CookieBehavior.ACCEPT_FIRST_PARTY to block third-party.
        // Set at runtime creation via ContentBlocking.Settings.
    }

    override suspend fun clearCache() = withContext(Dispatchers.Main) { Unit }

    override suspend fun clearHistory() = withContext(Dispatchers.Main) {
        session.purgeHistory()
    }

    override suspend fun clearWebStorage() = withContext(Dispatchers.Main) { Unit }

    override suspend fun clearAll() = withContext(Dispatchers.Main) {
        session.purgeHistory()
    }

    override fun setCacheMode(mode: CacheMode) {}
    override fun setDomStorageEnabled(enabled: Boolean) {}

    override suspend fun captureScreenshot(): Result<android.graphics.Bitmap> =
        Result.failure(UnsupportedOperationException("GeckoView capturePixels requires GeckoDisplay access"))

    override suspend fun saveScreenshotTo(file: java.io.File, format: com.browserengine.core.capabilities.ImageFormat): Result<Unit> =
        Result.failure(UnsupportedOperationException("GeckoView screenshot not implemented"))

    override val supportedFormats: List<ArchiveFormat> = listOf(ArchiveFormat.PDF, ArchiveFormat.HTML)

    override suspend fun savePage(destination: java.io.File, format: ArchiveFormat): Result<Unit> =
        Result.failure(UnsupportedOperationException("GeckoView saveAsPdf/savePage - implement per GeckoView version"))

    override suspend fun goBack() = withContext(Dispatchers.Main) { session.goBack() }
    override suspend fun goForward() = withContext(Dispatchers.Main) { session.goForward() }
    override suspend fun goTo(historyIndex: Int) = Unit

    override suspend fun getHistory(): List<HistoryEntry> = emptyList()

    override fun setUrlFilter(filter: UrlFilter?) { urlFilter = filter }
    override fun setUserAgent(userAgent: String) {
        TODO("Not yet implemented")
    }

    override fun getUserAgent(): String = ""
    override fun setDefaultHeaders(headers: Map<String, String>) { defaultHeaders = headers }
    override fun setRequestInterceptor(interceptor: RequestInterceptor?) { requestInterceptor = interceptor }
    override fun setJavaScriptEnabled(enabled: Boolean) {
        runtime.settings.javaScriptEnabled = enabled
    }

    override fun setMixedContentMode(mode: MixedContentMode) {}

    override fun setAutoplayEnabled(enabled: Boolean) {}
    override fun setMediaPolicy(policy: MediaPolicy) {}
    override suspend fun getAvailableCameras(): List<MediaSource> = emptyList()
    override suspend fun getAvailableMicrophones(): List<MediaSource> = emptyList()

    override fun setPopupRequestHandler(handler: PopupRequestHandler?) { popupRequestHandler = handler }
    override fun setPopupCloseHandler(handler: PopupCloseHandler?) { popupCloseHandler = handler }

    override fun setNavigationInterceptor(interceptor: NavigationInterceptor?) { navigationInterceptor = interceptor }
    override fun getNavigationInterceptor(): NavigationInterceptor? = navigationInterceptor
}
