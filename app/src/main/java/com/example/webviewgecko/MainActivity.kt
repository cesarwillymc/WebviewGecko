package com.example.webviewgecko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import com.browserengine.core.BrowserState
import com.browserengine.core.capabilities.UICapable
import com.example.webviewgecko.ui.theme.WebviewGeckoTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WebviewGeckoTheme {
                BrowserScreen()
            }
        }
    }
}

@Composable
fun BrowserScreen(
    viewModel: BrowserViewModel = hiltViewModel()
) {
    val engine by viewModel.engine.collectAsState()
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.completePermissionGrant(allGranted)
    }

    val pendingPermission by viewModel.pendingPermissionRequest.collectAsState()
    val featureSheetState by viewModel.featureSheetState.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val fallbackState = remember { MutableStateFlow(BrowserState()) }
    val state by (engine?.state ?: fallbackState).collectAsState()

    DisposableEffect(engine) {
        engine?.loadUrl(Script.robinhoodULR)
        onDispose { }
    }


    LaunchedEffect(pendingPermission) {
        if (pendingPermission != null) {
            val req = pendingPermission ?: return@LaunchedEffect
            val androidPerms = permissionStringsToAndroidManifest(req.permissions)
            val toRequest = androidPerms.filter {
                activity != null && ContextCompat.checkSelfPermission(context, it) != android.content.pm.PackageManager.PERMISSION_GRANTED
            }
            if (toRequest.isNotEmpty()) {
                permissionLauncher.launch(toRequest.toTypedArray())
            } else {
                viewModel.grantPermission()
            }
        }
    }


    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Button(
                onClick = { viewModel.startInjection() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Injection")
            }
            Button(
                onClick = { viewModel.sendExampleMessage() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Send Example")
            }
            if (state.isLoading) {
                LinearProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            engine?.capability(UICapable::class)
                ?.RenderUI(Modifier.fillMaxSize().weight(1f))
                ?: EngineLoadingState()

        }
    }

    if (featureSheetState.isVisible) {
        BrowserFeatureDownloadSheet(featureSheetState)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrowserFeatureDownloadSheet(
    state: BrowserFeatureSheetState
) {
    ModalBottomSheet(
        onDismissRequest = { }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = state.title)
            Text(
                text = state.description,
                modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
            )
            if (state.isDownloading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun EngineLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Preparing browser engine")
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WebviewGeckoTheme {
        Text(text = "Browser Engine Demo")
    }
}
