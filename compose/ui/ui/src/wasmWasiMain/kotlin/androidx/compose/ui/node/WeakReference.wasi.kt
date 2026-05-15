package androidx.compose.ui.node

// wasmWasi has no weak refs — hold strong. Single-threaded model so this is
// safe for compose-ui's use (subtree references, layer pointers).
internal actual class WeakReference<T : Any> actual constructor(referent: T) {
    private var ref: T? = referent
    actual fun get(): T? = ref
    actual fun clear() { ref = null }
}
