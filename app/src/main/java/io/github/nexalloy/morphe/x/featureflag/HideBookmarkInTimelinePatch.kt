package io.github.nexalloy.morphe.x.featureflag

import io.github.nexalloy.morphe.x.common.XFeatureFlags
import io.github.nexalloy.patch

/** Hides the inline bookmark icon shown on timeline posts. */
val HideBookmarkInTimeline = patch(
    name = "Hide bookmark icon in timeline",
    description = "Hides the bookmark icon shown below each post in the timeline.",
) {
    dependsOn(FeatureFlagHook)
    XFeatureFlags.register("bookmarks_in_timelines_enabled", false)
}
