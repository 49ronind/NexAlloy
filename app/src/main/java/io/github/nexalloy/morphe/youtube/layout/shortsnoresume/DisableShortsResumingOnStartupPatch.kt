package io.github.nexalloy.morphe.youtube.layout.shortsnoresume

import app.morphe.extension.youtube.patches.DisableShortsResumingOnStartupPatch
import io.github.nexalloy.morphe.shared.misc.litho.filter.featureFlagCheck
import io.github.nexalloy.morphe.shared.misc.settings.preference.SwitchPreference
import io.github.nexalloy.morphe.youtube.misc.settings.PreferenceScreen
import io.github.nexalloy.patch

internal const val USER_WAS_IN_SHORTS_FEATURE_FLAG = 45358360L

val DisableShortsResumingOnStartup = patch(
    name = "Disable Shorts resuming on startup",
    description = "Adds an option to disable Shorts from resuming on app startup when Shorts were last being watched.",
) {
    PreferenceScreen.SHORTS.addPreferences(
        SwitchPreference("morphe_disable_shorts_resuming_on_startup"),
    )

    // TODO UserWasInShortsEvaluateFingerprint (21.03+) — METHOD_MID
    // TODO UserWasInShortsListenerFingerprint (20.03-21.02) — METHOD_MID
    // TODO UserWasInShortsLegacyFingerprint (<20.03) — METHOD_MID

    ::featureFlagCheck.hookMethod {
        after {
            if (it.args[0] == USER_WAS_IN_SHORTS_FEATURE_FLAG) {
                it.result = DisableShortsResumingOnStartupPatch
                    .disableShortsResumingOnStartup(it.result as Boolean)
            }
        }
    }
}
