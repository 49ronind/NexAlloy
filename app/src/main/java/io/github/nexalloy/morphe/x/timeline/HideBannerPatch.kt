package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Hides the "new posts" banner that appears at the top of the timeline. */
val HideBanner = patch(
    name = "Hide Banner",
    description = "Hides the new-posts banner at the top of the timeline.",
) {
    dependsOn(XVersionCheck)

    HideBannerFingerprint.hookMethod {
        before { param ->
            // BaseNewTweetsBannerPresenter returns a boolean controlling banner visibility.
            // Return false to always hide the banner.
            param.result = false
        }
    }
}
