package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Blocks the redirect to the X Lite Android UI on launch. */
val BlockRedirectToXLite = patch(
    name = "Block redirecting to X Lite",
    description = "Blocks X from redirecting to the X Lite UI on launch.",
    use = true,
) {
    dependsOn(XVersionCheck)

    RedirectingToXLiteFingerprint.hookMethod {
        after { param ->
            // The method checks three feature flags and stores the result in shared prefs.
            // Force result to false so redirects never occur.
            param.result = false
        }
    }
}
