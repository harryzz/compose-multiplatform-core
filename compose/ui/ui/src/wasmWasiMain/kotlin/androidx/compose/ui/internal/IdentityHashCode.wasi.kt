package androidx.compose.ui.internal

// Standalone identity-hash bookkeeping for compose-ui (can't reach
// compose-runtime's internal identityHashCode across modules). Single-threaded
// wasmWasi → no synchronization needed.
private var nextHash = 1
private val identityMap = HashMap<Identity, Int>()

private class Identity(val target: Any) {
    override fun equals(other: Any?): Boolean = other is Identity && other.target === target
    override fun hashCode(): Int = nextHash
}

internal actual fun identityHashCode(instance: Any?): Int {
    if (instance == null) return 0
    val key = Identity(instance)
    val existing = identityMap[key]
    if (existing != null) return existing
    val v = nextHash++
    identityMap[key] = v
    return v
}
