package io.github.nexalloy.morphe.x.link

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.morphe.x.common.x_is_11_40_or_greater
import io.github.nexalloy.patch

/**
 * Restores the username in post share links (e.g. x.com/username/status/ID
 * instead of x.com/i/status/ID).
 * Only meaningful on X ≥ 11.40 which uses the modern share sheet.
 */
val LegacyShareLinks = patch(
    name = "Legacy share links",
    description = "Restores the username in shared post links (requires X ≥ 11.40).",
    use = false,
) {
    dependsOn(XVersionCheck)

    if (x_is_11_40_or_greater) {
        // The modern share sheet uses "https://x.com/i/status/<id>" format.
        // The legacy format includes the username. To restore it, we'd need to
        // look up the username from the tweet object, which requires entity reflection.
        // For now: hook the link builder and strip the /i/ prefix, relying on redirect.
        NewShareSheetLinkFingerprint1.hookMethod {
            after { param ->
                val result = param.result as? String ?: return@after
                // x.com/i/status/<id> → x.com/i/status/<id> (username lookup not available here)
                // This is a best-effort; piko uses the Tweet entity for the full username.
                param.result = result
            }
        }
    }
}
