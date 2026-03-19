package com.example.webviewgecko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
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
    val engine = viewModel.engine
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.values.all { it }
        viewModel.completePermissionGrant(allGranted)
    }

    val logs by viewModel.bridgeLogs.collectAsState()
    val pendingPermission by viewModel.pendingPermissionRequest.collectAsState()
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val state by engine.state.collectAsState()

    DisposableEffect(Unit) {
        engine.loadUrl(Script.robinhoodULR)
        onDispose { /* ViewModel calls engine.destroy() in onCleared */ }
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
                    progress = state.progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            engine.capability(UICapable::class)
                ?.RenderUI(Modifier.fillMaxSize().weight(1f))
                ?: Text(text = "Engine has no UI")

        }
    }
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    WebviewGeckoTheme {
        Text(text = "Browser Engine Demo")
    }
}
