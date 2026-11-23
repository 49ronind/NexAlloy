package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.login

import io.github.nexalloy.patch

val DisableForcedLogin = patch(
    name = "Disable login requirement",
    description = "Stops TikTok from forcing sign-in before browsing (HushFeed).",
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
