package androidx.compose.ui.platform

import androidx.compose.ui.text.AnnotatedString
import org.jetbrains.skiko.wasi.shell.Clipboard as WitClipboard

// ClipEntry stores plain text on wasmWasi (our wasi:android-clipboard WIT only
// exposes text). clipMetadata isn't synthesizable because skikoMain marks
// ClipMetadata's constructor `private`; we mirror the iOS/macOS strategy of
// throwing if anyone reads it.
actual class ClipEntry internal constructor(internal val text: String) {
    @Deprecated(
        "ClipMetadata is not implemented on wasmWasi; use Clipboard.nativeClipboard instead.",
    )
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented on wasmWasi")

    /**
     * wasmWasi-only public accessors so compose-foundation-wasi can route its
     * `ClipEntry.readText()` / `toClipEntry()` / `hasText()` actuals through.
     * Mirrors the iOS `getPlainText()` escape hatch.
     */
    fun getPlainText(): String = text
    companion object {
        fun ofPlainText(text: String): ClipEntry = ClipEntry(text)
    }
}

// Desktop/iOS use a typealias; do the same. Compose only treats this as an
// opaque escape hatch (Clipboard.nativeClipboard).
actual typealias NativeClipboard = Any

internal actual fun createPlatformClipboardManager(): ClipboardManager =
    object : ClipboardManager {
        override fun setText(annotatedString: AnnotatedString) {
            WitClipboard.Import.setText(annotatedString.text)
        }
        override fun getText(): AnnotatedString? =
            if (WitClipboard.Import.hasText()) AnnotatedString(WitClipboard.Import.getText())
            else null
    }

private object WasiNativeClipboard

internal actual fun createPlatformClipboard(): Clipboard =
    object : Clipboard {
        override suspend fun getClipEntry(): ClipEntry? =
            if (WitClipboard.Import.hasText()) ClipEntry(WitClipboard.Import.getText())
            else null
        override suspend fun setClipEntry(clipEntry: ClipEntry?) {
            if (clipEntry == null) WitClipboard.Import.clear()
            else WitClipboard.Import.setText(clipEntry.text)
        }
        override val nativeClipboard: NativeClipboard get() = WasiNativeClipboard
    }
