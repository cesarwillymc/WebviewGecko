package com.browserengine.core.capabilities

import android.graphics.Bitmap
import com.browserengine.core.BrowserCapability
import java.io.File

interface ScreenshotCapable : BrowserCapability {
    /**
     * Capture visible viewport as Bitmap.
     * WebView: draw(canvas) on software-rendered view
     * Gecko: GeckoDisplay.capturePixels()
     */
    suspend fun captureScreenshot(): Result<Bitmap>

    /**
     * Capture screenshot and save to file.
     */
    suspend fun saveScreenshotTo(file: File, format: ImageFormat = ImageFormat.PNG): Result<Unit>
}

enum class ImageFormat { PNG, JPEG, WEBP }
