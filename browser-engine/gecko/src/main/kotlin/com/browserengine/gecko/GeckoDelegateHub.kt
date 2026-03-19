package com.browserengine.gecko

import android.util.Log
import com.browserengine.core.BrowserEngine
import com.browserengine.core.capabilities.BrowserPermission
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.NavigationRequest
import com.browserengine.core.capabilities.NavigationResult
import com.browserengine.core.capabilities.NavigationTrigger
import com.browserengine.core.capabilities.PopupCloseHandler
import com.browserengine.core.capabilities.PopupRequestHandler
import com.browserengine.core.capabilities.UrlFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSession.Loader

class GeckoDelegateHub(
    private val stateStore: GeckoStateStore
) {
    private lateinit var engine: BrowserEngine
    private val popupEngines = mutableListOf<BrowserEngine>()
    private val _grantedPermissions = MutableStateFlow<Set<BrowserPermission>>(emptySet())

    var navigationInterceptor: NavigationInterceptor? = null
    var permissionRequestHandler: ((List<String>, () -> Unit, () -> Unit) -> Unit)? = null
    var urlFilter: UrlFilter? = null
    var popupRequestHandler: PopupRequestHandler? = null
    var popupCloseHandler: PopupCloseHandler? = null
    var allowBackgroundPopups: Boolean = false

    val grantedPermissions: StateFlow<Set<BrowserPermission>> = _grantedPermissions.asStateFlow()

    fun attach(engine: BrowserEngine) {
        this.engine = engine
    }

    val navigationDelegate = object : GeckoSession.NavigationDelegate {
        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: MutableList<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean
        ) {
            stateStore.updateUrl(url.orEmpty())
        }

        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest
        ): GeckoResult<AllowOrDeny>? {
            if (urlFilter?.shouldAllow(request.uri) == false) {
                return GeckoResult.fromValue(AllowOrDeny.DENY)
            }

            val interceptor = navigationInterceptor ?: return null
            return when (val result = interceptor.intercept(request.toNavigationRequest())) {
                NavigationResult.Allow -> GeckoResult.fromValue(AllowOrDeny.ALLOW)
                NavigationResult.Block -> GeckoResult.fromValue(AllowOrDeny.DENY)
                NavigationResult.ConsumedByApp -> GeckoResult.fromValue(AllowOrDeny.DENY)
                is NavigationResult.Redirect -> {
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
                opener = engine,
                uri = uri,
                isUserGesture = true,
                onAllow = { newEngine ->
                    popupEngines += newEngine
                    result.complete((newEngine as? GeckoEngine)?.session)
                },
                onBlock = { result.complete(null) }
            )
            return result
        }
    }

    val contentDelegate = object : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            stateStore.updateTitle(title.orEmpty())
        }

        override fun onCloseRequest(session: GeckoSession) {
            val popupEngine = popupEngines.find { (it as? GeckoEngine)?.session == session } ?: return
            popupEngines.remove(popupEngine)
            popupCloseHandler?.onPopupCloseRequested(popupEngine)
        }
    }

    val progressDelegate = object : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            stateStore.onPageStarted(url)
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            stateStore.onPageFinished(
                success = success,
                canGoBack = false,
                canGoForward = false
            )
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            stateStore.updateProgress(progress / 100f)
        }
    }

    val permissionDelegate = object : GeckoSession.PermissionDelegate {
        override fun onAndroidPermissionsRequest(
            session: GeckoSession,
            permissions: Array<out String>?,
            callback: GeckoSession.PermissionDelegate.Callback
        ) {
            permissionRequestHandler?.invoke(
                permissions.orEmpty().toList(),
                { callback.grant() },
                { callback.reject() }
            ) ?: callback.reject()
        }

        override fun onContentPermissionRequest(
            session: GeckoSession,
            perm: GeckoSession.PermissionDelegate.ContentPermission
        ): GeckoResult<Int> {
            val result = GeckoResult<Int>()
            permissionRequestHandler?.invoke(
                listOf(permissionTypeToString(perm.permission)),
                { result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_ALLOW) },
                { result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY) }
            ) ?: result.complete(GeckoSession.PermissionDelegate.ContentPermission.VALUE_DENY)
            return result
        }

        override fun onMediaPermissionRequest(
            session: GeckoSession,
            uri: String,
            video: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            audio: Array<out GeckoSession.PermissionDelegate.MediaSource>?,
            callback: GeckoSession.PermissionDelegate.MediaCallback
        ) {
            val requestedPermissions = buildList {
                if (!video.isNullOrEmpty()) add("video")
                if (!audio.isNullOrEmpty()) add("audio")
            }

            if (requestedPermissions.isEmpty()) {
                callback.reject()
                return
            }

            permissionRequestHandler?.invoke(
                requestedPermissions,
                { callback.grant(video?.firstOrNull(), audio?.firstOrNull()) },
                { callback.reject() }
            ) ?: callback.reject()
        }
    }

    private fun GeckoSession.NavigationDelegate.LoadRequest.toNavigationRequest() = NavigationRequest(
        url = uri,
        method = "GET",
        isRedirect = false,
        isUserInitiated = isDirectNavigation,
        trigger = NavigationTrigger.LINK_CLICK
    )

    private fun permissionTypeToString(type: Int): String = when (type) {
        GeckoSession.PermissionDelegate.PERMISSION_GEOLOCATION -> "geolocation"
        GeckoSession.PermissionDelegate.PERMISSION_DESKTOP_NOTIFICATION -> "notification"
        GeckoSession.PermissionDelegate.PERMISSION_PERSISTENT_STORAGE -> "persistent_storage"
        GeckoSession.PermissionDelegate.PERMISSION_XR -> "xr"
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_INAUDIBLE -> "autoplay_inaudible"
        GeckoSession.PermissionDelegate.PERMISSION_AUTOPLAY_AUDIBLE -> "autoplay_audible"
        GeckoSession.PermissionDelegate.PERMISSION_MEDIA_KEY_SYSTEM_ACCESS -> "media_key_system"
        GeckoSession.PermissionDelegate.PERMISSION_TRACKING -> "tracking"
        GeckoSession.PermissionDelegate.PERMISSION_STORAGE_ACCESS -> "storage_access"
        GeckoSession.PermissionDelegate.PERMISSION_LOCAL_DEVICE_ACCESS -> "local_device"
        GeckoSession.PermissionDelegate.PERMISSION_LOCAL_NETWORK_ACCESS -> "local_network"
        else -> {
            Log.w("GeckoEngine", "Unknown permission type: $type")
            "permission_$type"
        }
    }
}
