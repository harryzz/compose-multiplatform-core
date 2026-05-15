package androidx.compose.ui.viewinterop

// wasmWasi has no host-side native view hierarchy yet; AndroidView interop
// would require a separate WIT for ANativeWindow / SurfaceView embedding.
// Until then both the public InteropView alias and the internal
// InteropViewGroup alias resolve to Any (matches desktop/jsMain pattern).
actual typealias InteropView = Any

internal actual typealias InteropViewGroup = Any
