package com.example.webviewgecko

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.browserengine.core.BrowserConfig
import com.browserengine.core.EngineType
import com.browserengine.core.capabilities.UICapable
import com.browserengine.factory.BrowserEngineFactory
import com.example.webviewgecko.ui.theme.WebviewGeckoTheme

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
fun BrowserScreen() {
    val context = LocalContext.current
    val engine = remember(context) {
        BrowserEngineFactory.create(
            context = context,
            type = EngineType.WEBVIEW,
            config = BrowserConfig(
                javaScriptEnabled = true,
                domStorageEnabled = true,
                supportMultipleWindows = true
            )
        )
    }

    DisposableEffect(Unit) {
        engine.loadUrl("https://example.com")
        onDispose { engine.destroy() }
    }

    val state by engine.state.collectAsState()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (state.isLoading) {
                LinearProgressIndicator(
                    progress = state.progress,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            engine.capability<UICapable>()
                ?.RenderUI(Modifier.fillMaxSize().weight(1f))
                ?: Text("Engine has no UI")
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
