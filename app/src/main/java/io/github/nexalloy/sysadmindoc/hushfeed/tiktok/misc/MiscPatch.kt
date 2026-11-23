package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.misc

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.telephony.TelephonyManager
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch

val SimSpoof = patch(
    name = "SIM region spoof",
    description = "Spoofs SIM country and operator to US (T-Mobile) and suppresses SIM-change telemetry (HushFeed).",
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

    runCatching {
        CheckSimChangeTaskFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
}

val SanitizeShareUrls = patch(
    name = "Sanitize sharing links",
    description = "Removes tracking query parameters from shared TikTok links (HushFeed).",
) {
    runCatching {
        ShareUrlTrackerFingerprint.hookMethod {
            before { param ->
                val url = param.args.getOrNull(1) as? String ?: return@before
                if (url.startsWith("http://") || url.startsWith("https://")) {
                    val q = url.indexOf('?')
                    param.result = if (q > 0) url.substring(0, q) else url
                }
            }
            after { param ->
                val url = param.result as? String ?: return@after
                val q = url.indexOf('?')
                if (q > 0) {
                    param.result = url.substring(0, q)
                }
            }
        }
    }
}

val CopyCommentsWithoutUsername = patch(
    name = "Copy comments without username",
    description = "Copies only the comment text without the leading username prefix (HushFeed).",
) {
    runCatching {
        CommentClipDataBuilderFingerprint.hookMethod {
            before { param ->
                if (param.args.getOrNull(0) is String) {
                    param.args[0] = ""
                }
            }
        }
    }
}

// --- Open External Links Directly (HushFeed ExternalBrowserPatch) ---

private object ExternalLinkRouter {
    private val SAFETY_HOSTS = setOf(
        "www.tiktok.com",
        "www.tiktoklinksafety.us",
        "www.tiktoklinksafety.eu",
        "www.tiktoklinksafety.com",
    )

    private fun isLinkSafetyRedirect(uri: Uri): Boolean {
        if (!"https".equals(uri.scheme, ignoreCase = true)) return false
        if (uri.userInfo != null || (uri.port != -1 && uri.port != 443)) return false
        val path = uri.path ?: return false
        if (!path.startsWith("/link/")) return false
        val host = uri.host?.lowercase() ?: return false
        return host in SAFETY_HOSTS
    }

    fun resolveTarget(source: String?): Uri? {
        var value = source?.trim() ?: return null
        repeat(4) {
            val uri = runCatching { Uri.parse(value) }.getOrNull() ?: return null
            val scheme = uri.scheme
            if (scheme == null) {
                value = "https://$value"
                return@repeat
            }
            if (scheme.equals("aweme", ignoreCase = true)) {
                value = runCatching { uri.getQueryParameter("url") }.getOrNull() ?: return null
                return@repeat
            }
            val host = uri.host
            if (!(scheme.equals("http", ignoreCase = true) || scheme.equals("https", ignoreCase = true)) ||
                host.isNullOrBlank()
            ) {
                return null
            }
            if (isLinkSafetyRedirect(uri)) {
                val target = runCatching { uri.getQueryParameter("target") }.getOrNull()
                if (target != null) {
                    value = target
                    return@repeat
                }
            }
            return uri
        }
        return null
    }

    fun isAllowedScreen(target: Any?): Boolean {
        if (target == null) return false
        if ("bio_url" == target.getObjectFieldOrNull("seclinkConfig")?.callMethodOrNull("getScene")) {
            return true
        }
        val params = target.getObjectFieldOrNull("defaultParams") as? Map<*, *>
        return params?.get("sec_link_scene") == "bio_url"
    }

    fun findFieldByTypeName(target: Any?, typeName: String): Any? {
        var cls: Class<*>? = target?.javaClass ?: return null
        while (cls != null) {
            for (f in cls.declaredFields) {
                if (f.type.name == typeName) {
                    runCatching {
                        f.isAccessible = true
                        return f.get(target)
                    }
                }
            }
            cls = cls.superclass
        }
        return null
    }

    fun findContextField(target: Any?): Context? {
        var cls: Class<*>? = target?.javaClass ?: return null
        while (cls != null) {
            for (f in cls.declaredFields) {
                if (Context::class.java.isAssignableFrom(f.type)) {
                    runCatching {
                        f.isAccessible = true
                        val value = f.get(target)
                        if (value is Context) return value
                    }
                }
            }
            cls = cls.superclass
        }
        return null
    }

    fun openInBrowser(context: Context?, source: String?, classLoader: ClassLoader): Boolean {
        val uri = resolveTarget(source) ?: return false
        if (context == null) return false

        // Prefer TikTok's own IMainService.openSystemBrowser when available.
        val openedViaService = runCatching {
            val managerCls = XposedHelpers.findClass(
                "com.ss.android.ugc.aweme.framework.services.ServiceManager",
                classLoader,
            )
            val mainServiceCls = XposedHelpers.findClass(
                "com.ss.android.ugc.aweme.services.IMainService",
                classLoader,
            )
            val manager = XposedHelpers.callStaticMethod(managerCls, "get") ?: return@runCatching false
            val service = XposedHelpers.callMethod(manager, "getService", mainServiceCls)
                ?: return@runCatching false
            XposedHelpers.callMethod(service, "openSystemBrowser", context, uri.toString()) == true
        }.getOrDefault(false)
        if (openedViaService) return true

        // Fallback to standard Android ACTION_VIEW intent.
        return runCatching {
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        }.getOrDefault(false)
    }
}

val OpenExternalLinks = patch(
    name = "Open external links directly",
    description = "Opens profile bio and story links in your system browser instead of TikTok's in-app browser (HushFeed).",
) {
    runCatching {
        SparkThirdRouterOpenFingerprint.hookMethod {
            before { param ->
                val ctx = param.args.getOrNull(0) as? Context ?: return@before
                val sparkThird = param.args.getOrNull(1) ?: return@before
                if (!ExternalLinkRouter.isAllowedScreen(sparkThird)) return@before
                val url = sparkThird.getObjectFieldOrNull("url") as? String
                if (ExternalLinkRouter.openInBrowser(ctx, url, classLoader)) {
                    param.result = null
                }
            }
        }
    }

    runCatching {
        StoryLinkSheetFingerprint.hookMethod {
            before { param ->
                val sticker = param.args.getOrNull(0) ?: return@before
                if (sticker.callMethodOrNull("getType") != 106) return@before
                val url = sticker.callMethodOrNull("getUrlLinkSticker")
                    ?.callMethodOrNull("getFullURL") as? String ?: return@before
                val ctx = ExternalLinkRouter.findContextField(param.thisObject)
                if (ExternalLinkRouter.openInBrowser(ctx, url, classLoader)) {
                    param.result = null
                }
            }
        }
    }

    runCatching {
        SparkActivityOnCreateFingerprint.hookMethod {
            after { param ->
                val activity = param.thisObject as? Activity ?: return@after
                val sparkCtx = ExternalLinkRouter.findFieldByTypeName(
                    activity,
                    "com.bytedance.hybrid.spark.SparkContext",
                ) ?: return@after
                if (!ExternalLinkRouter.isAllowedScreen(sparkCtx)) return@after
                val url = sparkCtx.getObjectFieldOrNull("url") as? String
                if (ExternalLinkRouter.openInBrowser(activity, url, classLoader)) {
                    activity.finish()
                }
            }
        }
    }
}
