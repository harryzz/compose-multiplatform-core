package androidx.compose.ui.draganddrop

import androidx.compose.ui.geometry.Offset

// wasmWasi has no system drag-and-drop; we still provide value-class shells so
// the Modifier.dragAndDropTarget / dragAndDropSource APIs compile. Nothing on
// the platform side will ever construct these in our renderer.

actual class DragAndDropTransferData internal constructor()

actual class DragAndDropEvent internal constructor(
    internal val offset: Offset = Offset.Zero,
)

internal actual val DragAndDropEvent.positionInRoot: Offset
    get() = offset
