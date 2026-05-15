/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package androidx.compose.runtime

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

// wasmWasi has no browser requestAnimationFrame; this default is deprecated
// upstream anyway. Provide a yield-based clock so the type exists. Real wasi
// hosts wire their own MonotonicFrameClock via the coroutine context.
@Deprecated(
    "MonotonicFrameClocks are not globally applicable across platforms. " +
        "Use an appropriate local clock."
)
public actual val DefaultMonotonicFrameClock: MonotonicFrameClock =
    object : MonotonicFrameClock {
        override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R =
            suspendCoroutine { continuation ->
                // No animation frame on wasi — invoke immediately with 0.
                continuation.resume(onFrame(0L))
            }
    }
