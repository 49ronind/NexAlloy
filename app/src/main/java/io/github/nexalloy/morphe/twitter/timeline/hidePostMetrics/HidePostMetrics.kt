package io.github.nexalloy.morphe.twitter.timeline.hidePostMetrics

import io.github.nexalloy.patch

/**
 * NOTE on TweetStatViewTextFingerprint: piko overwrites a local register
 * mid-method (after register reuse), not necessarily the raw parameter
 * value. We approximate this by blanking the last String parameter
 * (assumed to be the displayed stat text, e.g. "42") before the method
 * runs. Please verify detailed post metrics (likes/reposts counts on the
 * tweet detail screen) are actually hidden when testing - if not, the
 * blanked parameter index likely needs adjusting.
 */
val HidePostMetrics = patch(
    name = "Hide post metrics",
    description = "Hides like, repost, etc. counts.",
) {
    InlineActionViewTextFingerprint.hookMethod {
        before { param -> param.result = Unit }
    }

    TweetStatViewTextFingerprint.hookMethod {
        before { param ->
            val lastIndex = param.args.size - 1
            if (lastIndex >= 0 && param.args[lastIndex] is String) {
                param.args[lastIndex] = ""
            }
        }
    }
}
