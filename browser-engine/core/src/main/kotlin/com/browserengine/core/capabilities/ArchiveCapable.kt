package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability
import java.io.File

interface ArchiveCapable : BrowserCapability {
    /**
     * Save page to a portable format.
     * WebView: saveWebArchive() → .mhtml
     * Gecko: saveAsPdf() → .pdf
     */
    suspend fun savePage(destination: File, format: ArchiveFormat): Result<Unit>

    val supportedFormats: List<ArchiveFormat>
}

enum class ArchiveFormat {
    MHTML,
    PDF,
    HTML
}
