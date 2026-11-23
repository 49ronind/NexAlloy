package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy

import android.app.Activity
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.callMethodOrNull
import io.github.nexalloy.getIntFieldOrNull
import io.github.nexalloy.getObjectFieldOrNull
import io.github.nexalloy.patch
import org.json.JSONObject
import java.util.Locale

private const val LOCAL_TELEMETRY_URL = "https://localhost/"
private const val DELIVERED_PRIORITY_DATA = "{\"message\":\"success\",\"magic_tag\":\"ss_app_log\"}"

private fun hasTelemetryLabel(url: String): Boolean {
    val schemeEnd = url.indexOf("://")
    val start = if (schemeEnd < 0) 0 else schemeEnd + 3
    var end = start
    while (end < url.length && "/:?#".indexOf(url[end]) < 0) end++
    return url.substring(start, end).split('.').any { it.equals("telemetry", ignoreCase = true) }
}

private fun sanitizeOrbuUrl(url: String?): String? {
    if (url == null || !hasTelemetryLabel(url)) return url
    return LOCAL_TELEMETRY_URL
}

val DisableTelemetry = patch(
    name = "Disable telemetry",
    description = "Blocks AppLog, AppsFlyer, Firebase Analytics, Crash/Npth reporting, and Orbu telemetry uploads (HushFeed).",
    use = false,
) {
    // Void AppLog, Firebase, MonitorCrash, Npth, AppsFlyer init & forward sinks
    listOf(
        AppLogOnEventFingerprint,
        AppLogOnEventV3StringFingerprint,
        AppLogOnEventV3JsonFingerprint,
        AppLogOnEventV3BundleFingerprint,
        AppLogOnMiscEventFingerprint,
        AppLogFlushFingerprint,
        AppLogFlushAsyncFingerprint,
        AppLogOnActivityPauseFingerprint,
        InitAppsFlyerRunFingerprint,
        FirebaseSetCurrentScreenFingerprint,
        MonitorCrashReportCustomErrFingerprint,
        MonitorCrashReportEventFingerprint,
        NpthCoreInitTaskFingerprint,
        NpthSecondInitTaskFingerprint,
        AppLogForwardSendFingerprint,
    ).forEach { fp ->
        runCatching {
            fp.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
    }

    // AppsFlyer singleton concrete overrides
    runCatching {
        val afClass = XposedHelpers.findClassIfExists("com.appsflyer.AppsFlyerLib", classLoader)
        val instance = afClass?.let { XposedHelpers.callStaticMethod(it, "getInstance") }
        val implClass = instance?.javaClass
        if (implClass != null) {
            val stub = object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            }
            XposedBridge.hookAllMethods(implClass, "logEvent", stub)
            XposedBridge.hookAllMethods(implClass, "logLocation", stub)
        }
    }

    // AppLog pack send: return HTTP 200 so worker drops the pack without retrying
    runCatching {
        AppLogSendPackFingerprint.hookMethod {
            before { param ->
                param.result = 200
            }
        }
    }

    // AppLog priority uploader: return synthetic 200 success PriorityHttpResponse
    runCatching {
        val responseClass = XposedHelpers.findClassIfExists(
            "com.bytedance.applog.priority.PriorityHttpResponse",
            classLoader,
        )
        AppLogPrioritySendFingerprint.hookMethod {
            before { param ->
                val synthetic = responseClass?.let {
                    runCatching {
                        XposedHelpers.newInstance(it, 200, "", DELIVERED_PRIORITY_DATA)
                    }.getOrNull()
                }
                if (synthetic != null) {
                    param.result = synthetic
                }
            }
        }
    }

    // Install SDK active check: return true so it does not re-fetch app_alert_check
    runCatching {
        InstallActiveCheckFingerprint.hookMethod {
            before { param ->
                param.result = true
            }
        }
    }

    // Orbu SDK log upload -> Unit
    runCatching {
        OrbuLogSendFingerprint.hookMethod {
            before { param ->
                param.result = Unit
            }
        }
    }

    // Orbu telemetry hosts -> https://localhost/
    runCatching {
        OrbuServerTypeBaseUrlFingerprint.hookMethod {
            after { param ->
                val url = param.result as? String ?: return@after
                val rewritten = sanitizeOrbuUrl(url)
                if (rewritten != url) param.result = rewritten
            }
        }
    }
    runCatching {
        ::orbuServerConfigInits.dexMethodList.forEach { init ->
            init.hookMethod {
                before { param ->
                    val url = param.args.getOrNull(1) as? String ?: return@before
                    val rewritten = sanitizeOrbuUrl(url)
                    if (rewritten != url) param.args[1] = rewritten
                }
            }
        }
    }
}

// --- Safe Browsing CAPTCHA Gate (HushFeed CaptchaGate) ---

private object CaptchaSafetyGate {
    private const val ACCOUNT_ACTIVITY_PREFIX = "com.ss.android.ugc.aweme.account."
    private val ACCOUNT_WORDS = setOf(
        "login", "logout", "signin", "signup", "register", "passport", "sms", "otp",
        "twice", "2fa", "mobile", "phone", "email", "password", "pwd",
    )

    private fun isAccountRoute(value: String?): Boolean {
        if (value.isNullOrEmpty()) return false
        return value.lowercase(Locale.ROOT)
            .split(Regex("[^a-z0-9]+"))
            .any { it in ACCOUNT_WORDS }
    }

    private fun isAccountVerification(activity: Activity?, detail: String?): Boolean {
        if (activity != null && activity.javaClass.name.startsWith(ACCOUNT_ACTIVITY_PREFIX)) {
            return true
        }
        val route = activity?.intent?.dataString
        return isAccountRoute(route) || isAccountRoute(detail)
    }

    fun shouldHide(activity: Activity?, detail: String?): Boolean {
        return !isAccountVerification(activity, detail)
    }
}

val HideCaptchaPopups = patch(
    name = "Hide CAPTCHA popups",
    description = "Skips browsing CAPTCHA puzzles while preserving sign-in, SMS, and 2FA security checks (HushFeed).",
    use = false,
) {
    runCatching {
        CaptchaPopupFingerprint.hookMethod {
            before { param ->
                val activity = param.args.getOrNull(0) as? Activity
                val riskInfo = param.args.getOrNull(1) as? String
                if (!CaptchaSafetyGate.shouldHide(activity, riskInfo)) return@before
                val callback = param.args.getOrNull(2)
                runCatching { callback?.callMethodOrNull("LIZJ") }
                param.result = null
            }
        }
    }
    runCatching {
        LegacyCaptchaPopupFingerprint.hookMethod {
            before { param ->
                val activity = param.args.getOrNull(0) as? Activity
                if (!CaptchaSafetyGate.shouldHide(activity, null)) return@before
                val callback = param.args.getOrNull(2)
                runCatching { callback?.callMethodOrNull("LIZJ") }
                param.result = null
            }
        }
    }
    runCatching {
        LiveHostCaptchaPopupFingerprint.hookMethod {
            before { param ->
                val activity = param.args.getOrNull(0) as? Activity
                val riskInfo = param.args.getOrNull(1) as? String
                if (!CaptchaSafetyGate.shouldHide(activity, riskInfo)) return@before
                val callback = param.args.getOrNull(2)
                runCatching { callback?.callMethodOrNull("LIZJ") }
                param.result = null
            }
        }
    }
    runCatching {
        BdTuringCaptchaPopupFingerprint.hookMethod {
            before { param ->
                val activity = param.args.getOrNull(0) as? Activity
                val verifyReq = param.args.getOrNull(1)
                val scene = verifyReq?.callMethodOrNull("LJIIJ") as? String
                // Fail-safe: only hide if scene is readable and not an account check.
                if (scene.isNullOrBlank() || !CaptchaSafetyGate.shouldHide(activity, scene)) return@before
                val callback = param.args.getOrNull(2)
                runCatching {
                    XposedHelpers.callMethod(
                        callback,
                        "onFail",
                        arrayOf<Class<*>>(Int::class.javaPrimitiveType!!, JSONObject::class.java),
                        3,
                        null,
                    )
                }
                param.result = null
            }
        }
    }
}

// --- Ghost Mode (HushFeed) ---

private fun isStoryAweme(aweme: Any?): Boolean {
    if (aweme == null) return false
    if (aweme.callMethodOrNull("getIsTikTokStory") == true) return true
    val type = (aweme.callMethodOrNull("getAwemeType") as? Number)?.toInt()
        ?: aweme.getIntFieldOrNull("awemeType")
        ?: return false
    return type == 40 || type == 45
}

val GhostMode = patch(
    name = "Ghost mode",
    description = "Hides story views, profile visits, typing indicators, and online activity status (HushFeed).",
    use = false,
) {
    runCatching {
        ::storyViewReporters.dexMethodList.forEach { method ->
            method.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
    }
    runCatching {
        ::profileViewReporters.dexMethodList.forEach { method ->
            method.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
    }
    runCatching {
        ::typingStatusReporters.dexMethodList.forEach { method ->
            method.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
    }
    // Block story play stats (/aweme/v1/aweme/stats/) while letting normal feed videos report.
    runCatching {
        AwemeStatsSenderFingerprint.hookMethod {
            before { param ->
                val aweme = param.args.getOrNull(4)
                if (isStoryAweme(aweme)) {
                    param.result = null
                }
            }
        }
    }
    // Hide online activity status ("Active now" green dot).
    listOf(
        OnlineStatusRegularReportFingerprint,
        OnlineStatusIrregularReportFingerprint,
    ).forEach { fp ->
        runCatching {
            fp.hookMethod {
                before { param ->
                    param.result = null
                }
            }
        }
    }
}

// --- Hide Read Receipts (HushFeed) ---

private fun isReadReceiptRequest(requestBody: Any?): Boolean {
    if (requestBody == null) return false
    return requestBody.getObjectFieldOrNull("mark_conversation_read_body") != null ||
        requestBody.getObjectFieldOrNull("batch_mark_read_body") != null
}

val HideReadReceipts = patch(
    name = "Hide read receipts",
    description = "Stops TikTok from telling others you read their messages while keeping local unread badges cleared (HushFeed).",
    use = false,
) {
    runCatching {
        ::chatRequestSendMethods.dexMethodList.forEach { method ->
            method.hookMethod {
                before { param ->
                    if (isReadReceiptRequest(param.args.getOrNull(1))) {
                        param.result = 0L
                    }
                }
            }
        }
    }
    runCatching {
        ChatRequestSendWithHeadersFingerprint.hookMethod {
            before { param ->
                if (isReadReceiptRequest(param.args.getOrNull(1))) {
                    param.result = null
                }
            }
        }
    }
}
