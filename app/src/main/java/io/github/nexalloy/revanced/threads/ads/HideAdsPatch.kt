package io.github.nexalloy.revanced.threads.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Hides injected ads, sponsored content, and paid partnership posts in Threads feed."
) {

    runCatching {
        ::adFetchSponsoredContentFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    runCatching {
        ::adContentDeliveredFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    runCatching {
        ::paidPartnershipLabelFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    runCatching {
        ::adMetadataFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    runCatching {
        ::sponsoredLabelInHeaderFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    }

    runCatching {
        ::spoolAdInjectorLambdaFingerprint.hookMethod(XC_MethodReplacement.returnConstant(null))
    }
}
