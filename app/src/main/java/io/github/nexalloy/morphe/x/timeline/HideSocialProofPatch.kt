package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Hides "Followed by X and Y" social-proof context shown under profiles. */
val HideSocialProof = patch(
    name = "Hide followed by context",
    description = "Hides the 'Followed by ...' context shown under profile names.",
) {
    dependsOn(XVersionCheck)

    HideSocialProofFingerprint.hookMethod {
        before { param -> param.result = Unit }
    }
}
