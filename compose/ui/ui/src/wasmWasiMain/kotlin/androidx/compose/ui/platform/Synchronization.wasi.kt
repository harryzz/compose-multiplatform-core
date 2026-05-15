/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
// Replaces upstream Synchronization.skiko.kt — its inline-warning-as-error
// suppress directive trips Kotlin 2.4. wasmWasi is single-threaded so the
// "lock" is a no-op marker.
package androidx.compose.ui.platform

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

internal actual class SynchronizedObject

internal actual inline fun makeSynchronizedObject(ref: Any?): SynchronizedObject =
    SynchronizedObject()

@OptIn(ExperimentalContracts::class)
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}
