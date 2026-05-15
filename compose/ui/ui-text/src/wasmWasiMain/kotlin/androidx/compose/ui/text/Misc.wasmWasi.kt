/*
 * Misc wasmWasi actuals for upstream Compose ui-text expects: WeakKeysCache,
 * ActualStringDelegate, synthesizeTypeface. None require host plumbing —
 * single-threaded process means a plain HashMap suffices for the cache, and
 * string-delegate / typeface-synthesis can no-op gracefully.
 */
@file:Suppress("UNUSED_PARAMETER")

package androidx.compose.ui.text

internal actual class WeakKeysCache<K : Any, V : Any> actual constructor() {
    private val map = HashMap<K, V>()
    actual inline fun getOrPut(key: K, loader: (K) -> V): V {
        // Cannot inline access to non-public map; do a manual put-or-get.
        @Suppress("UNCHECKED_CAST")
        val existing = (this as WeakKeysCache<*, *>).peek(key)
        if (existing != null) return existing as V
        val fresh = loader(key)
        (this as WeakKeysCache<K, V>).put(key, fresh)
        return fresh
    }
    @PublishedApi
    internal fun peek(key: Any): Any? = (map as Map<*, *>)[key]
    @PublishedApi
    internal fun put(key: K, value: V) { map[key] = value }
}
