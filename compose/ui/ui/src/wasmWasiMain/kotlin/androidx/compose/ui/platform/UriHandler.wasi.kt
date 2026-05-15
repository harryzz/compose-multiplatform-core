package androidx.compose.ui.platform

import org.jetbrains.skiko.wasi.wit.Canvas as WitCanvas

// Android-side URL handling would need a separate WIT (Intent.ACTION_VIEW).
// For now log + ignore so links don't crash.
internal actual fun createPlatformUriHandler(): UriHandler =
    object : UriHandler {
        override fun openUri(uri: String) {
            WitCanvas.Import.logMessage("UriHandler.openUri(\"$uri\") — not implemented on wasmWasi")
        }
    }
