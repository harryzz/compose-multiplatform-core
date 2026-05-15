package androidx.compose.foundation.gestures

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp

private val WasiScrollConfig = object : ScrollConfig {
    // Match the Android default of 64 dp per scroll-wheel notch.
    override fun Density.calculateMouseWheelScroll(
        event: PointerEvent,
        bounds: IntSize,
    ): Offset {
        val deltaPx = 64.dp.toPx()
        val anyChange = event.changes.firstOrNull()?.scrollDelta ?: Offset.Zero
        return Offset(
            x = anyChange.x * deltaPx,
            // Negate Y: PointerEvent.scrollDelta is "down is positive" but
            // Compose scrolling convention is "down is negative". (Matches
            // every other actual platform's scroll-direction inversion.)
            y = -anyChange.y * deltaPx,
        )
    }
}

internal actual fun CompositionLocalConsumerModifierNode.platformScrollConfig(): ScrollConfig =
    WasiScrollConfig
