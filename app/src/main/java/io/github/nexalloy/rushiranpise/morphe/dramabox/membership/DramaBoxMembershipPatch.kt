package io.github.nexalloy.rushiranpise.morphe.dramabox.membership

import io.github.nexalloy.patch

val MembershipUnlock = patch(
    name = "Unlock VIP membership",
    description = "Unlocks DramaBox VIP membership, privilege badges, and ad-free episode playback.",
) {
    // Layer 1: Force VIP=true into DataStore on every write
    runCatching {
        VipStateSetterFingerprint.hookMethod {
            before { param ->
                param.args[0] = true
            }
        }
    }

    // Layer 2: Short-circuit VIP reads to true
    runCatching {
        VipStateGetterFingerprint.hookMethod {
            before { param ->
                param.result = true
            }
        }
    }

    // Layer 3: Suppress billing callback VIP reset race
    runCatching {
        BillingSuccessFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
}
