package androidx.compose.ui.semantics

import androidx.compose.ui.unit.IntRect

// SemanticsRegion: a11y hit-test region. Upstream uses skia's IRect/Region;
// we don't have either bound to wasmWasi yet. Until accessibility is wired
// through wasi:android-accessibility, this no-op behaves like an empty region.
// It compiles cleanly and is only consulted by the (deferred) a11y tree.
private class WasiSemanticsRegion : SemanticsRegion {
    private var rect: IntRect = IntRect.Zero
    override fun set(rect: IntRect) { this.rect = rect }
    override fun intersect(region: SemanticsRegion): Boolean = false
    override fun difference(rect: IntRect): Boolean = false
    override val bounds: IntRect get() = rect
    override val isEmpty: Boolean get() = rect.width == 0 || rect.height == 0
}

internal actual fun SemanticsRegion(): SemanticsRegion = WasiSemanticsRegion()
