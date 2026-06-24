package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Hides like, repost, reply counts from posts. */
val HidePostMetrics = patch(
    name = "Hide post metrics",
    description = "Hides like, repost, and reply counts from posts.",
) {
    dependsOn(XVersionCheck)

    // Inline action bar metrics (like/repost count next to buttons)
    InlineActionViewTextFingerprint.hookMethod {
        before { param ->
            param.result = Unit  // skip setText → no count shown
        }
    }

    // Detailed post metrics (shown on post detail screen)
    TweetStatViewTextFingerprint.hookMethod {
        before { param ->
            param.result = Unit
        }
    }
}
