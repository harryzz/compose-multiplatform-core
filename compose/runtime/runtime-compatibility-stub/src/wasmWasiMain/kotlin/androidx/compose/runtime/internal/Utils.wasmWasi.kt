/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package androidx.compose.runtime.internal

// wasmWasi has no WeakMap/JsAny tricks for stable identity hashing.
// Fall back to Any.hashCode() — Kotlin/Wasm's hashCode for class instances
// is per-instance and stable for the object's lifetime.
internal actual fun identityHashCode(instance: Any?): Int =
    instance?.hashCode() ?: 0
