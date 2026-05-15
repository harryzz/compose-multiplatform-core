/*
 * wasmWasi actuals for foundation/text commonMain expects:
 *   - timeNowMillis (from monotonic clock)
 *   - isTypedEvent (KeyEvent type discrimination)
 *   - cancelsTextSelection (Android Back-key style cancel)
 *   - showCharacterPalette (macOS palette popup; no-op elsewhere)
 *   - rememberClipboardEventsHandler (DOM clipboard events on web; no-op here)
 *   - 3× ContextMenuArea overloads (we don't have a context-menu UI yet)
 *   - ProvideDefaultPlatformTextContextMenuProviders (no provider stack yet)
 */
@file:Suppress("UNUSED_PARAMETER")

package androidx.compose.foundation.text

import androidx.compose.foundation.text.contextmenu.internal.ProvideDefaultPlatformTextContextMenuProviders
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.text.AnnotatedString
import kotlin.time.TimeSource

@OptIn(kotlin.time.ExperimentalTime::class)
private val markNow = TimeSource.Monotonic.markNow()

@OptIn(kotlin.time.ExperimentalTime::class)
internal actual fun timeNowMillis(): Long = markNow.elapsedNow().inWholeMilliseconds

internal actual val KeyEvent.isTypedEvent: Boolean
    get() {
        val cp = this.utf16CodePoint
        // Filter ASCII control chars. Real platform KeyEvent has Type=KEY_DOWN
        // discriminator; on wasmWasi we just check for a printable scalar.
        return cp != 0 && cp >= 0x20 && cp != 0x7F
    }

internal actual fun KeyEvent.cancelsTextSelection(): Boolean = false

internal actual fun showCharacterPalette() {
    // macOS-only feature; no-op on wasmWasi (matches nativeMain).
}

@Suppress("ComposableNaming")
@Composable
internal actual inline fun rememberClipboardEventsHandler(
    crossinline onPaste: (AnnotatedString) -> Unit,
    crossinline onCopy: () -> AnnotatedString?,
    crossinline onCut: () -> AnnotatedString?,
    isEnabled: Boolean,
): Boolean {
    // wasmWasi has no DOM clipboard events. Routing through wasi:android-clipboard
    // happens via the Clipboard interface methods, not these hooks.
    return false
}

@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit,
) {
    content()
}

@Composable
internal actual fun ContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit,
) {
    content()
}

@Composable
internal actual fun ContextMenuArea(manager: SelectionManager, content: @Composable () -> Unit) {
    content()
}
