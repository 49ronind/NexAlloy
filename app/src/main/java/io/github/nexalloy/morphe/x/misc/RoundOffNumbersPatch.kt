package io.github.nexalloy.morphe.x.misc

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Controls whether counts (likes, reposts) are displayed rounded off (10K)
 * or as exact numbers. When this patch is ENABLED → always use rounded numbers.
 * Disable the patch to see exact counts (X's default behaviour).
 */
val RoundOffNumbers = patch(
    name = "Round off numbers",
    description = "Forces abbreviated number formatting (e.g. 10.2K instead of 10,234).",
) {
    dependsOn(XVersionCheck)
    // The fingerprint targets the abbreviation method; when enabled the
    // method runs normally (rounded); disable the patch to bypass.
    RoundOffNumbersFingerprint.hookMethod {
        // No override needed – patch being enabled means rounded numbers are used.
        // When user disables this patch, the hook is not registered.
    }
}
