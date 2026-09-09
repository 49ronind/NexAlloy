package io.github.nexalloy.icysymmetra.morphe.tiktok.login

import io.github.nexalloy.morphe.Fingerprint
import org.luckypray.dexkit.query.enums.StringMatchType

object MandatoryLoginEnableFingerprint : Fingerprint(
    name = "enableForcedLogin",
    returnType = "Z",
) {
    init {
        classMatcher { className("MandatoryLoginService", StringMatchType.EndsWith) }
    }
}

object MandatoryLoginShouldShowFingerprint : Fingerprint(
    name = "shouldShowForcedLogin",
    returnType = "Z",
) {
    init {
        classMatcher { className("MandatoryLoginService", StringMatchType.EndsWith) }
    }
}
