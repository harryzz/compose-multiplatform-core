/*
 * wasmWasi actuals for compose-ui commonMain expects that aren't covered by
 * upstream nonJvmMain or the (DOM-free subset of) webMain bundled in
 * build.gradle.kts.
 */
@file:Suppress("UNUSED_PARAMETER", "unused")

package androidx.compose.ui

internal actual fun areObjectsOfSameType(a: Any, b: Any): Boolean =
    a::class == b::class

/**
 * Cached "current" wall-clock millis, updated each frame by the renderer.
 *
 * Why we don't just call `org.jetbrains.skiko.currentNanoTime()` here:
 * that delegates to `kotlin.time.TimeSource.Monotonic.markNow().elapsedNow()`
 * which on wasmWasi calls into `wasi:clocks/monotonic-clock.now`. That host
 * call permanently pollutes the WIT realloc allocator — even an explicit
 * `freeAllComponentModelReallocAllocatedMemory()` does NOT clear it — so
 * the next WIT import (e.g. any draw call, log, etc.) traps with
 * "Can't create new allocators while realloc-allocated memory is not freed".
 *
 * `RectManager.dispatchCallbacks()` (compose-ui spatial) calls
 * `currentTimeMillis()` every frame on every layout invalidation. With the
 * old `currentNanoTime()` actual, that poisoned the renderer after the
 * first state change → silent trap inside the animation coroutine →
 * Animatable's coroutine dies → state visuals freeze forever.
 *
 * The fix: have the renderer push the frame nanos into this var before
 * each Compose `scene.render(...)`. RectManager only uses the value for
 * debounce timing, so the slight lag (latest frame vs. truly-now) is
 * harmless. See also feedback memories `feedback_currentnanotime_pollutes`
 * and `feedback_wasi_realloc_allocator`.
 */
// wasi is single-threaded so no concurrency annotations are needed.
private var cachedNanoTime: Long = 0L

/** Called by the wasi renderer at the start of every frame. */
public fun updateCachedNanoTime(nanos: Long) {
    cachedNanoTime = nanos
}

/// Read the latest frame nanos that the renderer pushed. Used by
/// `WasiFrameDispatcher` to compute delay deadlines without re-incurring
/// the `currentNanoTime()` realloc-allocator pollution.
public fun cachedNanoTime(): Long = cachedNanoTime

internal actual fun currentTimeMillis(): Long = cachedNanoTime / 1_000_000L
