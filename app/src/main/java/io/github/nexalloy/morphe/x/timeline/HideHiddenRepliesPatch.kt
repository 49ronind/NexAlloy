package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Hides replies that the tweet author has hidden. */
val HideHiddenReplies = patch(
    name = "Hide hidden replies",
    description = "Hides replies that have been hidden by the tweet author.",
) {
    dependsOn(XVersionCheck)

    HideHiddenRepliesFingerprint.hookMethod {
        after { param ->
            val tweet = param.result ?: return@after
            // The last IGET_BOOLEAN result controls hidden-reply visibility.
            // Force it to false (don't show hidden-reply indicator).
            try {
                val boolField = tweet.javaClass.declaredFields
                    .lastOrNull { it.type == Boolean::class.javaPrimitiveType }
                    ?: return@after
                boolField.isAccessible = true
                boolField.setBoolean(tweet, false)
            } catch (_: Exception) {}
        }
    }
}
