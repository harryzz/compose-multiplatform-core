package androidx.compose.ui.platform



// Android-side URL handling would need a separate WIT (Intent.ACTION_VIEW).
// For now log + ignore so links don't crash.
internal actual fun createPlatformUriHandler(): UriHandler =
    object : UriHandler {
        override fun openUri(uri: String) {
            org.jetbrains.skiko.wasi.shell.Logging.Import.log(
                org.jetbrains.skiko.wasi.shell.Logging.Level.WARN, "compose-ui",
                "UriHandler.openUri(\"$uri\") — not implemented on wasmWasi")
        }
    }
