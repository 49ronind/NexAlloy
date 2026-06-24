package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Controls whether videos auto-advance in the immersive player.
 * Disabling returns a very large threshold so auto-advance never triggers.
 */
val EnableVidAutoAdvance = patch(
    name = "Control video auto scroll",
    description = "Controls video auto-scroll behaviour in the immersive player.",
) {
    dependsOn(XVersionCheck)

    EnableVidAutoAdvanceFingerprint.hookMethod {
        after { param ->
            // Return Int.MAX_VALUE so threshold is never reached → no auto-advance
            if (param.result is Int) param.result = Int.MAX_VALUE
        }
    }
}
