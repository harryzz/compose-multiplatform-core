/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
// Patched copy of collection/collection/src/nonJvmMain/.../SparseArrayCompat.nonJvm.kt
// with @JvmField/@JvmSynthetic/@JvmOverloads stripped. Kotlin 2.4 forbids
// @OptionalExpectation annotation use outside commonMain, and these annotations
// are no-ops on wasm anyway. Original file is excluded via kotlin.exclude(...)
// in the build.gradle.
package androidx.collection

import androidx.collection.internal.EMPTY_INTS
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.idealIntArraySize

public actual open class SparseArrayCompat<E>
public actual constructor(initialCapacity: Int) {
    internal actual var garbage: Boolean = false
    internal actual var keys: IntArray
    internal actual var values: Array<Any?>
    internal actual var size: Int = 0

    init {
        if (initialCapacity == 0) {
            keys = EMPTY_INTS
            values = EMPTY_OBJECTS
        } else {
            val capacity = idealIntArraySize(initialCapacity)
            keys = IntArray(capacity)
            values = arrayOfNulls(capacity)
        }
    }

    public actual open operator fun get(key: Int): E? = commonGet(key)
    public actual open fun get(key: Int, defaultValue: E): E = commonGet(key, defaultValue)
    public actual open fun remove(key: Int): Unit = commonRemove(key)
    public actual open fun remove(key: Int, value: Any?): Boolean = commonRemove(key, value)
    public actual open fun removeAt(index: Int): Unit = commonRemoveAt(index)
    public actual open fun removeAtRange(index: Int, size: Int): Unit =
        commonRemoveAtRange(index, size)

    public actual open fun replace(key: Int, value: E): E? = commonReplace(key, value)
    public actual open fun replace(key: Int, oldValue: E, newValue: E): Boolean =
        commonReplace(key, oldValue, newValue)

    public actual open fun put(key: Int, value: E): Unit = commonPut(key, value)
    public actual open fun putAll(other: SparseArrayCompat<out E>): Unit = commonPutAll(other)
    public actual open fun putIfAbsent(key: Int, value: E): E? = commonPutIfAbsent(key, value)
    public actual open fun size(): Int = commonSize()
    public actual open fun isEmpty(): Boolean = commonIsEmpty()
    public actual open fun keyAt(index: Int): Int = commonKeyAt(index)
    public actual open fun valueAt(index: Int): E = commonValueAt(index)
    public actual open fun setValueAt(index: Int, value: E): Unit = commonSetValueAt(index, value)
    public actual open fun indexOfKey(key: Int): Int = commonIndexOfKey(key)
    public actual open fun indexOfValue(value: E): Int = commonIndexOfValue(value)
    public actual open fun containsKey(key: Int): Boolean = commonContainsKey(key)
    public actual open fun containsValue(value: E): Boolean = commonContainsValue(value)
    public actual open fun clear(): Unit = commonClear()
    public actual open fun append(key: Int, value: E): Unit = commonAppend(key, value)
    public actual override fun toString(): String = commonToString()
}
