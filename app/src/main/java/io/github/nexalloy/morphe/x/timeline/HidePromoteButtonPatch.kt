package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Hides the "Promote" button shown under your own posts.
 * The JsonTweetQuickPromoteEligibility field on JsonApiTweet is nulled.
 */
val HidePromoteButton = patch(
    name = "Hide promote button",
    description = "Hides the Promote button shown under self posts.",
) {
    dependsOn(XVersionCheck, TweetInfoHook)

    XTweetInfoProcessors.register { tweet ->
        val cls = tweet.javaClass
        try {
            val field = cls.declaredFields
                .firstOrNull { it.type.name.contains("JsonTweetQuickPromoteEligibility") }
                ?: return@register
            field.isAccessible = true
            field.set(tweet, null)
        } catch (_: Exception) {}
    }
}
