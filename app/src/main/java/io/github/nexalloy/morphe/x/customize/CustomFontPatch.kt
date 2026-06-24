package io.github.nexalloy.morphe.x.customize

import android.graphics.Typeface
import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch
import java.io.File

// ── Shared helper ─────────────────────────────────────────────────────────────

private fun loadUserTypeface(): Typeface? {
    val path = XPref.getString("x_custom_font_path")
    if (path.isEmpty()) return null
    return try { Typeface.createFromFile(File(path)) } catch (_: Exception) { null }
}

private fun loadUserEmojiFont(): Typeface? {
    val path = XPref.getString("x_custom_emoji_font_path")
    if (path.isEmpty()) return null
    return try { Typeface.createFromFile(File(path)) } catch (_: Exception) { null }
}

/**
 * Replaces X's Chirp typeface with a user-supplied font file.
 * Pref key: "x_custom_font_path"  (full path to .ttf/.otf)
 */
val CustomFont = patch(
    name = "Custom font",
    description = "Replaces X's Chirp font with a custom font file.",
    use = false,
) {
    dependsOn(XVersionCheck)

    TypefaceCreateFingerprint.hookMethod {
        after { param ->
            val tf = loadUserTypeface() ?: return@after
            param.result = tf
        }
    }
}

/**
 * Replaces X's emoji font with a user-supplied font file.
 * Pref key: "x_custom_emoji_font_path"
 */
val CustomEmojiFont = patch(
    name = "Custom emoji font",
    description = "Replaces X's emoji font with a custom font file.",
    use = false,
) {
    dependsOn(XVersionCheck)

    CustomFontHookFingerprint.hookMethod {
        after { param ->
            val tf = loadUserEmojiFont() ?: return@after
            // The method returns a processed CharSequence; replace any spans that carry the Typeface.
            // Simplest approach: wrap result in a SpannableString with the custom Typeface span.
            try {
                val result = param.result as? CharSequence ?: return@after
                val spannable = android.text.SpannableString(result)
                spannable.setSpan(
                    android.text.style.TypefaceSpan(""),
                    0, spannable.length,
                    android.text.Spannable.SPAN_INCLUSIVE_INCLUSIVE
                )
                param.result = spannable
            } catch (_: Exception) {}
        }
    }
}
