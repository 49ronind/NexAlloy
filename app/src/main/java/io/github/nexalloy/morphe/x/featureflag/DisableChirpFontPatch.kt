package io.github.nexalloy.morphe.x.featureflag

import io.github.nexalloy.morphe.x.common.XFeatureFlags
import io.github.nexalloy.patch

/** Disables X's Chirp typeface, reverting to the system font. */
val DisableChirpFont = patch(
    name = "Disable chirp font",
    description = "Disables X's Chirp typeface and reverts to the system font.",
) {
    dependsOn(FeatureFlagHook)
    // af_ui_chirp_enabled = false  → Chirp is disabled
    XFeatureFlags.register("af_ui_chirp_enabled", false)
}
