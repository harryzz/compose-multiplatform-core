/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 */
// Patched copy of collection/collection/src/nonJvmMain/.../LongSparseArray.nonJvm.kt
// with @JvmField/@JvmSynthetic/@JvmOverloads stripped (Kotlin 2.4 forbids
// @OptionalExpectation use outside commonMain). Original file is excluded via
// kotlin.exclude(...) in the build.gradle.
package androidx.collection

import androidx.collection.internal.EMPTY_LONGS
import androidx.collection.internal.EMPTY_OBJECTS
import androidx.collection.internal.idealLongArraySize

public actual open class LongSparseArray<E>
public actual constructor(initialCapacity: Int) {
    internal actual var garbage: Boolean = false
    internal actual var keys: LongArray
    internal actual var values: Array<Any?>
    internal actual var size: Int = 0

    init {
        if (initialCapacity == 0) {
            keys = EMPTY_LONGS
            values = EMPTY_OBJECTS
        } else {
            val idealCapacity = idealLongArraySize(initialCapacity)
            keys = LongArray(idealCapacity)
            values = arrayOfNulls(idealCapacity)
        }
    }

    public actual open operator fun get(key: Long): E? = commonGet(key)

    @Suppress("KotlinOperator")
    public actual open fun get(key: Long, defaultValue: E): E = commonGet(key, defaultValue)

    @Deprecated("Alias for `remove(key)`.", ReplaceWith("remove(key)"))
    public actual open fun delete(key: Long): Unit = commonRemove(key)

    public actual open fun remove(key: Long): Unit = commonRemove(key)
    public actual open fun remove(key: Long, value: E): Boolean = commonRemove(key, value)
    public actual open fun removeAt(index: Int): Unit = commonRemoveAt(index)

    public actual open fun replace(key: Long, value: E): E? = commonReplace(key, value)
    public actual open fun replace(key: Long, oldValue: E, newValue: E): Boolean =
        commonReplace(key, oldValue, newValue)

    public actual open fun put(key: Long, value: E): Unit = commonPut(key, value)
    public actual open fun putAll(other: LongSparseArray<out E>): Unit = commonPutAll(other)
    public actual open fun putIfAbsent(key: Long, value: E): E? = commonPutIfAbsent(key, value)
    public actual open fun size(): Int = commonSize()
    public actual open fun isEmpty(): Boolean = commonIsEmpty()
    public actual open fun keyAt(index: Int): Long = commonKeyAt(index)
    public actual open fun valueAt(index: Int): E = commonValueAt(index)
    public actual open fun setValueAt(index: Int, value: E): Unit = commonSetValueAt(index, value)
    public actual open fun indexOfKey(key: Long): Int = commonIndexOfKey(key)
    public actual open fun indexOfValue(value: E): Int = commonIndexOfValue(value)
    public actual open fun containsKey(key: Long): Boolean = commonContainsKey(key)
    public actual open fun containsValue(value: E): Boolean = commonContainsValue(value)
    public actual open fun clear(): Unit = commonClear()
    public actual open fun append(key: Long, value: E): Unit = commonAppend(key, value)
    actual override fun toString(): String = commonToString()
}
