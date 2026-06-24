package io.github.nexalloy.morphe.x.featureflag

import io.github.nexalloy.morphe.x.common.XFeatureFlags
import io.github.nexalloy.patch

/** Removes the immersive "swipe up for more videos" player. */
val HideImmersivePlayer = patch(
    name = "Hide immersive player",
    description = "Removes the swipe-up-for-more-videos immersive media player.",
) {
    dependsOn(FeatureFlagHook)
    XFeatureFlags.register("explore_relaunch_enable_immersive_player_across_twitter", false)
}
