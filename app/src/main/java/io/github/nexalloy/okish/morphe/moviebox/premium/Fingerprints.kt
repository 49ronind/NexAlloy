package io.github.nexalloy.okish.morphe.moviebox.premium

import io.github.nexalloy.morphe.Fingerprint

object MemberInfoIsActiveFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/memberapi/MemberInfo;",
    name = "isActive",
    returnType = "Z",
)

object MemberInfoVipLevelFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/memberapi/MemberInfo;",
    name = "getVipLevel",
    returnType = "I",
)

object MemberProviderIsMemberFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/MemberProvider;",
    name = "c",
    returnType = "Z",
)

object PremiumProviderIsActiveFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/premium/PremiumProvider;",
    name = "c",
    returnType = "Z",
)

object PremiumProviderIsProFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/premium/PremiumProvider;",
    name = "k",
    returnType = "Z",
)

object PremiumDaysLeftFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/premium/PremiumProvider;",
    name = "o",
    returnType = "Ljava/lang/Integer;",
)

object PremiumIsProMemberFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/premium/PremiumProvider;",
    name = "u",
    returnType = "Z",
)

object MemberProviderPayEnableFingerprint : Fingerprint(
    definingClass = "Lcom/transsion/member/MemberProvider;",
    returnType = "Z",
    strings = listOf("kv_is_pay_enable_member"),
)
