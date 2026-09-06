package io.github.nexalloy.okish.morphe.moviebox.premium

import io.github.nexalloy.patch

val PremiumUnlock = patch(
    name = "Unlock Premium",
    description = "Unlocks MovieBox Premium (VIP) by forcing the member state active at Pro level.",
) {
    MemberInfoIsActiveFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }

    MemberInfoVipLevelFingerprint.hookMethod {
        before { param ->
            param.result = 1
        }
    }

    MemberProviderIsMemberFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }

    PremiumProviderIsActiveFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }

    PremiumProviderIsProFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }

    PremiumDaysLeftFingerprint.hookMethod {
        before { param ->
            param.result = 30
        }
    }

    PremiumIsProMemberFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }

    MemberProviderPayEnableFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }
}
