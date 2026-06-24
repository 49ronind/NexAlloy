package io.github.nexalloy.morphe.x.premium

import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Enables the "undo post" grace-period window.
 * Pref key: "x_undo_posts_delay_ms"  (long, default 5000)
 * Mirrors piko's EnableUndoPostPatch.
 */
val EnableUndoPost = patch(
    name = "Enable Undo Posts",
    description = "Enables the post undo grace-period window.",
) {
    dependsOn(XVersionCheck)

    UndoPostsFingerprint.hookMethod {
        after { param ->
            val delayMs = XPref.getString("x_undo_posts_delay_ms").toLongOrNull() ?: 5_000L
            // undo_tweet_delay_ms is a long
            if ((param.result as? Long ?: 0L) <= 0L) {
                param.result = delayMs
            }
        }
    }
}
