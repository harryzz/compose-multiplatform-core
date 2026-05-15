/*
 * wasmWasi actuals for upstream skikoMain PlatformFont, currentPlatform,
 * loadTypeface. We don't actually load typefaces here — the host (Rust +
 * skia-safe) handles font lookup at render time via the WIT canvas API.
 * These stubs satisfy the type system so the rest of ui-text compiles.
 */
@file:Suppress("UNUSED_PARAMETER")

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation

actual sealed class PlatformFont actual constructor() : Font {
    actual abstract val identity: String
    actual abstract val variationSettings: FontVariation.Settings
    internal actual val cacheKey: String get() = identity
}

internal actual fun currentPlatform(): Platform = Platform.Linux

internal actual fun loadTypeface(font: Font): org.jetbrains.skia.Typeface =
    org.jetbrains.skia.Typeface()
