/*
 * wasmWasi actuals for foundation commonMain expects under
 * androidx.compose.foundation.internal:
 *   - identityHashCode
 *   - readText / readAnnotatedString / toClipEntry / hasText
 *   - isReadSupported / isWriteSupported
 */
package androidx.compose.foundation.internal

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.text.AnnotatedString

// Module-local identity-hash bookkeeping. Compose-runtime has its own private
// identityHashCode; we can't reach across modules.
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

// ClipEntry on wasmWasi (see compose-ui-wasi/.../Clipboard.wasi.kt) carries
// the text payload directly. AnnotatedString-shaped clipboards just hold the
// plain string.

internal actual suspend fun ClipEntry.readText(): String? = this.getPlainText()

internal actual suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? =
    AnnotatedString(this.getPlainText())

internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? =
    this?.let { ClipEntry.ofPlainText(it.text) }

internal actual fun ClipEntry?.hasText(): Boolean = this?.getPlainText()?.isNotEmpty() == true

internal actual fun Clipboard.isReadSupported(): Boolean = true
internal actual fun Clipboard.isWriteSupported(): Boolean = true
