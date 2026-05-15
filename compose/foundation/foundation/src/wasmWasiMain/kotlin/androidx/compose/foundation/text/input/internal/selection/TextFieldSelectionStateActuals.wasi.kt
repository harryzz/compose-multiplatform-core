/*
 * wasmWasi actuals for TextFieldSelectionState commonMain expects.
 * Tap/selection gestures use the default impls from commonMain (good enough
 * for a host that delivers normal pointer/key events). Context-menu addition
 * is a passthrough until menu UI lands. ClipboardPasteState polls our
 * wasi:android-clipboard via Clipboard.getClipEntry().
 */
@file:Suppress("UNUSED_PARAMETER")

package androidx.compose.foundation.text.input.internal.selection

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.foundation.text.selection.MouseSelectionObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope

internal actual suspend fun TextFieldSelectionState.detectTextFieldTapGestures(
    pointerInputScope: PointerInputScope,
    interactionSource: MutableInteractionSource?,
    requestFocus: () -> Unit,
    showKeyboard: () -> Unit,
) = defaultDetectTextFieldTapGestures(
    pointerInputScope, interactionSource, requestFocus, showKeyboard,
)

internal actual suspend fun TextFieldSelectionState.textFieldSelectionGestures(
    pointerInputScope: PointerInputScope,
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver,
) = pointerInputScope.defaultTextFieldSelectionGestures(mouseSelectionObserver, textDragObserver)

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    state: TextFieldSelectionState,
    coroutineScope: CoroutineScope,
): Modifier = this

internal actual class ClipboardPasteState actual constructor(private val clipboard: Clipboard) {
    private var _hasClip = false
    private var _hasText = false

    actual val hasText: Boolean get() = _hasText
    actual val hasClip: Boolean get() = _hasClip

    actual suspend fun update() {
        val entry = clipboard.getClipEntry()
        _hasClip = entry != null
        _hasText = entry != null
    }
}
