package io.github.nexalloy.morphe.x.sharemenu

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Enables the native Reader Mode option in the tweet share menu.
 * Reader Mode presents long threads in a clean, distraction-free view.
 * Mirrors piko's NativeReaderModePatch: hooks the reader-mode feature flag to return true.
 */
val NativeReaderMode = patch(
    name = "Native reader mode",
    description = "Enables the Reader Mode option in the tweet share menu.",
) {
    dependsOn(XVersionCheck)

    ReaderModeToggleFingerprint.hookMethod {
        after { param ->
            // Force article_reader_enabled to true
            if (param.result is Boolean) param.result = true
        }
    }
}
