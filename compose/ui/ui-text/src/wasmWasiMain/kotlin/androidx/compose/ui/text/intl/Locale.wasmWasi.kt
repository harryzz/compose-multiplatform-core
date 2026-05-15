/*
 * wasmWasi actuals for upstream Compose ui-text intl. Reads from host WIT
 * wasi:android-locale for the user's current preference (bg-BG on the dev
 * device, en-US fallback).
 */
@file:Suppress("UNUSED_PARAMETER")

package androidx.compose.ui.text.intl

import org.jetbrains.skiko.wasi.wit.Locale as WitLocale

actual class Locale {
    private val tag: String
    private val parts: List<String>

    actual constructor(languageTag: String) {
        this.tag = if (languageTag.isEmpty()) currentLanguageTag() else languageTag
        this.parts = tag.split('-', '_')
    }

    actual val language: String
        get() = parts.getOrNull(0)?.lowercase() ?: ""
    actual val script: String
        get() = parts.firstOrNull { it.length == 4 && it[0].isUpperCase() } ?: ""
    actual val region: String
        get() = parts.getOrNull(1)?.takeIf { it.length == 2 }?.uppercase()
            ?: parts.getOrNull(2)?.takeIf { it.length == 2 }?.uppercase()
            ?: ""

    actual fun toLanguageTag(): String = tag

    actual override fun equals(other: Any?): Boolean = other is Locale && other.tag == tag
    actual override fun hashCode(): Int = tag.hashCode()

    actual companion object {
        actual val current: Locale get() = Locale(currentLanguageTag())
    }
}

private fun currentLanguageTag(): String =
    WitLocale.Import.primaryLocale().ifEmpty { "en-US" }

internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate = WasiPlatformLocaleDelegate

private object WasiPlatformLocaleDelegate : PlatformLocaleDelegate {
    override val current: LocaleList
        get() = LocaleList(listOf(Locale(currentLanguageTag())))
}
