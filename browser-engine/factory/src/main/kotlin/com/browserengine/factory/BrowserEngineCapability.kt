package com.browserengine.factory

import android.util.Log
import com.browserengine.core.BrowserCapability
import com.browserengine.core.BrowserEngine
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MessagingBridgeCapable
import com.browserengine.core.capabilities.NavigationCapable
import com.browserengine.core.capabilities.NavigationInterceptCapable
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.PermissionCapable
import kotlin.reflect.KClass

private const val CAPABILITY_LOG_TAG = "BrowserEngineFactory"

fun interface BrowserEngineCapability {
    fun apply(engine: BrowserEngine): Set<KClass<out BrowserCapability>>
}

object BrowserCapabilities {
    fun javaScript(): BrowserEngineCapability = expose(JsCapable::class)

    fun messaging(
        onError: ((String) -> Unit)? = null,
        onReadJson: ((String) -> Unit)? = null,
        listener: MessagingBridgeCapable.MessageListener? = null
    ): BrowserEngineCapability = BrowserEngineCapability { engine ->
        val messaging = engine.capability(MessagingBridgeCapable::class)
        if (messaging == null) {
            logUnsupported(MessagingBridgeCapable::class)
            emptySet()
        } else {
            messaging.setOnErrorHandler(onError)
            messaging.setOnReadJsonHandler(onReadJson)
            listener?.let(messaging::addMessageListener)
            setOf(MessagingBridgeCapable::class)
        }
    }

    fun permissions(
        handler: (List<String>, () -> Unit, () -> Unit) -> Unit
    ): BrowserEngineCapability = BrowserEngineCapability { engine ->
        val permissionCapable = engine.capability(PermissionCapable::class)
        if (permissionCapable == null) {
            logUnsupported(PermissionCapable::class)
            emptySet()
        } else {
            permissionCapable.setPermissionRequestHandler(handler)
            setOf(PermissionCapable::class)
        }
    }

    fun navigationOverride(
        interceptor: NavigationInterceptor
    ): BrowserEngineCapability = BrowserEngineCapability { engine ->
        val interceptCapable = engine.capability(NavigationInterceptCapable::class)
        if (interceptCapable == null) {
            logUnsupported(NavigationInterceptCapable::class)
            emptySet()
        } else {
            interceptCapable.setNavigationInterceptor(interceptor)
            setOf(NavigationInterceptCapable::class)
        }
    }

    fun navigation(): BrowserEngineCapability = expose(NavigationCapable::class)

    private fun expose(
        capabilityClass: KClass<out BrowserCapability>
    ): BrowserEngineCapability = BrowserEngineCapability { engine ->
        if (engine.capability(capabilityClass) == null) {
            logUnsupported(capabilityClass)
            emptySet()
        } else {
            setOf(capabilityClass)
        }
    }

    private fun logUnsupported(capabilityClass: KClass<out BrowserCapability>) {
        Log.w(
            CAPABILITY_LOG_TAG,
            "${capabilityClass.simpleName} is not supported by the selected engine"
        )
    }
}
