package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Hides Community Notes (birdwatch notes) from tweets.
 * The first boolean field in JsonApiTweet controls note visibility.
 */
val HideCommunityNotes = patch(
    name = "Hide Community Notes",
    description = "Hides Community Notes (Birdwatch) from posts.",
) {
    dependsOn(XVersionCheck, TweetInfoHook)

    XTweetInfoProcessors.register { tweet ->
        val cls = tweet.javaClass
        val boolFields = cls.declaredFields.filter { it.type == Boolean::class.javaPrimitiveType }
        if (boolFields.isNotEmpty()) {
            boolFields[0].also { it.isAccessible = true }.set(tweet, null)
        }
    }
}
