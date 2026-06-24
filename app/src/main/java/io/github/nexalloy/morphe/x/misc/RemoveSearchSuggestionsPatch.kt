package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Removes / hides search suggestions in the Explore section. */
val RemoveSearchSuggestions = patch(
    name = "Remove search suggestions",
    description = "Hides search suggestions in the Explore section.",
) {
    dependsOn(XVersionCheck)

    SearchSuggestionFingerprint.hookMethod {
        before { param ->
            // Return an empty Collection → no suggestions shown
            param.result = emptyList<Any>()
        }
    }
}
