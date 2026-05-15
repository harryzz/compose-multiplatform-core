/*
 * No-op actual for FontSynthesis.synthesizeTypeface — real synthesis
 * (faux-bold, faux-italic) is host-side via skia-safe. Return the input
 * typeface unchanged; the host already applies synthesis when rendering.
 */
@file:Suppress("UNUSED_PARAMETER")

package androidx.compose.ui.text.font

import androidx.compose.ui.text.platform.Font

internal actual fun FontSynthesis.synthesizeTypeface(
    typeface: Any,
    font: Font,
    requestedWeight: FontWeight,
    requestedStyle: FontStyle,
): Any = typeface
