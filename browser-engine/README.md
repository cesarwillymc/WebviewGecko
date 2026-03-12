# 🌐 BrowserEngine — Unified Android Browser Abstraction Layer

> A unified, extensible, engine-agnostic browser abstraction for Android.  
> Supports **WebView (Chromium/Blink)** and **GeckoView (Firefox/Gecko)** under one interface.  
> Built with **Jetpack Compose**, **SOLID principles**, **Decorator pattern**, and **zero engine leakage**.

---

## 📋 Table of Contents

1. [Architecture Philosophy](#architecture-philosophy)
2. [Design Patterns Used](#design-patterns-used)
3. [Module Structure](#module-structure)
4. [Core Interfaces](#core-interfaces)
5. [Capability Interfaces](#capability-interfaces)
6. [Implementations Map](#implementations-map)
7. [Roadmap](#roadmap)
8. [Usage Examples](#usage-examples)
9. [Engine Capabilities Reference](#engine-capabilities-reference)

---

## Architecture Philosophy

### The Problem
Both `WebView` and `GeckoView` can do the same things, but through completely different APIs:

| Feature | WebView | GeckoView |
|---|---|---|
| JS Injection | `evaluateJavascript()` | `evaluateJavascript()` + WebExtension |
| Permissions | `onPermissionRequest` | `PermissionDelegate` (3-layer) |
| Screenshot | `draw(canvas)` | `capturePixels()` |
| Save Archive | `saveWebArchive()` (MHTML) | `saveAsPdf()` |
| Cookies | `CookieManager` | `GeckoRuntime` storage |
| Clear Data | `clearCache()` / `clearHistory()` | `GeckoRuntime.clearData()` |
| UI Embedding | `AndroidView{}` (Compose) | `AndroidView{}` (Compose) |

### The Solution
A **layered abstraction** where:
- ✅ The outside world only knows about **interfaces**
- ✅ Engine-specific code lives **only** inside its implementation
- ✅ New capabilities are added via **Decorator pattern** — never modifying the core
- ✅ UI is returned as a `@Composable` — the caller just calls `engine.renderUI()`
- ✅ Capabilities are opt-in — a `GeckoEngine` can expose `PdfCapable`, a `WebViewEngine` exposes `ArchiveCapable`

---

## Design Patterns Used

```
┌────────────────────────────────────────────────────────────┐
│                    PATTERN MAP                             │
├────────────────────────────────────────────────────────────┤
│ Interface Segregation  → BrowserEngine split into small   │
│                          focused capability interfaces     │
├────────────────────────────────────────────────────────────┤
│ Decorator Pattern      → PermissionCapable wraps engine   │
│                          and adds permission behavior      │
├────────────────────────────────────────────────────────────┤
│ Proxy Pattern          → BrowserEngineProxy centralizes   │
│                          capability resolution             │
├────────────────────────────────────────────────────────────┤
│ Factory Pattern        → BrowserEngineFactory creates the │
│                          right engine + decorators         │
├────────────────────────────────────────────────────────────┤
│ Strategy Pattern       → Interchangeable engines behind   │
│                          same interface                    │
├────────────────────────────────────────────────────────────┤
│ Composite Pattern      → CapabilityRegistry holds all     │
│                          engine capabilities together      │
└────────────────────────────────────────────────────────────┘
```

---

## Module Structure

```
browser-engine/
├── core/                          ← Pure interfaces, zero dependencies
│   ├── BrowserEngine.kt
│   ├── BrowserState.kt
│   ├── BrowserEvent.kt
│   └── capabilities/
│       ├── JsCapable.kt
│       ├── PermissionCapable.kt
│       ├── CookieCapable.kt
│       ├── StorageCapable.kt
│       ├── ScreenshotCapable.kt
│       ├── ArchiveCapable.kt
│       ├── NavigationCapable.kt
│       ├── NetworkCapable.kt
│       ├── UICapable.kt
│       ├── MediaCapable.kt
│       ├── PopupCapable.kt           ← NEW
│       └── NavigationInterceptCapable.kt  ← NEW
│
├── webview/                       ← Android WebView implementation
│   ├── WebViewEngine.kt
│   ├── WebViewJsCapable.kt
│   ├── WebViewPermissionCapable.kt
│   ├── WebViewCookieCapable.kt
│   ├── WebViewScreenshotCapable.kt
│   ├── WebViewArchiveCapable.kt
│   ├── WebViewPopupCapable.kt         ← NEW
│   ├── WebViewNavigationInterceptCapable.kt  ← NEW
│   └── compose/
│       └── WebViewComposable.kt
│
├── gecko/                         ← GeckoView implementation
│   ├── GeckoEngine.kt
│   ├── GeckoJsCapable.kt
│   ├── GeckoPermissionCapable.kt
│   ├── GeckoCookieCapable.kt
│   ├── GeckoScreenshotCapable.kt
│   ├── GeckoPdfCapable.kt
│   ├── GeckoPopupCapable.kt          ← NEW
│   ├── GeckoNavigationInterceptCapable.kt  ← NEW
│   └── compose/
│       └── GeckoComposable.kt
│
├── decorators/                    ← Capability decorators (engine-agnostic)
│   ├── LoggingBrowserDecorator.kt
│   ├── AnalyticsBrowserDecorator.kt
│   └── SecurityBrowserDecorator.kt
│
├── factory/
│   └── BrowserEngineFactory.kt
│
└── sample/                        ← Example app
    └── MainActivity.kt
```

---

## Implementations Map

### WebView Engine
```
WebViewEngine implements:
  ✅ BrowserEngine     → WebView + WebViewClient + WebChromeClient
  ✅ UICapable         → AndroidView { WebView(...) }
  ✅ JsCapable         → evaluateJavascript() + addJavascriptInterface()
  ✅ PermissionCapable → WebChromeClient.onPermissionRequest()
  ✅ CookieCapable     → CookieManager.getInstance()
  ✅ StorageCapable    → WebSettings + WebStorage
  ✅ ScreenshotCapable → draw(Canvas) on a Bitmap
  ✅ ArchiveCapable    → saveWebArchive() → .mhtml + saveAsPdf()
  ✅ NavigationCapable → webView.goBack() / goForward() / copyBackForwardList()
  ✅ NetworkCapable    → WebSettings + shouldInterceptRequest()
  ✅ MediaCapable      → WebChromeClient.onShowFileChooser()
  ✅ PopupCapable      → WebChromeClient.onCreateWindow() / onCloseWindow() + WebViewTransport
  ✅ NavigationInterceptCapable → WebViewClient.shouldOverrideUrlLoading()
```

### GeckoView Engine
```
GeckoEngine implements:
  ✅ BrowserEngine     → GeckoSession + GeckoRuntime
  ✅ UICapable         → AndroidView { GeckoView(...) }
  ✅ JsCapable         → evaluateJavascript() + WebExtension + MessageDelegate
  ✅ PermissionCapable → PermissionDelegate (3-layer: Android + Content + Media)
  ✅ CookieCapable     → GeckoRuntime.storageController
  ✅ StorageCapable    → GeckoRuntime.clearData()
  ✅ ScreenshotCapable → GeckoDisplay.capturePixels()
  ✅ ArchiveCapable    → saveAsPdf() → .pdf (no MHTML native)
  ✅ NavigationCapable → GeckoSession.goBack() / goForward()
  ✅ NetworkCapable    → GeckoSession.ContentDelegate + GeckoRuntimeSettings
  ✅ MediaCapable      → PermissionDelegate.onMediaPermissionRequest()
  ✅ PopupCapable      → ContentDelegate.onOpenWindow() / onCloseWindow() + new GeckoSession
  ✅ NavigationInterceptCapable → NavigationDelegate.onLoadRequest() returning LOAD_REQUEST_HANDLED
```

---

## Roadmap

```
PHASE 1 — Core Interfaces (Week 1-2)
══════════════════════════════════════
 ☑ Define BrowserEngine interface
 ☑ Define all capability interfaces (UICapable, JsCapable, etc.)
 ☑ Define PopupCapable, NavigationInterceptCapable
 ☑ Define BrowserState, BrowserEvent, BrowserError models
 ☐ Write unit tests for all interface contracts

PHASE 2 — WebView Implementation (Week 3-4)
════════════════════════════════════════════
 ☑ WebViewEngine core (load, reload, stop, destroy)
 ☑ WebViewUICapable → AndroidView Composable
 ☑ WebViewJsCapable → evaluateJavascript + addJavascriptInterface
 ☑ WebViewPermissionCapable → WebChromeClient.onPermissionRequest
 ☑ WebViewCookieCapable → CookieManager
 ☑ WebViewStorageCapable → WebSettings + WebStorage
 ☑ WebViewScreenshotCapable → Canvas draw
 ☑ WebViewArchiveCapable → saveWebArchive (MHTML)
 ☑ WebViewNavigationCapable → goBack/Forward + history
 ☑ WebViewNetworkCapable → shouldInterceptRequest + headers
 ☑ WebViewPopupCapable → onCreateWindow + WebViewTransport
 ☑ WebViewNavigationInterceptCapable → shouldOverrideUrlLoading
 ☐ Integration tests

PHASE 3 — GeckoView Implementation (Week 5-7)
══════════════════════════════════════════════
 ☐ GeckoEngine core (session + runtime setup)
 ☐ GeckoUICapable → AndroidView Composable
 ☐ GeckoJsCapable → evaluateJavascript + WebExtension bridge
 ☐ GeckoPermissionCapable → PermissionDelegate 3-layer
 ☐ GeckoCookieCapable → storageController
 ☐ GeckoStorageCapable → clearData
 ☐ GeckoScreenshotCapable → capturePixels
 ☐ GeckoPdfCapable → saveAsPdf
 ☐ GeckoNavigationCapable → session navigation
 ☐ GeckoNetworkCapable → ContentDelegate + RuntimeSettings
 ☐ GeckoPopupCapable → ContentDelegate.onOpenWindow + new session
 ☐ GeckoNavigationInterceptCapable → NavigationDelegate.onLoadRequest
 ☐ Integration tests

PHASE 4 — Decorators (Week 8)
══════════════════════════════
 ☐ LoggingBrowserDecorator → logs all events and calls
 ☐ AnalyticsBrowserDecorator → emits analytics events
 ☐ SecurityBrowserDecorator → enforces URL whitelist/blacklist
 ☐ CachingBrowserDecorator → smart cache strategies

PHASE 5 — Factory + DI (Week 9)
════════════════════════════════
 ☐ BrowserEngineFactory → creates correct engine by config
 ☐ Hilt/Koin module for DI
 ☐ Engine switching at runtime

PHASE 6 — UI Components (Week 10)
═════════════════════════════════
 ☐ BrowserToolbar composable (address bar, progress, buttons)
 ☐ BrowserPermissionDialog composable
 ☐ BrowserMediaPicker composable (camera source selector)
 ☐ BrowserDownloadDialog composable
 ☐ BrowserErrorScreen composable

PHASE 7 — Advanced Features (Week 11-12)
═════════════════════════════════════════
 ☐ SingleFile JS injection for page archiving
 ☐ Dark mode injection
 ☐ Ad-block via request interceptor
 ☐ Custom font scaling injection
 ☐ Reading mode JS injection
 ☐ Form autofill bridge
```

---

## PopupCapable — Usage

```kotlin
@Composable
fun BrowserHost(engine: BrowserEngine, factory: BrowserEngineFactory) {
    var popupEngine by remember { mutableStateOf<BrowserEngine?>(null) }

    LaunchedEffect(engine) {
        engine.capability<PopupCapable>()?.apply {
            setPopupRequestHandler { opener, uri, isUserGesture, onAllow, onBlock ->
                if (isUserGesture || allowBackgroundPopups) {
                    val newEngine = factory.create(EngineType.WEBVIEW, ...)
                    onAllow(newEngine)
                    popupEngine = newEngine
                } else {
                    onBlock()
                }
            }
            setPopupCloseHandler { engine ->
                popupEngine = null
                engine.destroy()
            }
        }
    }

    // Render popup as an overlay, bottom sheet, new tab, etc.
    popupEngine?.capability<UICapable>()?.RenderUI(
        modifier = Modifier.fillMaxSize()
    )
}
```

---

## NavigationInterceptCapable — Usage

```kotlin
fun setupNavigationIntercept(engine: BrowserEngine, context: Context) {
    engine.capability<NavigationInterceptCapable>()
        ?.setNavigationInterceptor { request ->
            val uri = Uri.parse(request.url)
            when {
                // Force HTTPS
                uri.scheme == "http" ->
                    NavigationResult.Redirect(request.url.replace("http://", "https://"))

                // Hand off deep links to the OS
                uri.scheme !in listOf("http", "https") -> {
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                    }
                    NavigationResult.ConsumedByApp
                }

                // Block a known ad/tracker domain
                uri.host?.endsWith("doubleclick.net") == true ->
                    NavigationResult.Block

                // Let everything else load normally
                else -> NavigationResult.Allow
            }
        }
}
```

---

## Key Design Rules

```
✅ DO:
  - Add features via new Capability interfaces
  - Let each engine implement only what it can support
  - Return Composables from UICapable — never raw Views
  - Use StateFlow for state, Flow for events
  - Use Result<T> for all async operations that can fail
  - Use capability<T>() for feature discovery
  - Write tests against interfaces only — never implementations

❌ DON'T:
  - Import WebView or GeckoView outside their modules
  - Expose WebViewClient, GeckoSession, etc. in any interface
  - Use callbacks where coroutines/Flow work
  - Add engine-specific checks in calling code (no "if gecko...")
  - Cast BrowserEngine to a concrete type in business logic
```
