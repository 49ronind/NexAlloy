package io.github.nexalloy.epxec.morphe.haloreelspro.premium

import io.github.nexalloy.patch

val EnableVip = patch(
    name = "Enable VIP",
    description = "Enables the VIP features of the app.",
) {
    runCatching {
        HaloreelsproVipFingerprint.hookMethod {
            before { param ->
                param.result = true
            }
        }
    }
}
