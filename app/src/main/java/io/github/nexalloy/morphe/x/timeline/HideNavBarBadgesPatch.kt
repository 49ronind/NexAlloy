package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Hides notification badges (counts) from navigation bar icons. */
val HideNavBarBadges = patch(
    name = "Hide badges from navigation bar icons",
    description = "Hides notification nudges and counts from navigation bar icons.",
) {
    dependsOn(XVersionCheck)

    HideNavBarBadgesFingerprint.hookMethod {
        before { param ->
            // setBadgeNumber(int count) → force count to 0
            param.args[0] = 0
        }
    }
}
