package io.github.nexalloy.icysymmetra.morphe.tiktok.login

import io.github.nexalloy.patch

val DisableForcedLogin = patch(
    name = "Disable login requirement",
    description = "Removes TikTok's mandatory login gate from browsing flows.",
) {
    runCatching {
        MandatoryLoginEnableFingerprint.hookMethod {
            before { param ->
                param.result = false
            }
        }
    }
    runCatching {
        MandatoryLoginShouldShowFingerprint.hookMethod {
            before { param ->
                param.result = false
            }
        }
    }
}
