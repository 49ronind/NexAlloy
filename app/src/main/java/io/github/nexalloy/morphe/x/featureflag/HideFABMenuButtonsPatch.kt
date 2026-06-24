package io.github.nexalloy.morphe.x.featureflag

import io.github.nexalloy.morphe.x.common.XFeatureFlags
import io.github.nexalloy.patch

/** Hides the extra buttons that appear in the FAB (Floating Action Button) menu. */
val HideFABMenuButtons = patch(
    name = "Hide FAB Menu Buttons",
    description = "Hides the extra buttons shown when tapping the compose FAB.",
) {
    dependsOn(FeatureFlagHook)
    XFeatureFlags.register("android_compose_fab_menu_enabled", false)
}
