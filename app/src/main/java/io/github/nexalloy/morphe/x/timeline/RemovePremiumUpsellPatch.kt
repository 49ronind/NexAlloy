package io.github.nexalloy.morphe.x.timeline

import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.patch

/** Removes the Premium subscription upsell card from the home timeline. */
val RemovePremiumUpsell = patch(
    name = "Remove premium upsell",
    description = "Removes the X Premium upsell card from the home timeline.",
) {
    dependsOn(XVersionCheck)

    RemovePremiumUpsellFingerprint.hookMethod {
        before { param ->
            // The method checks "subscriptions_upsells_premium_home_nav" flag;
            // return false early to prevent the upsell from showing.
            param.result = false
        }
    }
}
