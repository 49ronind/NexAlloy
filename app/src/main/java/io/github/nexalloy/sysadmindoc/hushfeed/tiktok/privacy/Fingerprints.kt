package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy

import io.github.nexalloy.morphe.AccessFlags
import io.github.nexalloy.morphe.Fingerprint
import io.github.nexalloy.morphe.StrictQuery
import io.github.nexalloy.morphe.fieldAccess
import io.github.nexalloy.morphe.findMethodDirect
import io.github.nexalloy.morphe.findMethodListDirect
import io.github.nexalloy.morphe.methodCall
import org.luckypray.dexkit.DexAccessFlags
import org.luckypray.dexkit.query.enums.StringMatchType

private const val APP_LOG_DESC = "Lcom/bytedance/applog/AppLog;"
private const val APPS_FLYER_DESC = "Lcom/appsflyer/AppsFlyerLib;"
private const val MONITOR_CRASH_DESC = "Lcom/bytedance/crash/MonitorCrash;"
private const val ORBU_SERVER_TYPE = "Lttp/orbu/sdk/configuration/model/ServerAPIType;"
private const val ORBU_SERVER_CONFIG = "Lttp/orbu/sdk/configuration/model/ServerConfig;"
private const val REQUEST_BODY = "Lcom/bytedance/im/core/proto/RequestBody;"
private const val REQUEST = "Lcom/bytedance/im/core/proto/Request;"

private val publicStatic = listOf(AccessFlags.PUBLIC, AccessFlags.STATIC)
private val publicFinal = listOf(AccessFlags.PUBLIC, AccessFlags.FINAL)

// --- Telemetry ---

object AppLogOnEventFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "onEvent",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

object AppLogOnEventV3StringFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "onEventV3",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = listOf("Ljava/lang/String;"),
)

object AppLogOnEventV3JsonFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "onEventV3",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lorg/json/JSONObject;"),
)

object AppLogOnEventV3BundleFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "onEventV3",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Landroid/os/Bundle;"),
)

object AppLogOnMiscEventFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "onMiscEvent",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Lorg/json/JSONObject;"),
)

object AppLogFlushFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "flush",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = emptyList(),
)

object AppLogFlushAsyncFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "flushAsync",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = emptyList(),
)

object AppLogOnActivityPauseFingerprint : Fingerprint(
    definingClass = APP_LOG_DESC,
    name = "onActivityPause",
    accessFlags = publicStatic,
    returnType = "V",
    parameters = emptyList(),
)

object InitAppsFlyerClassFingerprint : Fingerprint(
    name = "<clinit>",
    strings = listOf("XY8Lpakui8g4kBcposRgxA"),
)

object InitAppsFlyerRunFingerprint : Fingerprint(
    classFingerprint = InitAppsFlyerClassFingerprint,
    name = "run",
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

object FirebaseSetCurrentScreenFingerprint : Fingerprint(
    definingClass = "Lcom/google/firebase/analytics/FirebaseAnalytics;",
    name = "setCurrentScreen",
    accessFlags = publicFinal,
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "Ljava/lang/String;", "Ljava/lang/String;"),
)

object MonitorCrashReportCustomErrFingerprint : Fingerprint(
    definingClass = MONITOR_CRASH_DESC,
    name = "reportCustomErr",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Ljava/lang/Throwable;"),
)

object MonitorCrashReportEventFingerprint : Fingerprint(
    definingClass = MONITOR_CRASH_DESC,
    name = "reportEvent",
    accessFlags = listOf(AccessFlags.PUBLIC),
    returnType = "V",
    parameters = listOf("Ljava/lang/String;", "I", "Lorg/json/JSONObject;", "Lorg/json/JSONObject;"),
)

object NpthCoreInitTaskFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/NpthCoreInitTask;",
    name = "run",
    accessFlags = publicFinal,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

object NpthSecondInitTaskFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/legoImp/task/NpthSecondInitTask;",
    name = "run",
    accessFlags = publicFinal,
    returnType = "V",
    parameters = listOf("Landroid/content/Context;"),
)

object AppLogSendPackFingerprint : Fingerprint(
    returnType = "I",
    strings = listOf("ss_app_log", "applog_forward"),
    custom = {
        paramCount(10)
    },
)

object AppLogForwardSendFingerprint : Fingerprint(
    returnType = "V",
    strings = listOf("trySendForward start requestId={}, url={}"),
    custom = {
        paramCount(3)
    },
)

object AppLogPrioritySendFingerprint : Fingerprint(
    definingClass = "Lcom/bytedance/applog/priority/PriorityCallbackImpl;",
    name = "doHttpPost",
    returnType = "Lcom/bytedance/applog/priority/PriorityHttpResponse;",
    parameters = listOf("Ljava/lang/String;", "[B", "Lkotlin/Pair;"),
)

object InstallActiveCheckFingerprint : Fingerprint(
    returnType = "Z",
    strings = listOf("Register#active http error = "),
    custom = {
        paramCount(6)
    },
)

@StrictQuery
object OrbuLogSendFingerprint : Fingerprint(
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Lttp/orbu/sdk/logging/LogParams;", "L"),
    filters = listOf(
        methodCall(returnType = "Lttp/orbu/sdk/network/request/APIPutLogsDetailsBody;"),
    ),
)

object OrbuServerTypeBaseUrlFingerprint : Fingerprint(
    definingClass = ORBU_SERVER_TYPE,
    name = "getBaseUrl",
    returnType = "Ljava/lang/String;",
    parameters = emptyList(),
)

val orbuServerConfigInits = findMethodListDirect {
    findMethod {
        matcher {
            declaredClass("ttp.orbu.sdk.configuration.model.ServerConfig")
            name = "<init>"
            paramCount(min = 2)
        }
    }
}

// --- CAPTCHA Popups ---

object CaptchaPopupFingerprint : Fingerprint(
    name = "popCaptchaV2",
    returnType = "V",
    strings = listOf("popCaptchaV2 - riskInfo = "),
) {
    init {
        classMatcher { className("SecApiImpl", StringMatchType.EndsWith) }
    }
}

object LegacyCaptchaPopupFingerprint : Fingerprint(
    name = "popCaptcha",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "I", "L"),
    strings = listOf("popCaptcha - errorcode = "),
) {
    init {
        classMatcher { className("SecApiImpl", StringMatchType.EndsWith) }
    }
}

object LiveHostCaptchaPopupFingerprint : Fingerprint(
    name = "popCaptchaV2",
    returnType = "V",
    parameters = listOf(
        "Landroid/app/Activity;",
        "Ljava/lang/String;",
        "L",
        "Landroidx/fragment/app/Fragment;",
    ),
) {
    init {
        classMatcher { className("LiveHostUser", StringMatchType.EndsWith) }
    }
}

object BdTuringCaptchaPopupFingerprint : Fingerprint(
    definingClass = "Lcom/tts/oecverify/BdTuring;",
    name = "showVerifyDialog",
    returnType = "V",
    parameters = listOf("Landroid/app/Activity;", "L", "Lcom/tts/oecverify/BdTuringCallback;"),
)

// --- Ghost Mode ---

val storyViewReporters = findMethodListDirect {
    findClass {
        matcher { className("StoryApi", StringMatchType.EndsWith) }
    }.findMethod {
        matcher {
            returnType = "void"
        }
    }.filter { it.name in setOf("reportStoryViewed", "reportUserInteraction", "reportStoryReveal") }
}

val profileViewReporters = findMethodListDirect {
    findClass {
        matcher { className("ProfileViewerApiService", StringMatchType.EndsWith) }
    }.findMethod {
        matcher {
            name = "reportView"
            returnType = "void"
        }
    }
}

val typingStatusReporters = findMethodListDirect {
    findClass {
        matcher { className("TypingStatusSenderTimer", StringMatchType.EndsWith) }
    }.findMethod {
        matcher {
            returnType = "void"
            paramTypes("java.lang.String")
        }
    }
}

object AwemeStatsSenderFingerprint : Fingerprint(
    isStatic = true,
    returnType = "V",
    parameters = listOf(
        "Ljava/lang/String;", "I", "Ljava/lang/String;", "I",
        "Lcom/ss/android/ugc/aweme/feed/model/Aweme;", "Ljava/lang/String;",
        "Lkotlin/jvm/functions/Function1;",
    ),
    strings = listOf("aweme_stats_monitor"),
)

object OnlineStatusRegularReportFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = publicFinal,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("ActivityStatusReporter@261e.doReport\$1"),
)

object OnlineStatusIrregularReportFingerprint : Fingerprint(
    name = "invokeSuspend",
    accessFlags = publicFinal,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("Ljava/lang/Object;"),
    strings = listOf("ActivityStatusReporter@261e.doReport\$2"),
)

// --- Hide Read Receipts ---

val chatRequestSendMethods = findMethodListDirect {
    findMethod {
        matcher {
            returnType = "long"
            paramTypes(listOf("int", "com.bytedance.im.core.proto.RequestBody", null, "java.lang.Object[]"))
        }
    }.filter { (it.accessFlags and DexAccessFlags.FINAL) == 0 }
}

@StrictQuery
object ChatRequestSendWithHeadersFingerprint : Fingerprint(
    returnType = "V",
    parameters = listOf("I", REQUEST_BODY, "L", "Ljava/util/Map;", "[Ljava/lang/Object;"),
    filters = listOf(fieldAccess(definingClass = REQUEST, name = "headers")),
)

// --- Watch History & Search History (HushFeed Privacy Pack) ---

object ViewReportFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/api/AwemeStatsApi;",
    isStatic = true,
    returnType = "V",
    strings = listOf("basic_vv_batch_size", "homepage_hot"),
    custom = {
        paramCount(1)
    },
)

object ViewReportFlushFingerprint : Fingerprint(
    definingClass = "Lcom/ss/android/ugc/aweme/feed/api/AwemeStatsApi;",
    isStatic = true,
    returnType = "V",
    parameters = listOf("Ljava/util/List;"),
    strings = listOf("first_install_time"),
)

object SearchHistoryRecordFingerprint : Fingerprint(
    isStatic = false,
    returnType = "V",
    parameters = listOf("Lcom/ss/android/ugc/aweme/search/model/SearchHistory;", "Ljava/lang/String;"),
    strings = listOf("recordSearchHistory, repo:"),
)

// --- Keep Unsent Messages (HushFeed) ---

object RecallApplyFingerprint : Fingerprint(
    parameters = listOf("Ljava/lang/String;", "Ljava/lang/String;", "Lcom/bytedance/im/core/proto/MessageBody;", "I"),
    strings = listOf(
        "RecallMessageUpdater",
        "No server message id is found.",
        "msg to recalled is not found: ",
    ),
)

val recallAppliedResultInit = findMethodDirect {
    RecallApplyFingerprint().invokes
        .filter { it.name == "<init>" && it.paramTypeNames == listOf("java.lang.Integer") }
        .last()
}

val messageContentGetter = findMethodDirect {
    findMethod {
        matcher {
            name = "getContent"
            returnType = "java.lang.String"
            paramCount(0)
        }
    }.single { method ->
        val cls = method.declaredClass ?: return@single false
        cls.methods.any { it.name == "getContentPB" } &&
            cls.methods.any { it.name == "getLocalExtValue" } &&
            cls.methods.any { it.name == "getMsgId" && it.returnTypeName == "long" }
    }
}

