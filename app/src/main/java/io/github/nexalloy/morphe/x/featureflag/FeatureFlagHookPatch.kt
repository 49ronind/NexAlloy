package io.github.nexalloy.morphe.x.featureflag

import app.morphe.extension.shared.Logger
import io.github.nexalloy.morphe.x.common.XFeatureFlags
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/**
 * Base patch: intercepts X's feature-flag boolean getter and redirects
 * any flag registered in [XFeatureFlags] to the desired value.
 *
 * All individual feature-flag patches must [dependsOn] this patch.
 */
val FeatureFlagHook = patch(
    name = "Hook feature flag",
    description = "Intercepts X feature flags so individual patches can override them.",
) {
    dependsOn(XVersionCheck)

    FeatureFlagBooleanMethodFingerprint.hookMethod {
        after { param ->
            val flagName = param.args[0] as? String ?: return@after
            val override = XFeatureFlags.getOverride(flagName) ?: return@after
            Logger.printDebug { "FeatureFlag: $flagName -> $override (was ${param.result})" }
            param.result = override
        }
    }
}
