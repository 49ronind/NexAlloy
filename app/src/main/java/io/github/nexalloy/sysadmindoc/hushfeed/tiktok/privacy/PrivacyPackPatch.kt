package io.github.nexalloy.sysadmindoc.hushfeed.tiktok.privacy

import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Intent
import android.database.MatrixCursor
import android.location.LocationManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.provider.ContactsContract
import android.view.View
import android.webkit.WebView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import io.github.nexalloy.patch
import org.json.JSONObject
import java.lang.ref.WeakReference
import java.net.NetworkInterface
import java.util.Collections
import java.util.Enumeration
import java.util.Locale
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap

private val CONTACT_ALL_COLUMNS = arrayOf(
    "_id", "_count", "lookup", "display_name", "display_name_alt", "display_name_source",
    "phonetic_name", "phonetic_name_style", "sort_key", "sort_key_alt", "name_raw_contact_id",
    "photo_id", "photo_file_id", "photo_uri", "photo_thumb_uri", "in_visible_group",
    "in_default_directory", "is_user_profile", "has_phone_number",
    "contact_last_updated_timestamp", "times_contacted", "last_time_contacted", "starred",
    "pinned", "custom_ringtone", "send_to_voicemail", "contact_presence",
    "contact_chat_capability", "contact_status", "contact_status_ts",
    "contact_status_res_package", "contact_status_label", "contact_status_icon",
    "contact_id", "raw_contact_id", "aggregation_mode", "deleted", "raw_contact_is_read_only",
    "raw_contact_is_user_profile", "account_name", "account_type", "account_type_and_data_set",
    "data_set", "sourceid", "version", "dirty", "sync1", "sync2", "sync3", "sync4",
    "mimetype", "is_primary", "is_super_primary", "is_read_only", "data_version",
    "data1", "data2", "data3", "data4", "data5", "data6", "data7", "data8", "data9", "data10",
    "data11", "data12", "data13", "data14", "data15",
    "data_sync1", "data_sync2", "data_sync3", "data_sync4", "carrier_presence",
    "preferred_phone_account_component_name", "preferred_phone_account_id",
    "mode", "chat_capability", "status", "status_ts", "status_res_package", "status_label",
    "status_icon",
    "number", "type", "label", "normalized_number", "data_id",
    "title", "notes", "system_id", "group_visible", "should_sync", "auto_add", "favorites",
    "group_is_read_only", "summ_count", "summ_phones",
    "_display_name", "_size",
)

val BlockContactListAccess = patch(
    name = "Block contact list access",
    description = "Returns an empty contact cursor whenever TikTok queries Android ContactsContract (HushFeed).",
    use = false,
) {
    runCatching {
        XposedBridge.hookAllMethods(
            ContentResolver::class.java,
            "query",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val uri = param.args.getOrNull(0) as? Uri ?: return
                    if (uri.authority != ContactsContract.AUTHORITY) return
                    @Suppress("UNCHECKED_CAST")
                    val projection = param.args.getOrNull(1) as? Array<String>
                    param.result = MatrixCursor(projection ?: CONTACT_ALL_COLUMNS.clone())
                }
            },
        )
    }
}

private fun isAppInventoryScan(intent: Intent?): Boolean {
    return intent != null &&
        intent.action == Intent.ACTION_MAIN &&
        intent.hasCategory(Intent.CATEGORY_LAUNCHER) &&
        intent.`package` == null &&
        intent.component == null
}

val BlockInstalledAppScanning = patch(
    name = "Block installed app scanning",
    description = "Returns an empty list when TikTok enumerates installed apps while keeping Share and external app intents working (HushFeed).",
    use = false,
) {
    runCatching {
        val pmClass = appContext.packageManager.javaClass
        val emptyListHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = ArrayList<Any>()
            }
        }
        XposedBridge.hookAllMethods(pmClass, "getInstalledPackages", emptyListHook)
        XposedBridge.hookAllMethods(pmClass, "getInstalledApplications", emptyListHook)
        XposedBridge.hookAllMethods(
            pmClass,
            "queryIntentActivities",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args.getOrNull(0) as? Intent ?: return
                    if (isAppInventoryScan(intent)) {
                        param.result = ArrayList<Any>()
                    }
                }
            },
        )
    }
}

val LocationAccessGovernor = patch(
    name = "Location access governor",
    description = "Denies TikTok access to Android LocationManager GPS/network location queries and updates (HushFeed).",
    use = false,
) {
    runCatching {
        val lm = LocationManager::class.java
        val nullHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = null
            }
        }
        XposedBridge.hookAllMethods(lm, "getLastKnownLocation", nullHook)
        XposedBridge.hookAllMethods(lm, "requestSingleUpdate", nullHook)
        XposedBridge.hookAllMethods(lm, "requestLocationUpdates", nullHook)
    }
}

val BlockClipboardReads = patch(
    name = "Block clipboard reads",
    description = "Stops TikTok from silently reading your Android clipboard (HushFeed).",
    use = false,
) {
    runCatching {
        val cm = ClipboardManager::class.java
        XposedBridge.hookAllMethods(
            cm,
            "getPrimaryClip",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            },
        )
        XposedBridge.hookAllMethods(
            cm,
            "getText",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = ""
                }
            },
        )
        XposedBridge.hookAllMethods(
            cm,
            "hasPrimaryClip",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = false
                }
            },
        )
    }
}

private const val BLANK_ADVERTISING_ID = "00000000-0000-0000-0000-000000000000"

val BlockAdvertisingId = patch(
    name = "Block advertising ID",
    description = "Hands TikTok a blank zeroed Advertising ID and marks ad tracking as limited (HushFeed).",
    use = false,
) {
    runCatching {
        val infoClass = XposedHelpers.findClassIfExists(
            "com.google.android.gms.ads.identifier.AdvertisingIdClient\$Info",
            classLoader,
        ) ?: return@runCatching

        // Overwrite fields on construction so even R8-inlined field reads see blank ID & limited=true.
        XposedBridge.hookAllConstructors(
            infoClass,
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val instance = param.thisObject ?: return
                    for (field in infoClass.declaredFields) {
                        if (java.lang.reflect.Modifier.isStatic(field.modifiers)) continue
                        runCatching {
                            field.isAccessible = true
                            when (field.type) {
                                String::class.java -> field.set(instance, BLANK_ADVERTISING_ID)
                                Boolean::class.javaPrimitiveType, Boolean::class.javaObjectType -> field.set(instance, true)
                            }
                        }
                    }
                }
            },
        )
        XposedBridge.hookAllMethods(
            infoClass,
            "getId",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = BLANK_ADVERTISING_ID
                }
            },
        )
        XposedBridge.hookAllMethods(
            infoClass,
            "isLimitAdTrackingEnabled",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = true
                }
            },
        )
    }
}

private fun isVpnInterfaceName(name: String?): Boolean {
    if (name == null) return false
    return name.startsWith("tun") || name.startsWith("ppp") ||
        name.startsWith("ipsec") || name.startsWith("wg")
}

val HideVpnConnection = patch(
    name = "Hide VPN connection",
    description = "Hides VPN transport flags and tunnel network interfaces (tun/ppp/ipsec/wg) from TikTok (HushFeed).",
    use = false,
) {
    runCatching {
        XposedBridge.hookAllMethods(
            NetworkCapabilities::class.java,
            "hasTransport",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val transport = param.args.getOrNull(0) as? Int ?: return
                    if (transport == NetworkCapabilities.TRANSPORT_VPN) {
                        param.result = false
                    }
                }
            },
        )
    }
    runCatching {
        XposedBridge.hookAllMethods(
            NetworkInterface::class.java,
            "getNetworkInterfaces",
            object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    @Suppress("UNCHECKED_CAST")
                    val all = param.result as? Enumeration<NetworkInterface> ?: return
                    val kept = ArrayList<NetworkInterface>()
                    while (all.hasMoreElements()) {
                        val iface = all.nextElement()
                        if (!isVpnInterfaceName(iface.name)) {
                            kept.add(iface)
                        }
                    }
                    param.result = Collections.enumeration(kept)
                }
            },
        )
    }
}

val StopWatchHistory = patch(
    name = "Stop recording watch history",
    description = "Blocks TikTok's /aweme/v1/aweme/stats/ single and batch view reports so watched videos stay out of Watch history (HushFeed).",
    use = false,
) {
    runCatching {
        ViewReportFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
    runCatching {
        ViewReportFlushFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
}

val StopSearchHistory = patch(
    name = "Stop saving search history",
    description = "Stops TikTok from saving new search queries into local search history (HushFeed).",
    use = false,
) {
    runCatching {
        SearchHistoryRecordFingerprint.hookMethod {
            before { param ->
                param.result = null
            }
        }
    }
}

// --- In-App Browser Privacy Guard (HushFeed BrowserPrivacyGuard) ---

private object WebViewBridgeGuard {
    private val FIRST_PARTY_DOMAINS = arrayOf(
        "tiktok.com", "tiktokv.com", "tiktokv.us", "tiktokv.eu",
        "pipopayment.com", "pipopayment.us",
    )
    private val STATIC_CDN_DOMAINS = arrayOf(
        "tiktokcdn.com", "tiktokcdn-us.com", "tiktokcdn-eu.com",
    )
    private const val STATIC_FRONTEND = "/obj/static-tx/oec-captcha-ttweb/"
    private val HOST_PATTERN = Regex("[a-z0-9.-]+")
    private val PATH_PATTERN = Regex("[A-Za-z0-9._~/-]+")

    private val states = WeakHashMap<WebView, WeakReference<BridgeState>>()
    val internalCall: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }

    private fun sameOrSubdomain(host: String, domain: String): Boolean =
        host == domain || host.endsWith(".$domain")

    private fun isStaticFrontend(path: String?): Boolean {
        if (path == null || !path.startsWith(STATIC_FRONTEND) || path.contains("//")) return false
        if (!PATH_PATTERN.matches(path)) return false
        return path.split('/').none { it == "." || it == ".." }
    }

    fun isTrustedAppPage(url: String?): Boolean {
        if (url == null) return false
        val parsed = runCatching { Uri.parse(url) }.getOrNull() ?: return false
        if (!"https".equals(parsed.scheme, ignoreCase = true)) return false
        if (parsed.encodedUserInfo != null) return false
        var host = parsed.host?.lowercase(Locale.ROOT) ?: return false
        while (host.endsWith('.')) host = host.dropLast(1)
        if (host.isEmpty() || !HOST_PATTERN.matches(host)) return false

        if (host == "eu.pipopay.com") return true
        if (FIRST_PARTY_DOMAINS.any { sameOrSubdomain(host, it) }) return true
        if (STATIC_CDN_DOMAINS.any { sameOrSubdomain(host, it) }) {
            return isStaticFrontend(parsed.encodedPath)
        }
        return false
    }

    private fun isScriptUrl(url: String?): Boolean {
        if (url == null) return false
        val colon = url.indexOf(':')
        return colon > 0 && url.substring(0, colon).equals("javascript", ignoreCase = true)
    }

    fun stateFor(webView: WebView): BridgeState {
        synchronized(states) {
            val existing = states[webView]?.get()
            if (existing != null) return existing
            val created = BridgeState()
            webView.addOnAttachStateChangeListener(created)
            states[webView] = WeakReference(created)
            return created
        }
    }

    fun prepareForPage(webView: WebView, url: String?) {
        if (isScriptUrl(url)) return
        stateFor(webView).setBridgesAllowed(webView, isTrustedAppPage(url))
    }

    fun prepareForHistoryStep(webView: WebView, steps: Int) {
        if (steps == 0) return
        val history = runCatching { webView.copyBackForwardList() }.getOrNull() ?: return
        val target = history.currentIndex + steps
        if (target < 0 || target >= history.size) return
        prepareForPage(webView, history.getItemAtIndex(target)?.url)
    }

    class BridgeState : View.OnAttachStateChangeListener {
        private val interfaces = LinkedHashMap<String, Any>()
        private var bridgesAllowed = true

        @Synchronized
        fun bridgesAllowed(): Boolean = bridgesAllowed

        @Synchronized
        fun record(name: String, obj: Any) {
            interfaces[name] = obj
        }

        @Synchronized
        fun setBridgesAllowed(webView: WebView, allowed: Boolean) {
            if (bridgesAllowed == allowed) return
            bridgesAllowed = allowed
            internalCall.set(true)
            try {
                for ((name, obj) in interfaces) {
                    if (allowed) {
                        webView.addJavascriptInterface(obj, name)
                    } else {
                        webView.removeJavascriptInterface(name)
                    }
                }
            } finally {
                internalCall.set(false)
            }
        }

        override fun onViewAttachedToWindow(v: View) {}
        override fun onViewDetachedFromWindow(v: View) {}
    }
}

val BrowserPrivacyGuard = patch(
    name = "In-app browser privacy guard",
    description = "Withholds TikTok JavaScript bridges on third-party websites opened inside TikTok's WebView while keeping TikTok's own pages working (HushFeed).",
    use = false,
) {
    runCatching {
        val wv = WebView::class.java
        XposedBridge.hookAllMethods(
            wv,
            "addJavascriptInterface",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    if (WebViewBridgeGuard.internalCall.get() == true) return
                    val webView = param.thisObject as? WebView ?: return
                    val obj = param.args.getOrNull(0) ?: return
                    val name = param.args.getOrNull(1) as? String ?: return
                    val state = WebViewBridgeGuard.stateFor(webView)
                    val currentUrl = runCatching { webView.url }.getOrNull()
                    val allowed = if (currentUrl == null) {
                        state.bridgesAllowed()
                    } else {
                        WebViewBridgeGuard.isTrustedAppPage(currentUrl)
                    }
                    state.record(name, obj)
                    state.setBridgesAllowed(webView, allowed)
                    if (!allowed) {
                        param.result = null
                    }
                }
            },
        )

        val loadUrlHook = object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val webView = param.thisObject as? WebView ?: return
                val url = param.args.getOrNull(0) as? String
                WebViewBridgeGuard.prepareForPage(webView, url)
            }
        }
        XposedBridge.hookAllMethods(wv, "loadUrl", loadUrlHook)
        XposedBridge.hookAllMethods(wv, "postUrl", loadUrlHook)
        XposedBridge.hookAllMethods(wv, "loadDataWithBaseURL", loadUrlHook)
        XposedBridge.hookAllMethods(
            wv,
            "loadData",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val webView = param.thisObject as? WebView ?: return
                    WebViewBridgeGuard.prepareForPage(webView, null)
                }
            },
        )
        XposedBridge.hookAllMethods(
            wv,
            "reload",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val webView = param.thisObject as? WebView ?: return
                    WebViewBridgeGuard.prepareForPage(webView, runCatching { webView.url }.getOrNull())
                }
            },
        )
        XposedBridge.hookAllMethods(
            wv,
            "goBack",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val webView = param.thisObject as? WebView ?: return
                    WebViewBridgeGuard.prepareForHistoryStep(webView, -1)
                }
            },
        )
        XposedBridge.hookAllMethods(
            wv,
            "goForward",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val webView = param.thisObject as? WebView ?: return
                    WebViewBridgeGuard.prepareForHistoryStep(webView, 1)
                }
            },
        )
        XposedBridge.hookAllMethods(
            wv,
            "goBackOrForward",
            object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val webView = param.thisObject as? WebView ?: return
                    val steps = param.args.getOrNull(0) as? Int ?: return
                    WebViewBridgeGuard.prepareForHistoryStep(webView, steps)
                }
            },
        )
    }
}

// --- Keep Unsent Messages (HushFeed KeepUnsent) ---

private object KeepUnsentState {
    private const val UNSENT_TAG = "  (unsent)"
    val keptServerIds: MutableSet<Long> = Collections.newSetFromMap(ConcurrentHashMap())

    fun signedInUserId(classLoader: ClassLoader): String? = runCatching {
        val managerCls = XposedHelpers.findClass(
            "com.ss.android.ugc.aweme.framework.services.ServiceManager",
            classLoader,
        )
        val accountCls = XposedHelpers.findClass(
            "com.ss.android.ugc.aweme.IAccountUserService",
            classLoader,
        )
        val manager = XposedHelpers.callStaticMethod(managerCls, "get") ?: return@runCatching null
        val service = XposedHelpers.callMethod(manager, "getService", accountCls) ?: return@runCatching null
        if (XposedHelpers.callMethod(service, "isLogin") != true) return@runCatching null
        (XposedHelpers.callMethod(service, "getCurUserId") as? String)?.takeIf { it.isNotEmpty() }
    }.getOrNull()

    fun markContent(msgId: Long, content: String): String {
        if (!keptServerIds.contains(msgId)) return content
        return runCatching {
            val json = JSONObject(content)
            if (!json.has("text")) return@runCatching content
            val text = json.optString("text", "")
            if (text.endsWith(UNSENT_TAG)) return@runCatching content
            json.put("text", text + UNSENT_TAG).toString()
        }.getOrDefault(content)
    }
}

val KeepUnsentMessages = patch(
    name = "Keep unsent messages",
    description = "Keeps chat messages unsent by the other person and marks them as (unsent), while still allowing you to unsend your own (HushFeed).",
    use = false,
) {
    runCatching {
        val appliedCtor = ::recallAppliedResultInit.constructor.apply { isAccessible = true }
        RecallApplyFingerprint.hookMethod {
            before { param ->
                val serverId = param.args.getOrNull(0) as? String
                val recallUid = param.args.getOrNull(1) as? String
                if (serverId.isNullOrEmpty() || recallUid.isNullOrEmpty()) return@before
                val selfUid = KeepUnsentState.signedInUserId(classLoader)
                if (selfUid == null || selfUid == recallUid) return@before

                serverId.toLongOrNull()?.let { KeepUnsentState.keptServerIds.add(it) }
                val appliedResult = runCatching { appliedCtor.newInstance(null as Any?) }.getOrNull() ?: return@before
                param.result = appliedResult
            }
        }
    }

    runCatching {
        ::messageContentGetter.hookMethod {
            after { param ->
                if (KeepUnsentState.keptServerIds.isEmpty()) return@after
                val content = param.result as? String ?: return@after
                val msg = param.thisObject ?: return@after
                val msgId = runCatching {
                    (XposedHelpers.callMethod(msg, "getMsgId") as? Number)?.toLong()
                }.getOrNull() ?: return@after
                val marked = KeepUnsentState.markContent(msgId, content)
                if (marked !== content) {
                    param.result = marked
                }
            }
        }
    }
}
