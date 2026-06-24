package io.github.nexalloy.morphe.x.sharemenu

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Enables the "Share tweet as image" option in the tweet share menu.
 * Mirrors piko's NativeShareImagePatch.
 */
val NativeShareImage = patch(
    name = "Share Tweet as Image",
    description = "Enables the 'Share tweet as image' option in the share menu.",
) {
    dependsOn(XVersionCheck)

    ShareImageFingerprint.hookMethod {
        after { param ->
            if (param.result is Boolean) param.result = true
        }
    }
}
