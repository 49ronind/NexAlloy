package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Hides the Floating Action Button (compose button). */
val HideFAB = patch(
    name = "Hide FAB",
    description = "Hides the floating compose button.",
) {
    dependsOn(XVersionCheck)

    HideFABFingerprint.hookMethod {
        before { param ->
            // The method checks the FAB menu flag; returning false hides the FAB.
            param.result = false
        }
    }
}
