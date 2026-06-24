package io.github.nexalloy.morphe.x.link

import io.github.nexalloy.morphe.x.common.XPref
import io.github.nexalloy.morphe.x.common.XVersionCheck
import io.github.nexalloy.morphe.x.common.x_is_11_40_or_greater
import io.github.nexalloy.patch

/**
 * Replaces "x.com" in share URLs with a user-specified domain
 * (e.g. fxtwitter.com, vxtwitter.com).
 *
 * Pref key: "x_custom_sharing_domain"
 */
val CustomSharingDomain = patch(
    name = "Custom sharing domain",
    description = "Replaces x.com in share URLs with a custom domain (e.g. fxtwitter.com).",
    use = false,
) {
    dependsOn(XVersionCheck)

    // Legacy share path (< 11.40): hook AddSessionToken which receives the URL as arg[0]
    AddSessionTokenFingerprint.hookMethod {
        before { param ->
            val domain = XPref.getString("x_custom_sharing_domain").ifEmpty { return@before }
            val url = param.args[0] as? String ?: return@before
            param.args[0] = url.replace("https://x.com/", "https://$domain/")
                               .replace("https://twitter.com/", "https://$domain/")
        }
    }

    // Modern share sheet path (>= 11.40): hook link builder methods
    if (x_is_11_40_or_greater) {
        fun rewriteLink(param: de.robv.android.xposed.XC_MethodHook.MethodHookParam) {
            val domain = XPref.getString("x_custom_sharing_domain").ifEmpty { return }
            val result = param.result as? String ?: return
            param.result = result.replace("https://x.com/", "https://$domain/")
                                 .replace("https://twitter.com/", "https://$domain/")
        }

        NewShareSheetLinkFingerprint1.hookMethod { after { rewriteLink(it) } }
        NewShareSheetLinkFingerprint2.hookMethod { after { rewriteLink(it) } }
    }
}
