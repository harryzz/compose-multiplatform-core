/*
 * Replacement for foundation/desktopMain/text/KeyMapping.desktop.kt — drops
 * the DesktopPlatform dependency (we excluded DesktopPlatform.desktop.kt
 * because it uses System.getProperty / java reflection). wasmWasi runs on
 * Android-the-platform; keep the generic skiko mapping.
 */
package androidx.compose.foundation.text

internal actual val platformDefaultKeyMapping: KeyMapping
    get() = DefaultSkikoKeyMapping
