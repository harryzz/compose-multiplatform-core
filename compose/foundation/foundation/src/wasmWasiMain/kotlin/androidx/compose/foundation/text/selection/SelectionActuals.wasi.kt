/*
 * wasmWasi actuals for foundation/text/selection commonMain expects.
 * isCopyKeyEvent uses Ctrl+C (Android-default key chord since our host is
 * Android). Magnifier modifiers are passthroughs (no platform magnifier).
 * Context-menu component appenders are passthroughs (no menu UI yet).
 */
@file:Suppress("UNUSED_PARAMETER")

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.platform.Clipboard
import kotlinx.coroutines.CoroutineScope

internal actual fun isCopyKeyEvent(keyEvent: KeyEvent): Boolean =
    (keyEvent.key == Key.C && keyEvent.isCtrlPressed) || keyEvent.key == Key.Copy

internal actual fun Modifier.selectionMagnifier(manager: SelectionManager): Modifier = this

internal actual fun Modifier.addSelectionContainerTextContextMenuComponents(
    selectionManager: SelectionManager
): Modifier = this

internal actual val SelectionManager.skipCopyKeyEvent: Boolean
    get() = false

internal actual fun TextFieldSelectionManager.isSelectionHandleInVisibleBound(
    isStartHandle: Boolean
): Boolean = isSelectionHandleInVisibleBoundDefault(isStartHandle)

internal actual fun Modifier.textFieldMagnifier(manager: TextFieldSelectionManager): Modifier = this

internal actual fun Modifier.addBasicTextFieldTextContextMenuComponents(
    manager: TextFieldSelectionManager,
    coroutineScope: CoroutineScope,
): Modifier = this

internal actual suspend fun TextFieldSelectionManager.hasAvailableTextToPaste(): Boolean {
    val clip = clipboard ?: return false
    return clip.getClipEntry()?.let {
        // ClipEntry on wasmWasi is text-backed; non-null means text exists.
        true
    } ?: false
}
