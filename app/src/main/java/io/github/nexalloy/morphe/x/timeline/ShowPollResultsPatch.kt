package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Shows poll results without voting.
 * Mirrors piko's Pref.polls(): appends counts_are_final=true to the binding map.
 */
val ShowPollResults = patch(
    name = "Show poll results",
    description = "Shows poll results without having to vote first.",
) {
    dependsOn(XVersionCheck)

    JsonCardInstanceDataFingerprint.hookMethod {
        after { param ->
            try {
                @Suppress("UNCHECKED_CAST")
                val map = param.args.filterIsInstance<MutableMap<Any?, Any?>>()
                    .firstOrNull() ?: return@after

                // Already finalised – no change needed
                if (map["counts_are_final"]?.toString() == "true") return@after

                // Mark as final so results are displayed
                map["counts_are_final"] = "true"
            } catch (_: Exception) {}
        }
    }
}
