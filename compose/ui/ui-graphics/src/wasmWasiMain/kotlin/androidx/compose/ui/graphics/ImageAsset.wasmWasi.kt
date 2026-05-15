/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package androidx.compose.ui.graphics

// Lone wasmWasi actual for `ByteArray.putBytesInto`. Used by upstream
// SkiaImageAsset.skiko.kt to copy decoded BGRA8888 pixels into an IntArray
// pixel buffer. Same wire layout as webMain's impl.
internal actual fun ByteArray.putBytesInto(array: IntArray, offset: Int, length: Int) {
    var b = 0
    var i = 0
    while (i < length) {
        val a0 = this[b].toInt() and 0xFF
        val a1 = this[b + 1].toInt() and 0xFF
        val a2 = this[b + 2].toInt() and 0xFF
        val a3 = this[b + 3].toInt() and 0xFF
        // BGRA → ARGB
        array[offset + i] = (a3 shl 24) or (a2 shl 16) or (a1 shl 8) or a0
        b += 4
        i++
    }
}
