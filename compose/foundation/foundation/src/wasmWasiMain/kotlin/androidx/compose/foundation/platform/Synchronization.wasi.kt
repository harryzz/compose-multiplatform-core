/*
 * Replacement for foundation/skikoMain/platform/Synchronization.skiko.kt —
 * strips `@file:JvmName` (an @OptionalExpectation legal only in common
 * source). wasmWasi is single-threaded so synchronization is a no-op.
 */
package androidx.compose.foundation.platform

@PublishedApi
internal actual class SynchronizedObject

@Suppress("NOTHING_TO_INLINE")
internal actual inline fun makeSynchronizedObject(ref: Any?) = SynchronizedObject()

@Suppress("NOTHING_TO_INLINE", "LESS_VISIBLE_TYPE_ACCESS_IN_INLINE_WARNING")
internal actual inline fun <R> synchronized(lock: SynchronizedObject, block: () -> R): R = block()
