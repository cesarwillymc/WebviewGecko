package com.browserengine.gecko

import android.content.Context
import com.browserengine.core.BrowserConfig
import org.mozilla.geckoview.ContentBlocking
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoSessionSettings.USER_AGENT_MODE_MOBILE
import org.mozilla.geckoview.GeckoView

class GeckoRuntimeBundle(
    context: Context,
    config: BrowserConfig,
    delegates: GeckoDelegateHub
) {
    val runtime: GeckoRuntime = GeckoRuntime.create(context, createRuntimeSettings(config))
    val session: GeckoSession = GeckoSession(createSessionSettings(config)).apply {
        navigationDelegate = delegates.navigationDelegate
        contentDelegate = delegates.contentDelegate
        progressDelegate = delegates.progressDelegate
        permissionDelegate = delegates.permissionDelegate
    }
    val view: GeckoView = GeckoView(context).apply {
        setSession(this@GeckoRuntimeBundle.session)
    }

    fun open() {
        session.open(runtime)
    }

    fun destroy() {
        session.close()
        runtime.shutdown()
    }

    private fun createRuntimeSettings(config: BrowserConfig): GeckoRuntimeSettings =
        GeckoRuntimeSettings.Builder()
            .remoteDebuggingEnabled(true)
            .contentBlocking(
                ContentBlocking.Settings.Builder()
                    .cookieBehavior(
                        if (config.cookiesEnabled) {
                            ContentBlocking.CookieBehavior.ACCEPT_ALL
                        } else {
                            ContentBlocking.CookieBehavior.ACCEPT_NONE
                        }
                    )
                    .build()
            )
            .build()

    private fun createSessionSettings(config: BrowserConfig): GeckoSessionSettings =
        GeckoSessionSettings.Builder()
            .usePrivateMode(false)
            .useTrackingProtection(true)
            .userAgentMode(USER_AGENT_MODE_MOBILE)
            .apply {
                config.userAgent?.let(::userAgentOverride)
            }
            .suspendMediaWhenInactive(true)
            .allowJavascript(config.javaScriptEnabled)
            .viewportMode(GeckoSessionSettings.VIEWPORT_MODE_MOBILE)
            .displayMode(GeckoSessionSettings.DISPLAY_MODE_FULLSCREEN)
            .build()
}
