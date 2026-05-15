/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 */
package androidx.compose.ui.util

actual inline fun <T> trace(sectionName: String, block: () -> T): T = block()

actual fun traceValue(tag: String, value: Long) { }
