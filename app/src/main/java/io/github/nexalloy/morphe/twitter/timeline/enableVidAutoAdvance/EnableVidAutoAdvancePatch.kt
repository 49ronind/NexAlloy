package io.github.nexalloy.morphe.twitter.timeline.enableVidAutoAdvance

import io.github.nexalloy.patch

val EnableVidAutoAdvance = patch(
    name = "Control video auto scroll",
    description = "Enables auto-advancing to the next video in immersive video view.",
) {
    EnableVidAutoAdvanceFingerprint.hookMethod {
        after { param -> param.result = 1 }
    }
}
