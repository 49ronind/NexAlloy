package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Forces the "Translate" option to appear on all posts regardless of language.
 * The second boolean field in JsonApiTweet controls translate visibility.
 */
val ForceTranslate = patch(
    name = "Force enable translate",
    description = "Shows the Translate option on all posts regardless of language.",
) {
    dependsOn(XVersionCheck, TweetInfoHook)

    XTweetInfoProcessors.register { tweet ->
        val cls = tweet.javaClass
        val boolFields = cls.declaredFields.filter { it.type == Boolean::class.javaPrimitiveType }
        if (boolFields.size >= 2) {
            boolFields[1].also { it.isAccessible = true }.set(tweet, true)
        }
    }
}
