/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
// Wasm/WASI has no weak-reference primitive in stdlib (no WeakRef, no
// kotlin.native.ref.WeakReference). Fall back to a strong reference. This
// can hold objects past their normal lifecycle, but the alternative is no
// build at all. Compose's wasmJs ecosystem uses the same fallback pattern.
package androidx.lifecycle

internal actual class WeakReference<T : Any> actual constructor(reference: T) {
    private val delegate: T = reference

    actual fun get(): T? = delegate
}
