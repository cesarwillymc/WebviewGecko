package com.example.webviewgecko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
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
    val logs by viewModel.bridgeLogs.collectAsState()

    DisposableEffect(Unit) {
        engine.loadUrl("https://robinhood.com/login")
        onDispose { /* ViewModel calls engine.destroy() in onCleared */ }
    }

    val state by engine.state.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            Button(
                onClick = { viewModel.startInjection() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Start Injection")
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
