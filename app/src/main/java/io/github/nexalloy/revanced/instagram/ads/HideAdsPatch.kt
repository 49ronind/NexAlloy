package io.github.nexalloy.revanced.instagram.ads

import app.morphe.extension.shared.Logger
import de.robv.android.xposed.XC_MethodReplacement
import io.github.nexalloy.patch

val HideAds = patch(
    name = "Hide ads",
) {
    ::adInjectorFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    ::adSponsoredContentFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
    ::adV2DeliveryFingerprint.hookMethod(XC_MethodReplacement.DO_NOTHING)
    ::adV2InsertGateFingerprint.hookMethod(XC_MethodReplacement.returnConstant(false))
}
