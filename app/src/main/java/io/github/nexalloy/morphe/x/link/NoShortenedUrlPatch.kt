package io.github.nexalloy.morphe.x.link

import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch
import io.github.nexalloy.setObjectField

/**
 * Replaces t.co shortened URLs with the original expanded URL in timeline posts.
 * Mirrors piko's NoShortenedUrlPatch: hooks JsonUrlEntity.parse() and overwrites
 * the display/expanded url fields.
 */
val NoShortenedUrl = patch(
    name = "No shortened URL",
    description = "Replaces t.co links with the original full URL.",
) {
    dependsOn(XVersionCheck)

    JsonUrlEntityParseFingerprint.hookMethod {
        after { param ->
            val entity = param.result ?: return@after
            try {
                // JsonUrlEntity uses obfuscated field names; common mapping:
                // field 'a' = display_url, field 'b' = expanded_url
                val expanded = entity.getObjectFieldOrNull("b") as? String ?: return@after
                if (expanded.isNotEmpty()) {
                    entity.setObjectField("a", expanded)  // replace display_url with expanded_url
                }
            } catch (_: Exception) {}
        }
    }
}
