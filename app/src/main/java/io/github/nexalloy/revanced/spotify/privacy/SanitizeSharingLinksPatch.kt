package io.github.nexalloy.revanced.spotify.privacy

import android.content.ClipData
import android.net.Uri
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch

val SanitizeSharingLinks = patch(
    name = "Sanitize sharing links",
    description = "Removes tracking query parameters (such as si and utm) when copying or sharing links.",
) {
    fun sanitizeUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            if (uri.host?.contains("spotify.com") != true) return url
            val builder = uri.buildUpon().clearQuery()
            for (name in uri.queryParameterNames) {
                if (name != "si" && !name.startsWith("utm_")) {
                    for (v in uri.getQueryParameters(name)) {
                        builder.appendQueryParameter(name, v)
                    }
                }
            }
            builder.build().toString()
        } catch (_: Throwable) {
            url
        }
    }

    runCatching {
        val clipDataClass = ClipData::class.java
        val method = clipDataClass.getMethod(
            "newPlainText", CharSequence::class.java, CharSequence::class.java
        )
        XposedBridge.hookMethod(method, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val text = param.args[1] as? String ?: return
                if (text.contains("spotify.com")) {
                    param.args[1] = sanitizeUrl(text)
                }
            }
        })
    }
}
