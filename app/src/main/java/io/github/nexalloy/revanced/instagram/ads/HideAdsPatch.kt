package io.github.nexalloy.revanced.instagram.ads

import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Hides injected ads, sponsored content, paid partnership, and Reels/Stories ads."
) {

    runCatching { ::feedAcpContentInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::feedAdsProxyFetcherFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::adControllerIndexFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::adDeliveredFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::adContentDeliveredExternallyFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::adInsertGateFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false)) }
    runCatching { ::adHighestPositionGateFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false)) }
    runCatching { ::clipsAdPrewarmFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::sponsoredReelItemBinderFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::sponsoredReelMediaFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::sponsoredReelLabelFooterFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::sponsoredReelLabelOnBottomFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::storiesAdsBinderFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::storiesAdsPrepareFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::sponsoredStoriesLikeButtonFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::paidPartnershipLabelFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching { ::gridSponsoredPoolFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING) }
    runCatching {
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
