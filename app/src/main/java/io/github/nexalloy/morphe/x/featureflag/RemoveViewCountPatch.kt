package io.github.nexalloy.morphe.x.featureflag

import io.github.nexalloy.morphe.x.common.XFeatureFlags
import io.github.nexalloy.patch

/** Hides the view count shown at the bottom of posts. */
val RemoveViewCount = patch(
    name = "Remove view count",
    description = "Hides the view count shown beneath posts.",
) {
    dependsOn(FeatureFlagHook)
    XFeatureFlags.register("view_counts_public_visibility_enabled", false)
}
