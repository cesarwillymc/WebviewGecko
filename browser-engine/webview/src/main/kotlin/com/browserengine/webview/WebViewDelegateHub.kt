package com.browserengine.webview

import android.graphics.Bitmap
import android.os.Message
import android.webkit.PermissionRequest
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.browserengine.core.BrowserEngine
import com.browserengine.core.capabilities.BrowserPermission
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.NavigationRequest
import com.browserengine.core.capabilities.NavigationResult
import com.browserengine.core.capabilities.NavigationTrigger
import com.browserengine.core.capabilities.PopupCloseHandler
import com.browserengine.core.capabilities.PopupRequestHandler
import com.browserengine.core.capabilities.RequestInterceptor
import com.browserengine.core.capabilities.UrlFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class WebViewDelegateHub(
    private val stateStore: WebViewStateStore
) {
    private lateinit var engine: BrowserEngine
    private val popupEngines = mutableListOf<BrowserEngine>()
    private val mutableGrantedPermissions = MutableStateFlow<Set<BrowserPermission>>(emptySet())

    var navigationInterceptor: NavigationInterceptor? = null
    var urlFilter: UrlFilter? = null
    var popupRequestHandler: PopupRequestHandler? = null
    var popupCloseHandler: PopupCloseHandler? = null
    var allowBackgroundPopups: Boolean = false
    var permissionRequestHandler: ((List<String>, () -> Unit, () -> Unit) -> Unit)? = null
    var pendingPermissionGrant: (() -> Unit)? = null
    var pendingPermissionDeny: (() -> Unit)? = null
    var requestInterceptor: RequestInterceptor? = null

    val grantedPermissions: StateFlow<Set<BrowserPermission>> = mutableGrantedPermissions.asStateFlow()

    fun attach(engine: BrowserEngine) {
        this.engine = engine
    }

    val webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
            if (urlFilter?.shouldAllow(request.url.toString()) == false) {
                return true
            }

            val interceptor = navigationInterceptor ?: return false
            return when (
                val result = interceptor.intercept(
                    NavigationRequest(
                        url = request.url.toString(),
                        method = request.method,
                        isRedirect = request.isRedirect,
                        isUserInitiated = request.hasGesture(),
                        trigger = NavigationTrigger.LINK_CLICK
                    )
                )
            ) {
                NavigationResult.Allow -> false
                NavigationResult.Block -> true
                NavigationResult.ConsumedByApp -> true
                is NavigationResult.Redirect -> {
                    view.loadUrl(result.newUrl)
                    true
                }
            }
        }

        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            stateStore.onPageStarted(url.orEmpty())
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            stateStore.onPageFinished(
                url = url.orEmpty(),
                title = view?.title.orEmpty(),
                canGoBack = view?.canGoBack() == true,
                canGoForward = view?.canGoForward() == true
            )
        }

        override fun onReceivedError(
            view: WebView?,
            errorCode: Int,
            description: String?,
            failingUrl: String?
        ) {
            stateStore.onReceivedError(
                errorCode = errorCode,
                description = description.orEmpty(),
                failingUrl = failingUrl.orEmpty()
            )
        }
    }

    val webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            stateStore.updateProgress(newProgress / 100f)
        }

        override fun onReceivedTitle(view: WebView?, title: String?) {
            stateStore.updateTitle(title.orEmpty())
        }

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            if (!allowBackgroundPopups && !isUserGesture) return false
            val handler = popupRequestHandler ?: return false

            handler.onPopupRequested(
                opener = engine,
                uri = null,
                isUserGesture = isUserGesture,
                onAllow = onAllow@{ newEngine ->
                    val popupWebView = (newEngine as? WebViewEngine)?.webView ?: return@onAllow
                    val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return@onAllow
                    transport.webView = popupWebView
                    resultMsg.sendToTarget()
                    synchronized(popupEngines) { popupEngines += newEngine }
                },
                onBlock = {}
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

        override fun onPermissionRequest(request: PermissionRequest?) {
            request ?: return
            val resources = request.resources ?: run {
                request.deny()
                return
            }
            if (resources.isEmpty()) {
                request.deny()
                return
            }

            pendingPermissionGrant = { request.grant(resources) }
            pendingPermissionDeny = { request.deny() }
            permissionRequestHandler?.invoke(
                resources.toList(),
                { request.grant(resources) },
                { request.deny() }
            ) ?: request.deny()
        }
    }
}
