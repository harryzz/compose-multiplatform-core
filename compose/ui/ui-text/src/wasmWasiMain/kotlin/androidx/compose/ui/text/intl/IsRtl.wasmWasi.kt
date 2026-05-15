package androidx.compose.ui.text.intl

internal actual fun Locale.isRtl(): Boolean = when (language) {
    "ar", "fa", "he", "iw", "ur", "yi", "ji", "dv", "ps" -> true
    else -> false
}
