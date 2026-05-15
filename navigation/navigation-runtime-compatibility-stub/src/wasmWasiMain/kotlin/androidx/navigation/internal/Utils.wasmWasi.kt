/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
package androidx.navigation.internal

// wasmWasi has no kotlin.native.identityHashCode or JS-style hashCode tricks.
// Fall back to Any.hashCode() — not strictly identity-based but sufficient for
// navigation's use (it just needs a stable-per-instance int).
internal actual fun identityHashCode(instance: Any?): Int = instance.hashCode()
