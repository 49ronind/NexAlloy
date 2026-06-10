package io.github.nexalloy.revanced.instagram.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
    description = "Hides injected ads, sponsored content, paid partnership, and Reels/Stories ads."
) {

    runCatching {
        ::adV2DeliveryFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::adV2InsertGateFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
        ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::adSponsoredContentFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
        ::adInsertionActionFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::adContentDeliveredFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::feedAdsProxyFetcherFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::clipsAdPrewarmFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::storiesAdsBinderFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::paidPartnershipLabelFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::gridSponsoredPoolFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
        ::clipsAdAddItemFingerprint.hookMethod {
            before { param ->
                try {
                    @Suppress("UNCHECKED_CAST")
                    val items = param.args[0] as? MutableList<Any?> ?: return@before
                    val filtered = items.filter { item ->
                        item != null && !item.javaClass.name.contains("Sponsored", ignoreCase = true)
                    }
                    param.args[0] = filtered
                } catch (e: Exception) {
                }
            }
        }
    }
}
