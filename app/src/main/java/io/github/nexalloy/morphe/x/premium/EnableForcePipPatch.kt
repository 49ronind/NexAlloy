package io.github.nexalloy.morphe.x.premium

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Forces Picture-in-Picture (PiP) mode to be enabled automatically
 * when leaving the app while a video is playing.
 * Mirrors piko's EnableForcePipPatch.
 */
val EnableForcePip = patch(
    name = "Enable PiP mode automatically",
    description = "Automatically enters Picture-in-Picture mode when leaving the app during video playback.",
) {
    dependsOn(XVersionCheck)

    ForcePipFingerprint.hookMethod {
        before { param ->
            // Force isPipAvailable / pip-eligible check to return true
            param.result = true
        }
    }
}
