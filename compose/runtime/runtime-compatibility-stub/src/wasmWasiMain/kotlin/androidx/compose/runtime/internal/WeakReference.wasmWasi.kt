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

// wasmWasi has no WeakRef primitive — fall back to a strong reference.
internal actual class WeakReference<T : Any> actual constructor(reference: T) {
    private val delegate: T = reference

    actual fun get(): T? = delegate
}
