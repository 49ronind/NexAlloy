package io.github.nexalloy.okish.morphe.moviebox.ad

import io.github.nexalloy.patch

val AdRemoval = patch(
    name = "Remove ads",
    description = "Removes splash, banner, native, interstitial, and reward video ads in MovieBox.",
) {
    MemberProviderSkipAdFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }

    SkipShowAdStateFingerprint.hookMethod {
        before { param ->
            param.result = true
        }
    }

    MintegralVideoInitFingerprint.hookMethod {
        before { param ->
            param.result = null
        }
    }

    MintegralBannerShowFingerprint.hookMethod {
        before { param ->
            param.result = null
        }
    }

    MintegralNativeInitFingerprint.hookMethod {
        before { param ->
            param.result = null
        }
    }

    MintegralInterstitialInitFingerprint.hookMethod {
        before { param ->
            param.result = null
        }
    }

    MintegralSplashStartLoadFingerprint.hookMethod {
        before { param ->
            param.result = null
        }
    }
}
