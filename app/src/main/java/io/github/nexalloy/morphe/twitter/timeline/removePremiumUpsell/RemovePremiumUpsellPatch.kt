package io.github.nexalloy.morphe.twitter.timeline.removePremiumUpsell

import io.github.nexalloy.morphe.twitter.featureFlag.featureFlagPatch.FeatureFlagHook
import io.github.nexalloy.morphe.twitter.featureFlag.featureFlagPatch.featureFlagOverrides
import io.github.nexalloy.patch

val RemovePremiumUpsell = patch(
    name = "Remove premium upsell",
    description = "Removes the premium upsell banner in the home timeline.",
) {
    dependsOn(FeatureFlagHook)
    featureFlagOverrides["subscriptions_upsells_premium_home_nav"] = false
}
