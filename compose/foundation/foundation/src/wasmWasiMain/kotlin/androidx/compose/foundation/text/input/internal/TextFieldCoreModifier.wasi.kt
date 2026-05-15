/*
 * Diagnostic wasi actual for drawCursor / drawSelectionHighlight. Logs the
 * selection value the draw layer is currently observing so we can compare
 * against the snapshotFlow-observed value (which advances correctly per the
 * LaunchedEffect log in TextFieldCard). If draw sees a stale selection, the
 * draw-phase snapshot observer is failing to track transitive reads through
 * TransformedTextFieldState.visualText → TextFieldState.value.
 */
package androidx.compose.foundation.text.input.internal

import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

internal actual fun TextFieldCoreModifierNode.drawSelectionHighlight(
    scope: DrawScope,
    selection: TextRange,
    textLayoutResult: TextLayoutResult,
) = drawDefaultSelectionHighlight(scope, selection, textLayoutResult)

internal actual fun TextFieldCoreModifierNode.drawCursor(
    scope: DrawScope,
    brush: Brush,
    showCursor: Boolean,
    cursorAnimation: CursorAnimationState?,
    textFieldSelectionState: TextFieldSelectionState,
) = drawDefaultCursor(scope, brush, showCursor, cursorAnimation, textFieldSelectionState)
