package io.github.nexalloy.morphe.x.premium

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Redirects the Bookmarks navigation-bar tab so it opens the bookmarks
 * screen instead of the premium-upsell screen.
 * Mirrors piko's RedirectBMTab.
 */
val RedirectBMTab = patch(
    name = "Redirect bookmark tab",
    description = "Makes the bookmarks nav-bar icon open bookmarks directly (not a premium upsell).",
) {
    dependsOn(XVersionCheck)

    BookmarkTabRedirectFingerprint.hookMethod {
        before { param ->
            // The method checks whether to redirect to premium; return false to skip redirect.
            param.result = false
        }
    }
}
