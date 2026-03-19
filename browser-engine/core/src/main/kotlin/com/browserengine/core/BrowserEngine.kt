package com.browserengine.core

import android.util.Log
import com.browserengine.core.capabilities.ArchiveCapable
import com.browserengine.core.capabilities.ArchiveFormat
import com.browserengine.core.capabilities.JsCapable
import com.browserengine.core.capabilities.MessagingBridgeCapable
import com.browserengine.core.capabilities.NavigationCapable
import com.browserengine.core.capabilities.NavigationInterceptCapable
import com.browserengine.core.capabilities.NavigationInterceptor
import com.browserengine.core.capabilities.PermissionCapable
import com.browserengine.core.capabilities.TemporaryStorageCapable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import kotlin.reflect.KClass

/**
 * Root contract. Exposes NOTHING engine-specific.
 * Every feature lives in a capability interface.
 */
interface BrowserEngine {
    val state: StateFlow<BrowserState>
    val events: Flow<BrowserEvent>

    fun loadUrl(url: String, headers: Map<String, String> = emptyMap())
    fun loadHtml(html: String, baseUrl: String? = null)
    fun reload()
    fun stopLoading()
    fun destroy()

    /** Capability discovery (type-safe) */
    fun <T : BrowserCapability> capability(type: KClass<T>): T?

    suspend fun goBack() {
        capability(NavigationCapable::class)?.goBack()
            ?: logMissingCapability("goBack", NavigationCapable::class)
    }

    suspend fun goForward() {
        capability(NavigationCapable::class)?.goForward()
            ?: logMissingCapability("goForward", NavigationCapable::class)
    }

    suspend fun goTo(historyIndex: Int) {
        capability(NavigationCapable::class)?.goTo(historyIndex)
            ?: logMissingCapability("goTo", NavigationCapable::class)
    }

    suspend fun injectScript(script: String): Result<String> =
        capability(JsCapable::class)?.evaluateScript(script)
            ?: missingCapabilityResult("injectScript", JsCapable::class)

    suspend fun injectScriptFromAssets(assetPath: String): Result<Unit> =
        capability(JsCapable::class)?.injectScriptFromAssets(assetPath)
            ?: missingCapabilityResult("injectScriptFromAssets", JsCapable::class)

    suspend fun postMessageToJs(channel: String, data: String): Result<Unit> =
        capability(JsCapable::class)?.postMessageToJs(channel, data)
            ?: missingCapabilityResult("postMessageToJs", JsCapable::class)

    fun postMessage(message: String) {
        capability(MessagingBridgeCapable::class)?.postMessage(message)
            ?: logMissingCapability("postMessage", MessagingBridgeCapable::class)
    }

    fun setOnErrorHandler(handler: ((String) -> Unit)?) {
        capability(MessagingBridgeCapable::class)?.setOnErrorHandler(handler)
            ?: logMissingCapability("setOnErrorHandler", MessagingBridgeCapable::class)
    }

    fun setOnReadJsonHandler(handler: ((String) -> Unit)?) {
        capability(MessagingBridgeCapable::class)?.setOnReadJsonHandler(handler)
            ?: logMissingCapability("setOnReadJsonHandler", MessagingBridgeCapable::class)
    }

    fun addMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        capability(MessagingBridgeCapable::class)?.addMessageListener(listener)
            ?: logMissingCapability("addMessageListener", MessagingBridgeCapable::class)
    }

    fun removeMessageListener(listener: MessagingBridgeCapable.MessageListener) {
        capability(MessagingBridgeCapable::class)?.removeMessageListener(listener)
            ?: logMissingCapability("removeMessageListener", MessagingBridgeCapable::class)
    }

    fun setPermissionRequestHandler(handler: ((List<String>, () -> Unit, () -> Unit) -> Unit)?) {
        capability(PermissionCapable::class)?.setPermissionRequestHandler(handler)
            ?: logMissingCapability("setPermissionRequestHandler", PermissionCapable::class)
    }

    fun setNavigationInterceptor(interceptor: NavigationInterceptor?) {
        capability(NavigationInterceptCapable::class)?.setNavigationInterceptor(interceptor)
            ?: logMissingCapability("setNavigationInterceptor", NavigationInterceptCapable::class)
    }

    fun getNavigationInterceptor(): NavigationInterceptor? =
        capability(NavigationInterceptCapable::class)?.getNavigationInterceptor().also {
            if (it == null && capability(NavigationInterceptCapable::class) == null) {
                logMissingCapability("getNavigationInterceptor", NavigationInterceptCapable::class)
            }
        }

    suspend fun saveCurrentWebsite(
        directory: File,
        baseName: String? = null
    ): Result<SavedPage> {
        val archive = capability(ArchiveCapable::class)
            ?: return missingCapabilityResult("saveCurrentWebsite", ArchiveCapable::class)

        if (!directory.exists()) {
            directory.mkdirs()
        }

        val format = archive.preferredFormat()
        val file = File(directory, "${resolveBaseName(baseName)}.${format.extension}")
        return archive.savePage(file, format).map { SavedPage(file = file, format = format) }
    }

    suspend fun saveCurrentWebsite(
        baseName: String? = null
    ): Result<SavedPage> {
        val temporaryStorage = capability(TemporaryStorageCapable::class)
            ?: return missingCapabilityResult("saveCurrentWebsite", TemporaryStorageCapable::class)

        return saveCurrentWebsite(
            directory = temporaryStorage.temporaryDirectory(),
            baseName = baseName
        )
    }
}

/** Convenience inline extension */
inline fun <reified T : BrowserCapability> BrowserEngine.capability(): T? =
    capability(T::class)

private const val BROWSER_ENGINE_LOG_TAG = "BrowserEngine"

private fun BrowserEngine.logMissingCapability(
    action: String,
    capability: KClass<out BrowserCapability>
) {
    Log.w(
        BROWSER_ENGINE_LOG_TAG,
        "$action skipped because ${capability.simpleName} is not enabled for this engine"
    )
}

private fun <T> BrowserEngine.missingCapabilityResult(
    action: String,
    capability: KClass<out BrowserCapability>
): Result<T> {
    logMissingCapability(action, capability)
    return Result.failure(
        IllegalStateException("$action requires ${capability.simpleName}")
    )
}

data class SavedPage(
    val file: File,
    val format: ArchiveFormat
)

private fun ArchiveCapable.preferredFormat(): ArchiveFormat =
    when {
        ArchiveFormat.PDF in supportedFormats -> ArchiveFormat.PDF
        ArchiveFormat.MHTML in supportedFormats -> ArchiveFormat.MHTML
        ArchiveFormat.HTML in supportedFormats -> ArchiveFormat.HTML
        else -> supportedFormats.first()
    }

private val ArchiveFormat.extension: String
    get() = when (this) {
        ArchiveFormat.PDF -> "pdf"
        ArchiveFormat.MHTML -> "mhtml"
        ArchiveFormat.HTML -> "html"
    }

private fun BrowserEngine.resolveBaseName(baseName: String?): String {
    val candidate = baseName
        ?: state.value.title.takeIf { it.isNotBlank() }
        ?: state.value.url.substringAfter("://").substringBefore("/").takeIf { it.isNotBlank() }
        ?: "page"

    return candidate
        .trim()
        .replace(Regex("[^A-Za-z0-9._-]+"), "_")
        .trim('_')
        .ifBlank { "page" }
}
