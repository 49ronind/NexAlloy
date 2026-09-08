package io.github.nexalloy.okish.morphe.moviebox.ad

import io.github.nexalloy.morphe.Fingerprint

object MemberProviderSkipAdFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/MemberProvider;",
    returnType = "Z",
    strings = listOf("kv_is_skip_ad"),
)

object SkipShowAdStateFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("j376W52LrKvau6r8"),
)

object MintegralVideoInitFingerprint : Fingerprint(
    definingClass = "Lcom/hisavana/mintegral/executer/MintegralVideo;",
    name = "initVideo",
    returnType = "V",
)

object MintegralBannerShowFingerprint : Fingerprint(
    definingClass = "Lcom/hisavana/mintegral/executer/MintegralBanner;",
    name = "showBanner",
    returnType = "V",
)

object MintegralNativeInitFingerprint : Fingerprint(
    definingClass = "Lcom/hisavana/mintegral/executer/MintegralNative;",
    name = "initNative",
    returnType = "V",
)

object MintegralInterstitialInitFingerprint : Fingerprint(
    definingClass = "Lcom/hisavana/mintegral/executer/MintegralInterstitial;",
    name = "initInterstitial",
    returnType = "V",
)

object MintegralSplashStartLoadFingerprint : Fingerprint(
    definingClass = "Lcom/hisavana/mintegral/executer/MintegralSplash;",
    name = "onSplashStartLoad",
    returnType = "V",
)
