package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Internal base patch: hooks JsonApiTweet.parse() and runs registered processors.
 * HideCommunityNotes, HidePromoteButton, ForceTranslate all depend on this.
 */
internal val TweetInfoHook = patch(
    name = "Tweet info hook",
    description = "Internal: hooks the tweet-info JSON parser for downstream patches.",
) {
    dependsOn(XVersionCheck)

    TweetInfoHookFingerprint.hookMethod {
        after { param ->
            val tweet = param.result ?: return@after
            XTweetInfoProcessors.process(tweet)
        }
    }
}

/** Registry of tweet-info processors (populated by dependent patches). */
internal object XTweetInfoProcessors {
    private val processors = mutableListOf<(Any) -> Unit>()

    fun register(processor: (Any) -> Unit) { processors += processor }

    fun process(tweet: Any) {
        for (proc in processors) {
            try { proc(tweet) } catch (_: Exception) {}
        }
    }
}
