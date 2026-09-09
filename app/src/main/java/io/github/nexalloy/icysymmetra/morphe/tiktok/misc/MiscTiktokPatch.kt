package io.github.nexalloy.icysymmetra.morphe.tiktok.misc

import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch

val SimSpoof = patch(
    name = "SIM region spoof",
    description = "Spoofs SIM country and operator to US (T-Mobile) to bypass regional restrictions.",
) {
    runCatching {
        val tm = TelephonyManager::class.java
        val isoHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = "us"
            }
        }
        val opHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = "310260"
            }
        }
        val nameHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = "T-Mobile"
            }
        }

        XposedBridge.hookAllMethods(tm, "getSimCountryIso", isoHook)
        XposedBridge.hookAllMethods(tm, "getNetworkCountryIso", isoHook)
        XposedBridge.hookAllMethods(tm, "getSimOperator", opHook)
        XposedBridge.hookAllMethods(tm, "getNetworkOperator", opHook)
        XposedBridge.hookAllMethods(tm, "getSimOperatorName", nameHook)
        XposedBridge.hookAllMethods(tm, "getNetworkOperatorName", nameHook)
    }
}

val SanitizeShareUrls = patch(
    name = "Sanitize sharing links",
    description = "Removes tracking query parameters from shared TikTok links.",
) {
    runCatching {
        ShareUrlTrackerFingerprint.hookMethod {
            after { param ->
                val url = param.result as? String ?: return@after
                if (url.contains('?')) {
                    param.result = url.substringBefore('?')
                }
            }
        }
    }
}
