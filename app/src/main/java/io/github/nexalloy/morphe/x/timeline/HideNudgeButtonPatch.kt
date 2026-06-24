package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Hides follow/subscribe/follow-back nudge buttons on posts. */
val HideNudgeButton = patch(
    name = "Hide nudge button",
    description = "Hides follow/subscribe/follow-back nudge buttons shown on posts.",
) {
    dependsOn(XVersionCheck)

    HideNudgeButtonFingerprint.hookMethod {
        before { param ->
            // piko sets the button visibility to GONE (0x8).
            // In Xposed, the simplest approach is to return early, preventing the button binding.
            param.result = Unit
        }
    }
}
