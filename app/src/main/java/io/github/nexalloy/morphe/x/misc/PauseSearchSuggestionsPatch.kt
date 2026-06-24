package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Prevents search queries from being saved locally (search history). */
val PauseSearchSuggestions = patch(
    name = "Pause search suggestions",
    description = "Search suggestions will not be saved locally.",
) {
    dependsOn(XVersionCheck)

    SearchDbInsertFingerprint.hookMethod {
        before { param ->
            param.result = Unit  // skip DB insert entirely
        }
    }
}
