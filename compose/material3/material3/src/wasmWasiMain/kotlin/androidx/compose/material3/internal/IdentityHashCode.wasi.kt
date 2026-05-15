/*
 * wasmWasi actual for the only uncovered material3 commonMain expect:
 * identityHashCode (internal/System.kt). Same module-local bookkeeping
 * pattern as compose-ui-wasi and compose-foundation-wasi — can't cross
 * module boundaries because compose-runtime's identityHashCode is internal.
 */
package androidx.compose.material3.internal

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
