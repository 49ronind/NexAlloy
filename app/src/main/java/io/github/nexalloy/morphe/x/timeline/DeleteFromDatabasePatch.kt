package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Enables the "Delete from database" option in settings.
 * In NexAlloy this is a toggle that exposes the cache-clearing functionality
 * already present in X's settings activity.
 */
val DeleteFromDatabase = patch(
    name = "Delete from database",
    description = "Adds an option to delete cached timeline entries from the local database.",
    use = false,
) {
    dependsOn(XVersionCheck)
    // Settings entry only – the actual DB deletion is handled by X's own mechanism.
    // This patch exists so NexAlloy settings shows the toggle.
}
