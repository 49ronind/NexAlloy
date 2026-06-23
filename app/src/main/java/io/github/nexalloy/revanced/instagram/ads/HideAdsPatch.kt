package io.github.nexalloy.revanced.instagram.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Hides injected ads, sponsored content, paid partnership, and Reels/Stories ads."
) {

    runCatching {
        ::feedAcpContentInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::feedAdsProxyFetcherFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::adControllerIndexFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::adDeliveredFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::adContentDeliveredExternallyFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::adInsertGateFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
        ::adHighestPositionGateFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
        ::clipsAdPrewarmFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::sponsoredReelItemBinderFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::sponsoredReelMediaFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::sponsoredReelLabelFooterFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::sponsoredReelLabelOnBottomFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::storiesAdsBinderFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::storiesAdsPrepareFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::sponsoredStoriesLikeButtonFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::paidPartnershipLabelFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::gridSponsoredPoolFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::clipsAdAddItemFingerprint.hookMethod {
            before { param ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val items = param.args[0] as? MutableList<Any?> ?: return@before
                    param.args[0] = items.filter { item ->
                        item != null && !item.javaClass.name.contains("Sponsored", ignoreCase = true)
                    }
                } catch (_: Exception) {}
            }
        }
    }
}
