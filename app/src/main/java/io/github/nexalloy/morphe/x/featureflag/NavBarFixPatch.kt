package io.github.nexalloy.morphe.x.featureflag

import io.github.nexalloy.morphe.x.common.XFeatureFlags
import io.github.nexalloy.morphe.x.common.x_is_11_95_or_greater
import io.github.nexalloy.patch

/**
 * Fixes navigation-bar customisation availability by forcing
 * subscriptions_feature_1008 = true.
 * Required by [io.github.nexalloy.morphe.x.customize.CustomNavBar].
 */
internal val NavBarFix = patch(
    name = "Nav bar fix",
    description = "Internal: enables nav-bar customisation feature flags.",
) {
    dependsOn(FeatureFlagHook)
    XFeatureFlags.register("subscriptions_feature_1008", true)
    if (x_is_11_95_or_greater) {
        XFeatureFlags.register("subscriptions_feature_1008_sunset", false)
    }
}
