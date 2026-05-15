package androidx.compose.foundation.text.contextmenu.internal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal actual fun ProvideDefaultPlatformTextContextMenuProviders(
    modifier: Modifier,
    content: @Composable () -> Unit,
) {
    // wasmWasi: no platform-default text context-menu provider yet (mirrors
    // nativeMain). The context-menu UI surface will land alongside material3.
    content()
}
