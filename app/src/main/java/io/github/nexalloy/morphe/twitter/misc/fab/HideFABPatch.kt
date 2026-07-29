package io.github.nexalloy.morphe.twitter.misc.fab

import io.github.nexalloy.morphe.twitter.featureFlag.featureFlagPatch.FeatureFlagHook
import io.github.nexalloy.morphe.twitter.featureFlag.featureFlagPatch.featureFlagOverrides
import io.github.nexalloy.patch

val HideFAB = patch(
    name = "Hide FAB",
    description = "Hides the floating action button (compose tweet button).",
) {
    dependsOn(FeatureFlagHook)
    featureFlagOverrides["android_compose_fab_menu_enabled"] = false
}
