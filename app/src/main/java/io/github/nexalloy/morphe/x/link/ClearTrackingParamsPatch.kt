package io.github.nexalloy.morphe.x.link

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Removes tracking parameters (session token, share param) when sharing links.
 * Equivalent to piko's ClearTrackingParamsPatch: early-returns p0 (the bare URL).
 */
val ClearTrackingParams = patch(
    name = "Clear tracking params",
    description = "Removes tracking query parameters when sharing links.",
    use = true,
) {
    dependsOn(XVersionCheck)

    AddSessionTokenFingerprint.hookMethod {
        before { param ->
            // Return the first argument (bare URL) before session token is appended.
            param.result = param.args[0]
        }
    }
}
