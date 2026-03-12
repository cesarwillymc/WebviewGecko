package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability

interface MediaCapable : BrowserCapability {
    /** Enable/disable autoplay. */
    fun setAutoplayEnabled(enabled: Boolean)

    /** Set media playback policy. */
    fun setMediaPolicy(policy: MediaPolicy)

    /** List available camera sources. */
    suspend fun getAvailableCameras(): List<MediaSource>

    /** List available microphones. */
    suspend fun getAvailableMicrophones(): List<MediaSource>
}

enum class MediaPolicy {
    ALLOW_ALL,
    BLOCK_ALL,
    USER_GESTURE_REQUIRED
}
