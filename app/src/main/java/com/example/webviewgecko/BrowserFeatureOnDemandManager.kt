package com.example.webviewgecko

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.browserengine.core.EngineType
import com.google.android.play.core.ktx.requestProgressFlow
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

data class BrowserFeatureSheetState(
    val isVisible: Boolean = false,
    val title: String = "",
    val description: String = "",
    val isDownloading: Boolean = false
)

interface BrowserFeatureOnDemand {
    val sheetState: StateFlow<BrowserFeatureSheetState>

    fun featureIsAvailable(type: EngineType): Boolean
    fun requestDownloadFeature(type: EngineType)
    suspend fun downloadAndShowModal(type: EngineType, blockedAction: suspend () -> Unit)
}

interface BrowserEngineRemoteConfig {
    fun isFeatureEnabled(type: EngineType): Boolean
    fun startDownloaded(type: EngineType): Boolean
}

@Singleton
class SimulatedBrowserEngineRemoteConfig @Inject constructor() : BrowserEngineRemoteConfig {
    override fun isFeatureEnabled(type: EngineType): Boolean = true

    override fun startDownloaded(type: EngineType): Boolean = when (type) {
        EngineType.WEBVIEW -> true
        EngineType.GECKO -> false
    }
}

@Singleton
class BrowserFeatureOnDemandManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val remoteConfig: SimulatedBrowserEngineRemoteConfig
) : BrowserFeatureOnDemand {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val splitInstallManager: SplitInstallManager = SplitInstallManagerFactory.create(context)
    private val mutableSheetState = MutableStateFlow(BrowserFeatureSheetState())

    override val sheetState: StateFlow<BrowserFeatureSheetState> = mutableSheetState.asStateFlow()

    override fun featureIsAvailable(type: EngineType): Boolean {
        if (!remoteConfig.isFeatureEnabled(type)) return false
        return when (type) {
            EngineType.WEBVIEW -> true
            EngineType.GECKO -> remoteConfig.startDownloaded(type) ||
                moduleName(type) in splitInstallManager.installedModules
        }
    }

    override fun requestDownloadFeature(type: EngineType) {
        scope.launch {
            downloadFeature(type, showModal = false)
        }
    }

    override suspend fun downloadAndShowModal(type: EngineType, blockedAction: suspend () -> Unit) {
        if (featureIsAvailable(type)) {
            registerFeature(type)
            blockedAction()
            return
        }

        downloadFeature(type, showModal = true)
        blockedAction()
    }

    fun buildValidation(type: EngineType): Pair<Boolean, String?> {
        if (!remoteConfig.isFeatureEnabled(type)) {
            return false to "$type is disabled by remote config"
        }
        if (!featureIsAvailable(type)) {
            return false to "$type is not downloaded yet"
        }
        return true to null
    }

    private suspend fun downloadFeature(type: EngineType, showModal: Boolean) {
        if (featureIsAvailable(type)) {
            registerFeature(type)
            return
        }

        if (showModal) {
            mutableSheetState.value = BrowserFeatureSheetState(
                isVisible = true,
                title = "Downloading ${type.name.lowercase().replaceFirstChar(Char::uppercase)}",
                description = "This engine is configured as a feature on demand. Please wait while it becomes available.",
                isDownloading = true
            )
        } else {
            showDownloadingNotification(type)
        }

        if (!remoteConfig.startDownloaded(type)) {
            val moduleName = moduleName(type)
            val request = SplitInstallRequest.newBuilder()
                .addModule(moduleName)
                .build()
            splitInstallManager.startInstall(request)
            splitInstallManager.requestProgressFlow().collectLatest { state ->
                if (state.moduleNames().contains(moduleName) && moduleName in splitInstallManager.installedModules) {
                    return@collectLatest
                }
            }
        } else {
            delay(400)
        }

        registerFeature(type)
        if (showModal) {
            mutableSheetState.value = BrowserFeatureSheetState()
        } else {
            hideDownloadingNotification(type)
        }
    }

    private fun registerFeature(type: EngineType) {
        when (type) {
            EngineType.WEBVIEW -> Unit
            EngineType.GECKO -> runCatching {
                val entry = Class.forName("com.example.webviewgecko.feature.gecko.GeckoFeatureEntry")
                entry.getMethod("register").invoke(null)
            }
        }
    }

    private fun moduleName(type: EngineType): String = when (type) {
        EngineType.WEBVIEW -> "base"
        EngineType.GECKO -> "feature_gecko"
    }

    private fun showDownloadingNotification(type: EngineType) {
        if (!canPostNotifications()) return

        ensureNotificationChannel()
        NotificationManagerCompat.from(context).notify(
            notificationId(type),
            NotificationCompat.Builder(context, DOWNLOAD_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentTitle("Downloading ${engineLabel(type)}")
                .setContentText("Browser feature is being downloaded in the background")
                .setOngoing(true)
                .setProgress(0, 0, true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build()
        )
    }

    private fun hideDownloadingNotification(type: EngineType) {
        NotificationManagerCompat.from(context).cancel(notificationId(type))
    }

    private fun ensureNotificationChannel() {
        val manager = NotificationManagerCompat.from(context)
        val channel = NotificationChannelCompat.Builder(
            DOWNLOAD_CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_LOW
        )
            .setName("Feature downloads")
            .setDescription("Shows browser engine feature download progress")
            .build()
        manager.createNotificationChannel(channel)
    }

    private fun canPostNotifications(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun engineLabel(type: EngineType): String =
        type.name.lowercase().replaceFirstChar(Char::uppercase)

    private fun notificationId(type: EngineType): Int = when (type) {
        EngineType.WEBVIEW -> 1001
        EngineType.GECKO -> 1002
    }

    private companion object {
        const val DOWNLOAD_CHANNEL_ID = "browser_feature_downloads"
    }
}
