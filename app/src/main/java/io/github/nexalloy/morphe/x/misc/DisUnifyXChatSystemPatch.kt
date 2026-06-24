package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.morphe.x.common.x_is_11_70_or_greater
import io.github.nexalloy.patch

/**
 * Restores legacy messaging and share sheet by disabling the unified XChat tab.
 * Only works on X < 11.70; no-ops on later versions.
 */
val DisUnifyXChatSystem = patch(
    name = "Disunify xchat system",
    description = "Restores legacy DMs and share sheet (X ≤ 11.69 only).",
    use = false,
) {
    dependsOn(XVersionCheck)

    if (x_is_11_70_or_greater) return@patch  // not supported on 11.70+

    XchatSubSystemUserCheckFingerprint.hookMethod {
        before { param ->
            // Return false → user is NOT above the snowflake threshold → legacy UI used
            param.result = false
        }
    }
}
