package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability
import kotlinx.coroutines.flow.StateFlow

interface PermissionCapable : BrowserCapability {
    /** Permissions currently granted */
    val grantedPermissions: StateFlow<Set<BrowserPermission>>

    /**
     * Called when engine needs Android-level permission.
     * Both engines need: CAMERA, RECORD_AUDIO, ACCESS_FINE_LOCATION
     * WebView: onPermissionRequest → PermissionRequest.grant()
     * Gecko: onAndroidPermissionsRequest → Callback.grant()
     */
    fun onPermissionRequested(
        permissions: List<BrowserPermission>,
        onGrant: () -> Unit,
        onDeny: () -> Unit
    )

    /**
     * Grant a web content permission (geolocation, notifications, etc).
     * WebView: PermissionRequest.grant(resources)
     * Gecko: ContentPermission VALUE_ALLOW
     */
    fun grantContentPermission(permission: BrowserPermission)
    fun denyContentPermission(permission: BrowserPermission)

    /**
     * For media (camera/mic): select which source to use.
     * WebView: PermissionRequest.grant()
     * Gecko: MediaCallback.grant(videoSource, audioSource)
     */
    fun selectMediaSource(
        videoSources: List<MediaSource>,
        audioSources: List<MediaSource>,
        onSelected: (video: MediaSource?, audio: MediaSource?) -> Unit
    )
}

enum class BrowserPermission {
    CAMERA,
    MICROPHONE,
    LOCATION_FINE,
    LOCATION_COARSE,
    NOTIFICATIONS,
    STORAGE,
    AUTOPLAY,
    DRM,
    XR
}

data class MediaSource(
    val id: String,
    val name: String,
    val type: MediaType,
    val source: SourceType
)

enum class MediaType { VIDEO, AUDIO }

enum class SourceType { CAMERA, MICROPHONE, SCREEN }
