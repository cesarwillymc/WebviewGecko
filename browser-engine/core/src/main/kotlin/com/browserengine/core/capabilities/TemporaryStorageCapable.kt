package com.browserengine.core.capabilities

import com.browserengine.core.BrowserCapability
import java.io.File

interface TemporaryStorageCapable : BrowserCapability {
    fun temporaryDirectory(): File
}
