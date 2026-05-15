/*
 * Wasi replacement for the desktopMain `TextFieldCursor.desktop.kt` which
 * defaults `DefaultCursorThickness = 1.dp`. At our typical 3.5x density
 * that's 3-4 raw pixels — visually disappears once the BasicTextField is
 * focused. Bumping to 3.dp gives ~10px, matching what Android's xxhdpi
 * users experience with the Android-default 2.dp at mdpi (also ~10px on
 * the same physical screens).
 */
package androidx.compose.foundation.text

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal actual val DefaultCursorThickness: Dp = 3.dp
