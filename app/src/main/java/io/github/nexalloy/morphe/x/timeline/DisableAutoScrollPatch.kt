package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Prevents the timeline from scrolling to new posts automatically on launch. */
val DisableAutoScroll = patch(
    name = "Disable auto timeline scroll on launch",
    description = "Prevents the timeline from auto-scrolling to new posts on app launch.",
) {
    dependsOn(XVersionCheck)

    DisableAutoScrollFingerprint.hookMethod {
        // piko patches the LAST method in the class: return 0 immediately.
        before { param ->
            param.result = false
        }
    }
}
