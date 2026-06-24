package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Shows the source label (client app name) on public posts.
 * Mirrors piko: forces "show_tweet_source_disabled" flag check to proceed.
 */
val ShowSourceLabel = patch(
    name = "Show post source label",
    description = "Shows the source label (e.g. 'Twitter for iPhone') on posts.",
) {
    dependsOn(XVersionCheck)

    SourceLabelFingerprint.hookMethod {
        before { param ->
            // Skip the feature-flag gate that disables source labels.
            param.result = true
        }
    }
}
