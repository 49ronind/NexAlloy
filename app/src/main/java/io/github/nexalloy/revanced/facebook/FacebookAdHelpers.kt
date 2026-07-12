package io.github.nexalloy.revanced.facebook

import android.app.Activity
import android.app.Instrumentation
import android.content.ContextWrapper
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.widget.TextView
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import org.json.JSONObject
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import java.util.Collections
import java.util.IdentityHashMap
import java.util.LinkedHashMap
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

// ─── Constants ───────────────────────────────────────────────────────────────

const val FB_TAG = "NexAlloy/Facebook"
private const val BEFORE_SIZE_EXTRA = "nexalloy_fb_ads_before_size"
private const val GAME_AD_SUCCESS_INSTANCE_PREFIX = "nexalloy_fb_noop_ad"
private const val HOOK_HIT_LOG_EVERY = 25
private const val GAME_AD_RECENT_WINDOW_MS    = 30_000L
private const val GAME_AD_PROMISE_WINDOW_MS   = 10 * 60_000L
private const val AUDIENCE_NETWORK_REWARD_CLOSE_RETRY_WINDOW_MS = 35_000L

const val GRAPHQL_FEED_UNIT_EDGE_CLASS       = "com.facebook.graphql.model.GraphQLFeedUnitEdge"
const val GRAPHQL_MULTI_ADS_FEED_UNIT_CLASS  = "com.facebook.graphql.model.GraphQLFBMultiAdsFeedUnit"
const val GRAPHQL_QUICK_PROMO_FEED_UNIT_CLASS =
    "com.facebook.graphql.model.GraphQLQuickPromotionNativeTemplateFeedUnit"
const val AUDIENCE_NETWORK_ACTIVITY_CLASS        = "com.facebook.ads.AudienceNetworkActivity"
const val AUDIENCE_NETWORK_REMOTE_ACTIVITY_CLASS = "com.facebook.ads.internal.ipc.AudienceNetworkRemoteActivity"
const val NEKO_PLAYABLE_ACTIVITY_CLASS           = "com.facebook.neko.playables.activity.NekoPlayableAdActivity"

const val GAME_AD_REJECTION_MESSAGE   = "Game ad request blocked"
const val GAME_AD_REJECTION_CODE      = "CLIENT_UNSUPPORTED_OPERATION"
const val GAME_AD_UNAVAILABLE_MESSAGE = "Rewarded ad unavailable"
const val GAME_AD_UNAVAILABLE_CODE    = "ADS_UNAVAILABLE"

val GAME_AD_MESSAGE_TYPES = setOf(
    "getinterstitialadasync", "getrewardedvideoasync", "getrewardedinterstitialasync",
    "loadadasync", "showadasync", "loadbanneradasync", "hidebanneradasync"
)

/** Only these types are auto-fixed (banner/hide); rewarded/interstitial get ADS_UNAVAILABLE. */
val GAME_AD_AUTOFIX_MESSAGE_TYPES = setOf("loadbanneradasync", "hidebanneradasync")

val GAME_AD_UNAVAILABLE_MESSAGE_TYPES = setOf("getrewardedvideoasync", "getrewardedinterstitialasync")

val GAME_AD_ACTIVITY_CLASS_NAMES = setOf(
    AUDIENCE_NETWORK_ACTIVITY_CLASS,
    AUDIENCE_NETWORK_REMOTE_ACTIVITY_CLASS,
    NEKO_PLAYABLE_ACTIVITY_CLASS
)

val HARD_BLOCKED_GAME_AD_ACTIVITY_CLASS_NAMES = setOf(NEKO_PLAYABLE_ACTIVITY_CLASS)

val AUDIENCE_NETWORK_REWARD_COMPLETION_METHOD_NAMES = setOf(
    "onRewardedVideoCompleted", "onRewardedAdCompleted", "onRewardedInterstitialCompleted",
    "onAdComplete", "onAdCompleted"
)

val AUDIENCE_NETWORK_CLOSE_LISTENER_CLASS_NAMES = setOf("X.mGv", "X.mGo", "p000X.mGv", "p000X.mGo")

val FEED_AD_CATEGORY_VALUES          = setOf("SPONSORED", "PROMOTION", "AD", "ADVERTISEMENT", "BANNER", "ENGAGEMENT_QP")
val FEED_SAFE_CONTAINER_CATEGORY_VALUES = setOf("FB_SHORTS", "MULTI_FB_STORIES_TRAY")

// Upstream gate: the BROAD feed/reel-CTA text-marker fallbacks below are disabled by
// default in FacebookAppAdsRemover (higher false-positive risk than the explicit-card
// detector). The explicit "Hide ad" + AdChoices card detector always stays on.
const val ENABLE_FEED_UI_MARKER_FALLBACKS = false

val FEED_SURFACE_AD_MARKER_TOKENS = listOf(
    "hide ad", "ad\u2022", "sponsored", "promoted", "ad choices", "adchoices"
)

val EXPLICIT_FEED_CARD_AD_MARKER_TOKENS = listOf(
    "hide ad", "ad\u2022", "ad choices", "adchoices"
)

val EXPLICIT_FEED_AD_CTA_TOKENS = listOf(
    "apply now", "send message", "learn more", "shop now", "contact us",
    "get quote", "book now", "call now", "sign up", "download"
)

val FEED_REEL_CTA_AD_MARKER_TOKENS = listOf(
    "shared link:", "send message", "your business", "your ad"
)

val FEED_AD_SIGNAL_TOKENS = listOf(
    "sponsored", "promotion", "multiads", "quickpromotion",
    "reels_banner_ad", "reelsbannerads", "reels_post_loop_deferred_card", "deferred_card",
    "adbreakdeferredcta", "instreamadidlewithbannerstate", "instream_legacy_banner_ad",
    "unified_player_banner_ad", "banner_ad_", "floatingcta"
)

val REELS_AD_SIGNAL_TOKENS = listOf(
    "sponsored", "promotion", "multiads", "quickpromotion",
    "reels_banner_ad", "reelsbannerads", "adbreakdeferredcta",
    "instreamadidlewithbannerstate", "instream_legacy_banner_ad",
    "unified_player_banner_ad", "banner_ad_"
)

val GAME_AD_METHOD_TAGS = listOf(
    "Invalid JSON content received by onGetInterstitialAdAsync: ",
    "Invalid JSON content received by onGetRewardedInterstitialAsync: ",
    "Invalid JSON content received by onRewardedVideoAsync: ",
    "Invalid JSON content received by onLoadAdAsync: ",
    "Invalid JSON content received by onShowAdAsync: "
)

// ─── Shared state ─────────────────────────────────────────────────────────────

val gameAdInstanceIds    = ConcurrentHashMap<String, String>()
val gameAdInstanceTypes  = ConcurrentHashMap<String, String>()
val gameAdPromiseSnapshots = ConcurrentHashMap<String, GameAdPromiseSnapshot>()
val recentGameAdTargets  = Collections.synchronizedMap(WeakHashMap<Any, Long>())
val recentGameAdPayloads = Collections.synchronizedList(ArrayList<GameAdPayloadSnapshot>())
val hookHitCounters      = ConcurrentHashMap<String, AtomicInteger>()
private val gameAdResultHooksInstalled         = AtomicInteger(0)
private val gameAdServiceDispatchHooksInstalled = AtomicInteger(0)
private val gameAdSurfaceHooksInstalled        = AtomicInteger(0)
private val audienceNetworkRewardHooksInstalled = AtomicInteger(0)
private val audienceNetworkRewardClassesHooked  = Collections.newSetFromMap(ConcurrentHashMap<String, Boolean>())
private val audienceNetworkRewardAdListeners    = Collections.synchronizedMap(WeakHashMap<Any, Any>())
private val scheduledGameAdActivityCloses       = Collections.synchronizedMap(WeakHashMap<Activity, Long>())
private val scheduledAudienceNetworkExitViews   = Collections.synchronizedMap(WeakHashMap<View, Long>())
private val lastGameAdActivityCloseMs    = AtomicLong(0L)
private val lastUnavailableGameAdMs      = AtomicLong(0L)
private val marketplaceAdsPackCache      = ConcurrentHashMap<String, Boolean>()

// ─── Data classes ─────────────────────────────────────────────────────────────

data class FeedListSanitizerHook(val method: Method, val listArgIndex: Int)
data class FeedCsrFilterHook(val method: Method, val listArgIndex: Int)

data class StoryAdProviderHooks(
    val providerClass: Class<*>,
    val mergeMethod: Method?,
    val fetchMoreAdsMethod: Method?,
    val deferredUpdateMethod: Method?,
    val insertionTriggerMethod: Method?
)

data class GameAdPayloadSnapshot(
    val target: Any,
    val payload: JSONObject,
    val messageType: String?,
    val timestampMs: Long
)

data class GameAdPromiseSnapshot(
    val payload: JSONObject,
    val messageType: String?,
    val timestampMs: Long
)

// ─── AdStoryInspector ─────────────────────────────────────────────────────────

class AdStoryInspector(private val adKindEnumClass: Class<*>) {
    private val enumMethodCache = ConcurrentHashMap<Class<*>, List<Method>>()
    private val fieldCache      = ConcurrentHashMap<Class<*>, List<Field>>()

    fun containsAdStory(
        value: Any?, depth: Int = 0, seen: IdentityHashMap<Any, Boolean> = IdentityHashMap()
    ): Boolean = containsAdKind(value, depth, seen) && containsReelsAdSignal(value, 0, IdentityHashMap())

    private fun containsAdKind(value: Any?, depth: Int, seen: IdentityHashMap<Any, Boolean>): Boolean {
        if (value == null || depth > 4) return false
        if (isAdKind(value)) return true
        val type = value.javaClass
        if (type.isPrimitive || value is String || value is Number || value is Boolean || value is CharSequence) return false
        if (seen.put(value, true) != null) return false
        if (value is Iterable<*>) { var n = 0; for (i in value) { if (containsAdKind(i, depth+1, seen)) return true; if (++n >= 8) break } }
        if (type.isArray) { val a = value as? Array<*>; if (a != null) { var n = 0; for (i in a) { if (containsAdKind(i, depth+1, seen)) return true; if (++n >= 8) break } } }
        for (m in enumMethodsFor(type)) if (isAdKind(runCatching { m.invoke(value) }.getOrNull())) return true
        for (f in fieldsFor(type)) if (containsAdKind(runCatching { f.get(value) }.getOrNull(), depth+1, seen)) return true
        return false
    }

    private fun containsReelsAdSignal(value: Any?, depth: Int, seen: IdentityHashMap<Any, Boolean>): Boolean {
        if (value == null || depth > 4) return false
        if (value is CharSequence) return isReelsAdSignalText(value.toString())
        val type = value.javaClass
        if (isReelsAdSignalText(type.name)) return true
        if (type.isEnum) return isReelsAdSignalText(value.toString())
        if (type.isPrimitive || value is Number || value is Boolean) return false
        if (seen.put(value, true) != null) return false
        if (value is Iterable<*>) { var n = 0; for (i in value) { if (containsReelsAdSignal(i, depth+1, seen)) return true; if (++n >= 8) break } }
        if (type.isArray) { val a = value as? Array<*>; if (a != null) { var n = 0; for (i in a) { if (containsReelsAdSignal(i, depth+1, seen)) return true; if (++n >= 8) break } } }
        if (isReelsAdSignalText(runCatching { value.toString() }.getOrNull())) return true
        for (m in stringMethodsFor(type)) if (isReelsAdSignalText(runCatching { m.invoke(value) as? String }.getOrNull())) return true
        for (f in fieldsFor(type)) if (containsReelsAdSignal(runCatching { f.get(value) }.getOrNull(), depth+1, seen)) return true
        return false
    }

    private fun isAdKind(v: Any?) = v != null && v.javaClass == adKindEnumClass && v.toString() == "AD"

    private fun enumMethodsFor(type: Class<*>) = enumMethodCache.getOrPut(type) {
        val map = LinkedHashMap<String, Method>()
        var cur: Class<*>? = type
        while (cur != null && cur != Any::class.java) {
            cur.declaredMethods.forEach { m ->
                if (!Modifier.isStatic(m.modifiers) && m.parameterCount == 0 && m.returnType == adKindEnumClass) {
                    m.isAccessible = true; map.putIfAbsent("${cur.name}#${m.name}", m)
                }
            }; cur = cur.superclass
        }; map.values.toList()
    }

    private fun fieldsFor(type: Class<*>) = fieldCache.getOrPut(type) {
        val list = ArrayList<Field>(); var cur: Class<*>? = type
        while (cur != null && cur != Any::class.java && list.size < 24) {
            cur.declaredFields.forEach { f -> if (!Modifier.isStatic(f.modifiers) && list.size < 24) { f.isAccessible = true; list.add(f) } }; cur = cur.superclass
        }; list
    }

    private fun stringMethodsFor(type: Class<*>) = allMethodsFor(type).asSequence()
        .filter { m -> m.parameterCount == 0 && m.returnType == String::class.java && m.name != "toString" }
        .take(12).onEach { it.isAccessible = true }.toList()

    private fun allMethodsFor(type: Class<*>): List<Method> {
        val map = LinkedHashMap<String, Method>(); var cur: Class<*>? = type
        while (cur != null && cur != Any::class.java) {
            cur.declaredMethods.forEach { m -> if (!Modifier.isStatic(m.modifiers)) { m.isAccessible = true; map.putIfAbsent("${cur.name}#${m.name}/${m.parameterCount}", m) } }; cur = cur.superclass
        }; return map.values.toList()
    }

    private fun isReelsAdSignalText(v: String?): Boolean {
        if (v.isNullOrBlank()) return false
        val n = v.lowercase(); return REELS_AD_SIGNAL_TOKENS.any { n.contains(it) }
    }
}

// ─── FeedItemInspector ────────────────────────────────────────────────────────

class FeedItemInspector(itemContractTypes: Collection<Class<*>>) {
    private val itemModelAccessor =
        resolveItemContractAccessor(itemContractTypes, "B2r") ?: resolveItemModelAccessor(itemContractTypes)
    private val itemEdgeAccessor =
        resolveItemContractAccessor(itemContractTypes, "BG7") ?: resolveItemEdgeAccessor(itemContractTypes)
    private val itemNetworkAccessor =
        resolveItemContractAccessor(itemContractTypes, "ArH") ?: resolveItemNetworkAccessor(itemContractTypes)
    private val categoryMethodCache       = ConcurrentHashMap<Class<*>, Method>()
    private val edgeAccessorCache         = ConcurrentHashMap<Class<*>, Method>()
    private val edgeCategoryAccessorCache = ConcurrentHashMap<Class<*>, Method>()
    private val feedUnitAccessorCache     = ConcurrentHashMap<Class<*>, Method>()
    private val backendDataAccessorCache  = ConcurrentHashMap<Class<*>, Method>()
    private val typeNameMethodCache       = ConcurrentHashMap<Class<*>, Method>()
    private val stringAccessorCache       = ConcurrentHashMap<Class<*>, List<Method>>()
    private val stringFieldCache          = ConcurrentHashMap<Class<*>, List<Field>>()

    private data class FeedItemFacts(
        val modelCategory: String?,
        val edgeCategory: String?,
        val network: Boolean?,
        val inflatedUnitClass: String?,
        val inflatedTypeName: String?,
        val backendUnitClass: String?,
        val backendTypeName: String?
    )

    fun isSponsoredFeedItem(value: Any?): Boolean {
        if (isDefinitelySponsoredFeedItem(value)) return true
        val model       = invokeNoThrow(itemModelAccessor, value)
        val edge        = edgeFrom(value)
        val feedUnit    = feedUnitFrom(edge)
        // TEST 3 - DISABLED: backendData is new in this version; skip it to check
        // if it's contributing to the mis-hide (old code never looked at it).
        // val backendData = backendDataFrom(edge)
        if (containsKnownAdSignals(value))       return true
        if (containsKnownAdSignals(model))       return true
        if (containsKnownAdSignals(edge))        return true
        if (containsKnownAdSignals(feedUnit))    return true
        // if (containsKnownAdSignals(backendData)) return true
        return false
    }

    fun isDefinitelySponsoredFeedItem(value: Any?): Boolean {
        if (value == null) return false
        val model         = invokeNoThrow(itemModelAccessor, value)
        val modelCategory = readCategory(model)
        if (isSafeFeedContainerCategory(modelCategory)) return false
        if (isSponsoredFeedCategory(modelCategory))     return true

        val edge         = edgeFrom(value)
        // TEST 3 - DISABLED: readEdgeCategory is new; fall back to the old readCategory only.
        val edgeCategory = /* readEdgeCategory(edge) ?: */ readCategory(edge)
        if (isSafeFeedContainerCategory(edgeCategory)) return false
        if (isSponsoredFeedCategory(edgeCategory))     return true

        val feedUnit               = feedUnitFrom(edge)
        // val backendData            = backendDataFrom(edge)
        val inflatedUnitClassName  = feedUnit?.javaClass?.name
        // val backendUnitClassName   = backendData?.javaClass?.name
        if (inflatedUnitClassName == GRAPHQL_MULTI_ADS_FEED_UNIT_CLASS ||
            inflatedUnitClassName == GRAPHQL_QUICK_PROMO_FEED_UNIT_CLASS) return true

        val typeName = readTypeName(feedUnit) /* ?: readTypeName(backendData) */
        if (isLikelyAdTypeName(typeName) ||
            isAdSignalText(inflatedUnitClassName) /* || isAdSignalText(backendUnitClassName) */) return true

        return false
    }

    /** Kept as a separate hook from [isDefinitelySponsoredFeedItem] so the strict-block
     *  reason can later be differentiated (e.g. "strict" vs. a broader network-based
     *  reason) without changing [hookStoryPoolAdd]'s call site. */
    fun storyPoolBlockReason(value: Any?): String? =
        if (isDefinitelySponsoredFeedItem(value)) "strict" else null

    fun describe(item: Any?): String {
        if (item == null) return "null"
        val facts = factsFor(item)
        val modelCategory     = facts.modelCategory ?: "unknown"
        val edgeCategory      = facts.edgeCategory ?: "unknown"
        val network           = facts.network?.toString() ?: "unknown"
        val inflatedUnitClass = facts.inflatedUnitClass ?: "null"
        val inflatedTypeName  = facts.inflatedTypeName ?: "unknown"
        val backendUnitClass  = facts.backendUnitClass ?: "null"
        val backendTypeName   = facts.backendTypeName ?: "unknown"
        return "modelCat=$modelCategory edgeCat=$edgeCategory isAd=${isSponsoredFeedItem(item)} " +
            "network=$network wrapper=${item.javaClass.name} " +
            "inflated=$inflatedUnitClass/$inflatedTypeName backend=$backendUnitClass/$backendTypeName"
    }

    private fun factsFor(item: Any?): FeedItemFacts {
        val model       = invokeNoThrow(itemModelAccessor, item)
        val edge        = edgeFrom(item)
        val feedUnit    = feedUnitFrom(edge)
        val backendData = backendDataFrom(edge)
        return FeedItemFacts(
            modelCategory     = readCategory(model),
            edgeCategory      = readEdgeCategory(edge) ?: readCategory(edge),
            network           = invokeNoThrow(itemNetworkAccessor, item) as? Boolean,
            inflatedUnitClass = feedUnit?.javaClass?.name,
            inflatedTypeName  = readTypeName(feedUnit),
            backendUnitClass  = backendData?.javaClass?.name,
            backendTypeName   = readTypeName(backendData)
        )
    }

    private fun edgeFrom(value: Any?): Any? {
        if (value == null) return null
        if (value.javaClass.name == GRAPHQL_FEED_UNIT_EDGE_CLASS) return value
        invokeNoThrow(itemEdgeAccessor, value)?.let { d -> if (d.javaClass.name == GRAPHQL_FEED_UNIT_EDGE_CLASS) return d }
        val fallback = cachedMethod(edgeAccessorCache, value.javaClass) {
            resolveChildAccessor(value) { it != null && it.javaClass.name == GRAPHQL_FEED_UNIT_EDGE_CLASS }
        }
        return invokeNoThrow(fallback, value)
    }

    /** Tries the known obfuscated accessor names first ("BL9"/"A03"); falls back to the
     *  generic child-accessor heuristic, now also excluding "FeedBackendData" (the
     *  sibling node added by [backendDataFrom]) so the two never resolve to each other. */
    private fun feedUnitFrom(edge: Any?): Any? {
        if (edge == null) return null
        val accessor = cachedMethod(feedUnitAccessorCache, edge.javaClass) {
            resolveNamedNoArgAccessor(edge.javaClass, "BL9")
                ?: resolveNamedNoArgAccessor(edge.javaClass, "A03")
                ?: resolveChildAccessor(edge) { v ->
                    val cn = v?.javaClass?.name
                    cn == GRAPHQL_MULTI_ADS_FEED_UNIT_CLASS || cn == GRAPHQL_QUICK_PROMO_FEED_UNIT_CLASS ||
                    readTypeName(v)?.let { it != "FeedUnitEdge" && it != "FeedBackendData" } == true
                }
        }
        return invokeNoThrow(accessor, edge)
    }

    /** "FeedBackendData" is a sibling of the inflated feed unit on the same edge — a
     *  second, independent data path that can also carry ad signals (this is the
     *  upstream fix for ads that don't show up via the normal inflated feedUnit). */
    private fun backendDataFrom(edge: Any?): Any? {
        if (edge == null) return null
        val accessor = cachedMethod(backendDataAccessorCache, edge.javaClass) {
            resolveNamedNoArgAccessor(edge.javaClass, "BL0")
                ?: resolveNamedNoArgAccessor(edge.javaClass, "A05")
                ?: resolveChildAccessor(edge) { v -> readTypeName(v) == "FeedBackendData" }
        }
        return invokeNoThrow(accessor, edge)
    }

    private fun readEdgeCategory(value: Any?): String? {
        if (value == null) return null
        val accessor = cachedMethod(edgeCategoryAccessorCache, value.javaClass) {
            resolveNamedNoArgAccessor(value.javaClass, "B4k")
                ?: allInstanceMethods(value.javaClass).firstOrNull { m ->
                    m.parameterCount == 0 && m.returnType.isEnum &&
                    m.returnType.enumConstants?.any { val n = it.toString(); n == "SPONSORED" || n == "PROMOTION" } == true
                }?.apply { isAccessible = true }
        }
        return invokeNoThrow(accessor, value)?.toString()
    }

    /** Looks for an exact-named accessor on the item-contract interfaces first
     *  (the obfuscated name is stable across most Facebook builds for a given
     *  feed-item contract), before falling back to the generic shape-based search. */
    private fun resolveItemContractAccessor(itemContractTypes: Collection<Class<*>>, methodName: String): Method? =
        itemContractTypes.asSequence()
            .flatMap { allInstanceMethods(it).asSequence() }
            .firstOrNull { m -> m.parameterCount == 0 && m.name == methodName }
            ?.apply { isAccessible = true }

    private fun resolveNamedNoArgAccessor(type: Class<*>, methodName: String): Method? =
        allInstanceMethods(type).firstOrNull { m -> m.parameterCount == 0 && m.name == methodName }
            ?.apply { isAccessible = true }

    fun describeAccessors(): String =
        "model=${accessorName(itemModelAccessor)} edge=${accessorName(itemEdgeAccessor)} network=${accessorName(itemNetworkAccessor)}"

    private fun accessorName(method: Method?): String =
        method?.let { "${it.declaringClass.name}.${it.name}" } ?: "unresolved"

    private fun readCategory(value: Any?): String? {
        if (value == null) return null
        if (value.javaClass.isEnum) return value.toString()
        val accessor = cachedMethod(categoryMethodCache, value.javaClass) {
            allInstanceMethods(value.javaClass).firstOrNull { m ->
                m.parameterCount == 0 && m.returnType.isEnum &&
                m.returnType.enumConstants?.any { val n = it.toString(); n == "SPONSORED" || n == "PROMOTION" } == true
            }?.apply { isAccessible = true }
        }
        return invokeNoThrow(accessor, value)?.toString()
    }

    private fun readTypeName(value: Any?): String? {
        if (value == null) return null
        val accessor = cachedMethod(typeNameMethodCache, value.javaClass) {
            resolveNamedNoArgAccessor(value.javaClass, "getTypeName")
                ?: allInstanceMethods(value.javaClass).firstOrNull { m ->
                    m.parameterCount == 0 && m.returnType == String::class.java && m.name == "getTypeName"
                }?.apply { isAccessible = true }
        }
        return invokeNoThrow(accessor, value) as? String
    }

    private fun cachedMethod(cache: ConcurrentHashMap<Class<*>, Method>, type: Class<*>, resolver: () -> Method?): Method? {
        cache[type]?.let { return it }; val resolved = resolver() ?: return null
        return cache.putIfAbsent(type, resolved) ?: resolved
    }

    // Excludes "A02"/"BG7" in addition to "clone" — those are the named edge/other
    // accessors that could otherwise be mistakenly picked up by this generic search.
    private fun resolveItemModelAccessor(types: Collection<Class<*>>) = types.asSequence()
        .flatMap { allInstanceMethods(it).asSequence() }
        .firstOrNull { m ->
            m.parameterCount == 0 && m.name != "clone" && m.name != "A02" && m.name != "BG7" &&
            !m.returnType.isPrimitive && m.returnType != Any::class.java && m.returnType != String::class.java && !m.returnType.isEnum
        }
        ?.apply { isAccessible = true }

    private fun resolveItemEdgeAccessor(types: Collection<Class<*>>) = types.asSequence()
        .flatMap { allInstanceMethods(it).asSequence() }
        .firstOrNull { m ->
            m.parameterCount == 0 && m.name != "clone" &&
            (m.returnType == Any::class.java || m.returnType.name == GRAPHQL_FEED_UNIT_EDGE_CLASS)
        }
        ?.apply { isAccessible = true }

    private fun resolveItemNetworkAccessor(types: Collection<Class<*>>) = types.asSequence()
        .flatMap { allInstanceMethods(it).asSequence() }
        .firstOrNull { m -> m.parameterCount == 0 && m.returnType == Boolean::class.javaPrimitiveType }
        ?.apply { isAccessible = true }

    private fun resolveChildAccessor(target: Any, acceptsValue: (Any?) -> Boolean): Method? =
        allInstanceMethods(target.javaClass).asSequence()
            .filter { m -> m.parameterCount == 0 && !m.returnType.isPrimitive && m.returnType != Void.TYPE && m.returnType != String::class.java && !m.returnType.isEnum && m.declaringClass != Any::class.java }
            .sortedByDescending { m -> scoreChildAccessor(m.returnType) }
            .firstOrNull { m -> acceptsValue(invokeNoThrow(m.apply { isAccessible = true }, target)) }

    private fun scoreChildAccessor(type: Class<*>): Int = when {
        type.name == GRAPHQL_FEED_UNIT_EDGE_CLASS                             -> 4
        type.name.startsWith("com.facebook.graphql.model.")                   -> 3
        type.name.startsWith("com.facebook.")                                 -> 2
        !type.name.startsWith("java.") && !type.name.startsWith("javax.") &&
        !type.name.startsWith("android.") && !type.name.startsWith("kotlin.") -> 1
        else                                                                   -> 0
    }

    private fun containsKnownAdSignals(value: Any?): Boolean {
        if (value == null) return false
        if (value is CharSequence) return isAdSignalText(value.toString())
        val type = value.javaClass
        if (isAdSignalText(type.name)) return true
        if (type.isEnum) return isAdSignalText(value.toString())
        if (type.isPrimitive || value is Number || value is Boolean) return false
        if (isAdSignalText(runCatching { value.toString() }.getOrNull())) return true
        for (m in stringAccessorsFor(type)) if (isAdSignalText(invokeNoThrow(m, value) as? String)) return true
        for (f in stringFieldsFor(type)) if (isAdSignalText(runCatching { f.get(value) as? String }.getOrNull())) return true
        return false
    }

    private fun stringAccessorsFor(type: Class<*>) = stringAccessorCache.getOrPut(type) {
        allInstanceMethods(type).asSequence()
            .filter { m -> m.parameterCount == 0 && m.returnType == String::class.java && m.declaringClass != Any::class.java && m.name != "toString" }
            .take(12).onEach { m -> m.isAccessible = true }.toList()
    }

    private fun stringFieldsFor(type: Class<*>) = stringFieldCache.getOrPut(type) {
        val list = ArrayList<Field>(); var cur: Class<*>? = type
        while (cur != null && cur != Any::class.java && list.size < 12) {
            cur.declaredFields.forEach { f -> if (!Modifier.isStatic(f.modifiers) && f.type == String::class.java && list.size < 12) { f.isAccessible = true; list.add(f) } }; cur = cur.superclass
        }; list
    }

    fun isAdSignalText(value: String?): Boolean {
        if (value.isNullOrBlank()) return false; val n = value.lowercase()
        return FEED_AD_SIGNAL_TOKENS.any { n.contains(it) }
    }

    private fun isSponsoredFeedCategory(v: String?)    = v != null && v in FEED_AD_CATEGORY_VALUES
    private fun isSafeFeedContainerCategory(v: String?) = v != null && v in FEED_SAFE_CONTAINER_CATEGORY_VALUES
    private fun isLikelyAdTypeName(v: String?)          = v != null && (v.contains("QuickPromotion", ignoreCase = true) || isAdSignalText(v))

    private fun allInstanceMethods(type: Class<*>): List<Method> {
        val map = LinkedHashMap<String, Method>(); var cur: Class<*>? = type
        while (cur != null && cur != Any::class.java) {
            cur.declaredMethods.forEach { m -> if (!Modifier.isStatic(m.modifiers)) { m.isAccessible = true; map.putIfAbsent("${cur.name}#${m.name}/${m.parameterCount}", m) } }
            cur.interfaces.forEach { iface -> iface.declaredMethods.forEach { m -> if (!Modifier.isStatic(m.modifiers)) { m.isAccessible = true; map.putIfAbsent("${iface.name}#${m.name}/${m.parameterCount}", m) } } }
            cur = cur.superclass
        }; return map.values.toList()
    }

    private fun invokeNoThrow(method: Method?, target: Any?) =
        if (method == null || target == null) null else runCatching { method.invoke(target) }.getOrNull()
}

// ─── Logging ──────────────────────────────────────────────────────────────────

fun logHookHitThrottled(hookName: String, method: Method, detail: String? = null) {
    val hits = hookHitCounters.computeIfAbsent(hookName) { AtomicInteger(0) }.incrementAndGet()
}

// ─── Hook installers – Reels / list-builder ───────────────────────────────────

fun hookListBuilderAppend(method: Method, inspector: AdStoryInspector) {
    val listArgIndex = method.listParameterIndexes().singleOrNull()
    if (listArgIndex == null) {
        return
    }
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            param.setObjectExtra(BEFORE_SIZE_EXTRA, (param.args.getOrNull(listArgIndex) as? List<*>)?.size ?: -1)
        }
        override fun afterHookedMethod(param: MethodHookParam) {
            val beforeSize = param.getObjectExtra(BEFORE_SIZE_EXTRA) as? Int ?: return
            val list = param.args.getOrNull(listArgIndex) as? MutableList<Any?> ?: return
            if (beforeSize < 0 || beforeSize > list.size) return
            var removed = 0
            for (i in list.lastIndex downTo beforeSize) { if (inspector.containsAdStory(list[i])) { list.removeAt(i); removed++ } }
        }
    })
}

fun hookListResultFilter(method: Method, source: String, inspector: AdStoryInspector) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val result = param.result as? MutableList<Any?> ?: return
            val removed = filterAdItems(result, inspector)
        }
    })
}

fun hookPluginPackFallback(method: Method, inspector: AdStoryInspector) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            if (isMarketplaceAdsPluginPack(param.thisObject)) {
                param.result = arrayListOf<Any?>(); return
            }
            if (inspector.containsAdStory(param.thisObject)) {
                param.result = arrayListOf<Any?>()
            }
        }
        override fun afterHookedMethod(param: MethodHookParam) {
            if (isMarketplaceAdsPluginPack(param.thisObject)) return
            val result = param.result as? MutableList<Any?> ?: return
            val removed = filterAdItems(result, inspector)
        }
    })
}

private fun isMarketplaceAdsPluginPack(instance: Any): Boolean {
    val className = instance.javaClass.name
    return marketplaceAdsPackCache.getOrPut(className) {
        runCatching {
            instance.javaClass.declaredMethods
                .filter { m -> m.parameterCount == 0 && m.returnType == String::class.java && !Modifier.isStatic(m.modifiers) }
                .any { m -> m.isAccessible = true; (m.invoke(instance) as? String)?.contains("Ads", ignoreCase = true) == true }
        }.getOrDefault(false)
    }
}

// ─── Hook installers – Feed CSR / late-list ───────────────────────────────────

fun hookFeedCsrFilterInput(hook: FeedCsrFilterHook, inspector: FeedItemInspector) {
    XposedBridge.hookMethod(hook.method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val originalList = param.args.getOrNull(hook.listArgIndex) as? Iterable<*> ?: return
            val kept = ArrayList<Any?>(); var removed = 0
            // Strict check on the way IN: Facebook's own pipeline hasn't finished
            // resolving every item yet here, so the broader heuristic risks false
            // positives — only drop items we're certain are ads.
            for (item in originalList) { if (inspector.isDefinitelySponsoredFeedItem(item)) removed++ else kept.add(item) }
            if (removed <= 0) return
            buildImmutableListLike(param.args.getOrNull(hook.listArgIndex), kept)?.let { param.args[hook.listArgIndex] = it }
        }
        override fun afterHookedMethod(param: MethodHookParam) {
            val resultItems = extractFeedItemsFromResult(param.result) ?: return
            val kept = ArrayList<Any?>(); var removed = 0
            // Broader check on the way OUT: the pipeline has now resolved everything,
            // so the wider heuristic is safe to apply here.
            for (item in resultItems) { if (inspector.isSponsoredFeedItem(item)) removed++ else kept.add(item) }
            if (removed > 0) replaceFeedItemsInResult(param, kept)
        }
    })
}

fun hookLateFeedListSanitizer(hook: FeedListSanitizerHook, inspector: FeedItemInspector) {
    XposedBridge.hookMethod(hook.method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val originalList = param.args.getOrNull(hook.listArgIndex) as? Iterable<*> ?: return
            val kept = ArrayList<Any?>(); var removed = 0
            for (item in originalList) { if (inspector.isSponsoredFeedItem(item)) removed++ else kept.add(item) }
            if (removed <= 0) return
            buildImmutableListLike(param.args.getOrNull(hook.listArgIndex), kept)?.let {
                param.args[hook.listArgIndex] = it
            }
        }
    })
}

fun hookStoryPoolAdd(method: Method, inspector: FeedItemInspector) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val item = param.args.getOrNull(0)
            val blockReason = inspector.storyPoolBlockReason(item)
            if (blockReason == null) {
                // Broad check: log but allow (same as upstream)
                if (inspector.isSponsoredFeedItem(item)) {
                    logHookHitThrottled("storyPoolBroadAllowed", method, inspector.describe(item))
                }
                return
            }
            param.result = false
            logHookHitThrottled(
                if (blockReason == "strict") "storyPoolStrictBlock" else "storyPoolBroadNetworkBlock",
                method, inspector.describe(item)
            )
        }
    })
}

fun hookInstreamBannerEligibility(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) { logHookHitThrottled("bannerState", method); param.result = false }
    })
}

fun hookIndicatorPillAdEligibility(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) { logHookHitThrottled("indicatorPill", method, "slot=${param.args.getOrNull(2) ?: "?"}"); param.result = false }
    })
}

fun hookReelsBannerRender(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) { logHookHitThrottled("reelsBannerRender", method); param.result = null }
    })
}

// ─── Hook installers – Sponsored pool ────────────────────────────────────────

fun hookSponsoredPoolAdd(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) { param.result = false }
    })
}

fun hookSponsoredStoryNext(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) { param.result = null }
    })
}

fun hookSponsoredPoolListMethods(poolClass: Class<*>) {
    var hooked = 0
    poolClass.declaredMethods.filter { m -> !Modifier.isStatic(m.modifiers) && m.parameterCount == 0 && List::class.java.isAssignableFrom(m.returnType) }.forEach { m ->
        m.isAccessible = true
        XposedBridge.hookMethod(m, object : XC_MethodHook() { override fun beforeHookedMethod(param: MethodHookParam) { param.result = arrayListOf<Any?>() } })
        hooked++
    }
}

fun hookSponsoredPoolResultMethods(poolClass: Class<*>) {
    var hooked = 0
    poolClass.declaredMethods.filter { m ->
        !Modifier.isStatic(m.modifiers) && isSponsoredResultCarrier(m.returnType) &&
        (m.parameterCount == 0 || (m.parameterCount == 1 && m.parameterTypes[0] == Boolean::class.javaPrimitiveType))
    }.forEach { m ->
        m.isAccessible = true
        XposedBridge.hookMethod(m, object : XC_MethodHook() { override fun beforeHookedMethod(param: MethodHookParam) { buildSponsoredEmptyResult(m.returnType)?.let { param.result = it } } })
        hooked++
    }
}

/** Blocks any list/collection-returning getter on the sponsored story manager class
 *  whose params are limited to int/long/boolean (paging/cursor style accessors) —
 *  these back the "next sponsored story" vending path alongside the single-item
 *  [hookSponsoredStoryNext] hook. */
fun hookSponsoredStoryListMethods(managerClass: Class<*>) {
    var hooked = 0
    managerClass.declaredMethods.filter { m -> !Modifier.isStatic(m.modifiers) && isSponsoredStoryListMethod(m) }.forEach { m ->
        m.isAccessible = true
        XposedBridge.hookMethod(m, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                buildEmptyListReturn(m.returnType)?.let { param.result = it }
            }
        })
        hooked++
    }
}

private fun isSponsoredStoryListMethod(method: Method): Boolean {
    if (method.parameterCount > 2) return false
    if (!Iterable::class.java.isAssignableFrom(method.returnType) &&
        method.returnType.name != "com.google.common.collect.ImmutableList") {
        return false
    }
    return method.parameterTypes.all { type ->
        type == Int::class.javaPrimitiveType || type == Long::class.javaPrimitiveType || type == Boolean::class.javaPrimitiveType
    }
}

private fun buildEmptyListReturn(returnType: Class<*>): Any? {
    if (returnType.name == "com.google.common.collect.ImmutableList") {
        return runCatching {
            val of = returnType.getDeclaredMethod("of")
            of.isAccessible = true
            of.invoke(null)
        }.getOrNull()
    }
    return when {
        returnType.isAssignableFrom(ArrayList::class.java) -> arrayListOf<Any?>()
        Iterable::class.java.isAssignableFrom(returnType) -> emptyList<Any?>()
        else -> null
    }
}

// ─── List builder method resolution (flexible, score-based) ──────────────────
//
// Facebook occasionally ships a list-builder variant with a different parameter
// count/order than the structural 6-param shape we search for at the DexKit level
// (see listBuilderClassFingerprint). Once the CLASS is resolved — which IS cached
// via DexKit — we pick the append/factory method by scoring every candidate method
// on that class via plain reflection, instead of requiring an exact param shape.
// This mirrors upstream FacebookAppAdsRemover's resolveAppendMethod/resolveFactoryMethod.

private fun Method.listParameterIndexes(): List<Int> =
    parameterTypes.mapIndexedNotNull { index, type -> index.takeIf { List::class.java.isAssignableFrom(type) } }

private fun resolveListBuilderMethods(clazz: Class<*>): List<Method> {
    val methods = LinkedHashMap<String, Method>()
    (clazz.declaredMethods + clazz.methods).forEach { method ->
        if (method.name != "<init>" && method.name != "<clinit>") {
            methods.putIfAbsent("${method.name}/${method.parameterCount}/${Modifier.isStatic(method.modifiers)}", method)
        }
    }
    return methods.values.toList()
}

private fun scoreAppendMethod(method: Method, owner: Class<*>): Int {
    val listIndex = method.listParameterIndexes().firstOrNull() ?: return Int.MIN_VALUE
    var score = 0
    if (listIndex == method.parameterCount - 1) score += 10_000
    if (method.parameterCount == 6) score += 5_000
    if (!Modifier.isStatic(method.modifiers)) score += 2_000
    if (Modifier.isStatic(method.modifiers) && method.parameterTypes.getOrNull(1) == owner) score += 1_500
    if (Modifier.isStatic(method.modifiers) && method.parameterTypes.firstOrNull() == owner) score += 750
    score -= method.parameterCount * 10
    return score
}

private fun scoreFactoryMethod(method: Method, owner: Class<*>): Int {
    var score = 0
    if (method.parameterCount == 6) score += 4_000
    if (method.parameterCount == 5) score += 3_000
    if (method.parameterTypes.getOrNull(1) == owner) score += 2_000
    if (method.parameterTypes.firstOrNull() == owner) score += 1_000
    if (method.parameterTypes.firstOrNull()?.name == "com.facebook.auth.usersession.FbUserSession") score += 500
    score -= method.parameterCount * 10
    return score
}

/** Resolves the static/instance "append item(s) to the in-progress Reels list" method:
 *  exactly one List-typed parameter, positioned last, on the resolved list-builder class. */
fun resolveListBuilderAppendMethod(listBuilderClass: Class<*>): Method =
    resolveListBuilderMethods(listBuilderClass)
        .filter { method ->
            method.returnType == Void.TYPE &&
            method.listParameterIndexes().size == 1 &&
            method.listParameterIndexes().first() == method.parameterCount - 1
        }
        .maxByOrNull { method -> scoreAppendMethod(method, listBuilderClass) }
        ?.apply { isAccessible = true }
        ?: error("Unable to resolve the Facebook Reels list append method on ${listBuilderClass.name}")

/** Resolves the static "ArrayList factory(listBuilder, ..., boolean)" method —
 *  optional: not every Facebook build has a separate factory method. */
fun resolveListBuilderFactoryMethod(listBuilderClass: Class<*>): Method? =
    resolveListBuilderMethods(listBuilderClass)
        .filter { method ->
            Modifier.isStatic(method.modifiers) &&
            method.returnType == ArrayList::class.java &&
            method.parameterTypes.lastOrNull() == Boolean::class.javaPrimitiveType &&
            (method.parameterTypes.firstOrNull() == listBuilderClass || method.parameterTypes.getOrNull(1) == listBuilderClass)
        }
        .maxByOrNull { method -> scoreFactoryMethod(method, listBuilderClass) }
        ?.apply { isAccessible = true }

// ─── Hook installers – Story ad providers ────────────────────────────────────

fun hookStoryAdsNoOp(method: Method, reason: String = "story ad", source: String = method.declaringClass.name) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            param.result = null
        }
    })
}

fun hookStoryAdsMerge(method: Method, source: String = method.declaringClass.name) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val originalBuckets = param.args.getOrNull(2)
            if (originalBuckets != null) {
                param.result = originalBuckets
            }
        }
    })
}

fun hookStoryAdProvider(provider: StoryAdProviderHooks) {
    val hooked = ArrayList<String>()
    provider.mergeMethod?.let { method ->
        hookStoryAdsMerge(method, provider.providerClass.name); hooked.add("merge")
    }
    provider.fetchMoreAdsMethod?.let { method ->
        hookStoryAdsNoOp(method, "story ad fetchMoreAds", provider.providerClass.name); hooked.add("fetchMoreAds")
    }
    provider.deferredUpdateMethod?.let { method ->
        hookStoryAdsNoOp(method, "story ad deferred update", provider.providerClass.name); hooked.add("deferredUpdate")
    }
    provider.insertionTriggerMethod?.let { method ->
        hookStoryAdsNoOp(method, "story ad insertion trigger", provider.providerClass.name); hooked.add("insertionTrigger")
    }
}

// ─── Hook installers – Game ads ───────────────────────────────────────────────

fun hookGameAdRequest(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val payload = param.args.getOrNull(0) ?: return
            val messageType = inferGameAdMessageType(method, payload)
            rememberGameAdPayload(param.thisObject, payload, messageType)
            if (rejectUnavailableGameAdPayloadIfNeeded(param.thisObject, payload, messageType, "request ${method.declaringClass.name}.${method.name}")) { param.result = null; return }
            if (!shouldAutofixGameAdMessage(messageType)) return
            if (resolveGameAdPayload(param.thisObject, payload, messageType)) {
                dispatchPostResolveGameAdSignals(param.thisObject, payload, messageType)
                param.result = null
            } else if (rejectGameAdPayload(param.thisObject, payload)) {
                param.result = null
            }
        }
    })
}

fun hookGameAdBridge(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun beforeHookedMethod(param: MethodHookParam) {
            val raw = param.args.getOrNull(0) as? String ?: return
            val payload = runCatching { JSONObject(raw) }.getOrNull() ?: return
            val type = payload.optString("type"); if (type !in GAME_AD_MESSAGE_TYPES) return
            rememberGameAdPayload(param.thisObject, payload, type)
            if (rejectUnavailableGameAdPayloadIfNeeded(param.thisObject, payload, type, "bridge ${method.declaringClass.name}.${method.name}")) { param.result = null; return }
            if (!shouldAutofixGameAdMessage(type)) return
            if (resolveGameAdPayload(param.thisObject, payload, type)) {
                dispatchPostResolveGameAdSignals(param.thisObject, payload, type)
                param.result = null
            } else if (rejectGameAdPayload(param.thisObject, payload)) {
                param.result = null
            }
        }
    })
}

/** Hook resolve/reject methods on the bridge class for deeper interception. */
fun hookGameAdResultMethods(bridgeClass: Class<*>) {
    if (!gameAdResultHooksInstalled.compareAndSet(0, 1)) return
    val resolveMethod = resolveGameAdResolveMethod(bridgeClass)
    val rejectMethod  = resolveGameAdRejectMethod(bridgeClass)
    val bridgeRejectMethod = resolveGameAdBridgeRejectMethod(bridgeClass)
    var hooked = 0

    resolveMethod?.let { m ->
        XposedBridge.hookMethod(m, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val promiseId = param.args.getOrNull(0) as? String ?: return
                val snapshot = gameAdPromiseSnapshots[promiseId] ?: return
                if (snapshot.messageType !in GAME_AD_MESSAGE_TYPES) return
                if (!shouldAutofixGameAdMessage(snapshot.messageType)) return
                val original = param.args.getOrNull(1)
                param.args[1] = forceGameAdSuccessResult(promiseId, original, snapshot.payload, snapshot.messageType)
            }
        }); hooked++
    }

    if (rejectMethod != null && resolveMethod != null) {
        XposedBridge.hookMethod(rejectMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val promiseId = param.args.getOrNull(0) as? String ?: return
                val reason = param.args.drop(1).joinToString(" ") { it?.toString().orEmpty() }
                if (!shouldConvertGameAdRejectToSuccess(promiseId, reason)) return
                val snapshot = gameAdPromiseSnapshots[promiseId]
                val success = forceGameAdSuccessResult(promiseId, null, snapshot?.payload, snapshot?.messageType ?: gameAdPromiseTypeFromReason(reason))
                runCatching { XposedBridge.invokeOriginalMethod(resolveMethod, param.thisObject, arrayOf(promiseId, success)); param.result = null }
            }
        }); hooked++
    }

    if (bridgeRejectMethod != null && resolveMethod != null && bridgeRejectMethod != rejectMethod) {
        XposedBridge.hookMethod(bridgeRejectMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val payload = param.args.getOrNull(2) as? JSONObject ?: return
                val promiseId = extractPromiseId(payload) ?: return
                val reason = param.args.take(2).joinToString(" ") { it?.toString().orEmpty() }
                if (!shouldConvertGameAdRejectToSuccess(promiseId, reason)) return
                val snapshot = gameAdPromiseSnapshots[promiseId]
                val success = forceGameAdSuccessResult(promiseId, null, snapshot?.payload ?: payload, snapshot?.messageType ?: gameAdPromiseTypeFromReason(reason))
                runCatching { XposedBridge.invokeOriginalMethod(resolveMethod, param.thisObject, arrayOf(promiseId, success)); param.result = null }
            }
        }); hooked++
    }
}

/** Hook Bundle-based service dispatch methods on the bridge class. */
fun hookGameAdServiceDispatchMethods(bridgeClass: Class<*>) {
    if (!gameAdServiceDispatchHooksInstalled.compareAndSet(0, 1)) return
    val methods = (bridgeClass.declaredMethods + bridgeClass.methods).filter { m ->
        !Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE && m.parameterCount == 2 && m.parameterTypes[0] == Bundle::class.java
    }.distinctBy { m -> m.name + m.parameterTypes.joinToString { it.name } }
    var hooked = 0
    methods.forEach { m ->
        m.isAccessible = true
        XposedBridge.hookMethod(m, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val bundle = param.args.getOrNull(0) as? Bundle ?: return
                val messageType = param.args.getOrNull(1)?.toString()?.lowercase()?.takeIf { it in GAME_AD_MESSAGE_TYPES } ?: return
                val payload = buildGameAdPayloadFromServiceBundle(bundle, messageType)
                rememberGameAdPayload(param.thisObject, payload, messageType)
                if (rejectUnavailableGameAdPayloadIfNeeded(param.thisObject, payload, messageType, "service dispatch ${m.name}")) { param.result = null; return }
                if (!shouldAutofixGameAdMessage(messageType)) return
                if (resolveGameAdPayload(param.thisObject, payload, messageType)) {
                    dispatchPostResolveGameAdSignals(param.thisObject, payload, messageType); param.result = null
                }
            }
        }); hooked++
    }
}

fun hookPlayableAdActivity(method: Method) {
    XposedBridge.hookMethod(method, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val activity = param.thisObject as? Activity ?: return
            if (activity.javaClass.name != method.declaringClass.name) return
            handleGameAdActivity(activity, "direct hook ${method.declaringClass.name}.${method.name}")
        }
    })
}

fun hookGlobalGameAdActivityLifecycleFallback() {
    val onResume = (Activity::class.java.declaredMethods + Activity::class.java.methods)
        .firstOrNull { m -> m.name == "onResume" && m.parameterCount == 0 }?.apply { isAccessible = true } ?: return
    XposedBridge.hookMethod(onResume, object : XC_MethodHook() {
        override fun afterHookedMethod(param: MethodHookParam) {
            val activity = param.thisObject as? Activity ?: return
            // Always schedule a surface sweep on resume (catches async ad loads)
            scheduleGameAdSurfaceSweep(activity.window?.decorView, "activity resume ${activity.javaClass.name}")
            if (activity.javaClass.name !in GAME_AD_ACTIVITY_CLASS_NAMES) return
            handleGameAdActivity(activity, "global lifecycle fallback")
        }
    })
}

fun hookGameAdActivityLaunchFallbacks() {
    val methods = LinkedHashMap<String, Method>()
    listOf(Instrumentation::class.java, Activity::class.java, ContextWrapper::class.java).forEach { type ->
        (type.declaredMethods + type.methods).filter { m ->
            m.name in setOf("execStartActivity","startActivity","startActivityForResult","startActivityIfNeeded") &&
            m.parameterTypes.any { it == Intent::class.java }
        }.forEach { m -> m.isAccessible = true; methods.putIfAbsent("${m.declaringClass.name}.${m.name}(${m.parameterTypes.joinToString(",") { it.name }})", m) }
    }
    var hooked = 0
    methods.values.forEach { m ->
        runCatching {
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    val intent = param.args.firstOrNull { it is Intent } as? Intent ?: return
                    val target = intent.component?.className ?: return
                    if (target !in GAME_AD_ACTIVITY_CLASS_NAMES) return
                    if (!shouldBlockGameAdActivityLaunch(target)) return
                    completeRecentGameAdRequests("launch fallback $target")
                    param.result = if (m.returnType == Boolean::class.javaPrimitiveType) false else null
                }
            }); hooked++
        }
    }
}

/** Hook ViewGroup.addView, TextView.setText, View.setContentDescription, WebView
 *  methods to catch native ad views and text-marker-based ad cards. */
fun hookGlobalGameAdSurfaceFallbacks() {
    if (!gameAdSurfaceHooksInstalled.compareAndSet(0, 1)) return
    var hooked = 0

    (ViewGroup::class.java.declaredMethods + ViewGroup::class.java.methods)
        .filter { m -> m.name == "addView" && m.parameterTypes.any { it == View::class.java } }
        .distinctBy { m -> m.name + m.parameterTypes.joinToString { it.name } }
        .forEach { m ->
            m.isAccessible = true
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val parent = param.thisObject as? ViewGroup
                    val child = param.args.firstOrNull { it is View } as? View ?: return
                    when {
                        isPotentialNativeGameAdView(child) -> {
                            hideLikelyAdContainer(child, "native ad view add ${child.javaClass.name}")
                            scheduleGameAdSurfaceSweep(child, "native ad view add ${child.javaClass.name}")
                        }
                        isPotentialExplicitFeedAdMarkerView(child) -> {
                            hideLikelyAdContainer(child, "explicit feed ad view add ${child.javaClass.name}")
                            scheduleGameAdSurfaceSweep(child, "explicit feed ad view add ${child.javaClass.name}")
                        }
                        ENABLE_FEED_UI_MARKER_FALLBACKS && isPotentialFeedAdMarkerView(child) -> {
                            hideLikelyAdContainer(child, "feed ad marker view add ${child.javaClass.name}")
                            scheduleGameAdSurfaceSweep(child, "feed ad marker view add ${child.javaClass.name}")
                        }
                        ENABLE_FEED_UI_MARKER_FALLBACKS && isPotentialFeedReelCtaAdMarkerView(child) -> {
                            hideLikelyFeedReelCtaAdContainer(child, "feed reel CTA view add ${child.javaClass.name}")
                            scheduleGameAdSurfaceSweep(child, "feed reel CTA view add ${child.javaClass.name}")
                        }
                        false && shouldScheduleFeedRowSweep(parent, child) -> {
                            // TEST 1 - DISABLED: feed-row periodic sweep (new in this
                            // version, not present in the old code). Left in place but
                            // unreachable so it's easy to re-enable by removing "false &&"
                            // once we confirm whether this is the source of the mis-hide.
                            scheduleFeedRowSweep(child, "feed row add ${child.javaClass.name}")
                        }
                        child is WebView -> injectGameAdHidingScript(child)
                    }
                }
            }); hooked++
        }

    (TextView::class.java.declaredMethods + TextView::class.java.methods)
        .filter { m -> m.name == "setText" && m.parameterTypes.isNotEmpty() && CharSequence::class.java.isAssignableFrom(m.parameterTypes[0]) }
        .distinctBy { m -> m.name + m.parameterTypes.joinToString { it.name } }
        .forEach { m ->
            m.isAccessible = true
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val tv = param.thisObject as? TextView ?: return
                    if (isExplicitFeedAdMarkerText(tv.text)) {
                        hideLikelyAdContainer(tv, "explicit feed ad text")
                        return
                    }
                    if (!ENABLE_FEED_UI_MARKER_FALLBACKS) return
                    if (isAnyAdMarkerText(tv.text)) {
                        hideLikelyAdContainer(tv, "ad marker text")
                    } else if (isFeedReelCtaAdMarkerText(tv.text)) {
                        hideLikelyFeedReelCtaAdContainer(tv, "feed reel CTA text")
                    }
                }
            }); hooked++
        }

    // TEST 4 - DISABLED: the setContentDescription hook itself is new in this version
    // (old code never hooked this method at all). Registration removed entirely to
    // check if merely having this hook installed (regardless of its internal
    // condition, already neutralized in Test 2) is contributing to the mis-hide.
    // (View::class.java.declaredMethods + View::class.java.methods)
    //     .filter { m -> m.name == "setContentDescription" && m.parameterTypes.size == 1 && CharSequence::class.java.isAssignableFrom(m.parameterTypes[0]) }
    //     .distinctBy { m -> m.name + m.parameterTypes.joinToString { it.name } }
    //     .forEach { m ->
    //         m.isAccessible = true
    //         XposedBridge.hookMethod(m, object : XC_MethodHook() {
    //             override fun afterHookedMethod(param: MethodHookParam) {
    //                 val v = param.thisObject as? View ?: return
    //                 if (isExplicitFeedAdMarkerText(v.contentDescription)) {
    //                     hideLikelyAdContainer(v, "explicit feed ad content description")
    //                     return
    //                 }
    //                 if (!ENABLE_FEED_UI_MARKER_FALLBACKS) return
    //                 if (isFeedAdMarkerText(v.contentDescription)) {
    //                     hideLikelyAdContainer(v, "feed ad content description")
    //                 } else if (isFeedReelCtaAdMarkerText(v.contentDescription)) {
    //                     hideLikelyFeedReelCtaAdContainer(v, "feed reel CTA content description")
    //                 }
    //             }
    //         }); hooked++
    //     }

    (WebView::class.java.declaredMethods + WebView::class.java.methods)
        .filter { m -> m.name in setOf("loadUrl","loadData","loadDataWithBaseURL","onAttachedToWindow") }
        .distinctBy { m -> m.name + m.parameterTypes.joinToString { it.name } }
        .forEach { m ->
            m.isAccessible = true
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val wv = param.thisObject as? WebView ?: return
                    injectGameAdHidingScript(wv)
                    scheduleGameAdSurfaceSweep(wv, "webview ${m.name}")
                }
            }); hooked++
        }

}

/** Hook Audience Network reward classes to fire completion callbacks. */
fun hookAudienceNetworkRewardFallbacks(classLoader: ClassLoader) {
    if (!audienceNetworkRewardHooksInstalled.compareAndSet(0, 1)) return

    listOf(
        "com.facebook.ads.RewardedVideoAd",
        "com.facebook.ads.RewardedInterstitialAd",
        "com.facebook.ads.RewardedVideoAdListener",
        "com.facebook.ads.RewardedInterstitialAdListener",
        "com.facebook.ads.RewardedVideoAd\$RewardedVideoAdLoadConfigBuilder",
        "com.facebook.ads.RewardedInterstitialAd\$RewardedInterstitialAdLoadConfigBuilder"
    ).forEach { cn -> runCatching { tryHookAudienceNetworkRewardClass(classLoader.loadClass(cn)) } }

    (ClassLoader::class.java.declaredMethods + ClassLoader::class.java.methods)
        .filter { m -> m.name == "loadClass" && m.parameterTypes.isNotEmpty() && m.parameterTypes[0] == String::class.java }
        .distinctBy { m -> m.name + m.parameterTypes.joinToString { it.name } }
        .forEach { m ->
            m.isAccessible = true
            XposedBridge.hookMethod(m, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val clazz = param.result as? Class<*> ?: return
                    if (isAudienceNetworkRewardRelevantClass(clazz.name)) tryHookAudienceNetworkRewardClass(clazz)
                }
            })
        }
}

private fun tryHookAudienceNetworkRewardClass(clazz: Class<*>) {
    val className = clazz.name
    if (!isAudienceNetworkRewardRelevantClass(className) || !audienceNetworkRewardClassesHooked.add(className)) return
    var hooked = 0
    val methods = runCatching { clazz.declaredMethods + clazz.methods }.getOrDefault(emptyArray())
    methods.distinctBy { m -> m.name + m.parameterTypes.joinToString(prefix = "(", postfix = ")") { it.name } }
        .forEach { m ->
            runCatching {
                m.isAccessible = true
                if (isAudienceNetworkRewardShowMethod(clazz, m)) {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            val adObject = param.thisObject ?: return
                            if (!completeAudienceNetworkRewardObject(adObject, "show ${clazz.name}.${m.name}")) return
                            param.result = when (m.returnType) {
                                Boolean::class.javaPrimitiveType, Boolean::class.java -> true
                                else -> null
                            }
                        }
                    }); hooked++
                } else if (isAudienceNetworkRewardListenerRegistrationMethod(m)) {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) { rememberAudienceNetworkRewardListeners(param.thisObject, param.args, m) }
                        override fun afterHookedMethod(param: MethodHookParam) {
                            rememberAudienceNetworkRewardListeners(param.thisObject, param.args, m)
                            rememberAudienceNetworkRewardListeners(param.result, param.args, m)
                        }
                    }); hooked++
                } else if (isAudienceNetworkRewardLoadMethod(clazz, m)) {
                    XposedBridge.hookMethod(m, object : XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) { rememberAudienceNetworkRewardListeners(param.thisObject, param.args, m) }
                    }); hooked++
                }
            }.onFailure {  }
        }
}

private fun isAudienceNetworkRewardLoadMethod(clazz: Class<*>, method: Method) =
    clazz.name.lowercase().contains("reward") &&
    method.name.lowercase().contains("load") &&
    !Modifier.isStatic(method.modifiers) &&
    method.parameterCount >= 1

// ─── Game ad payload helpers ──────────────────────────────────────────────────

fun resolveGameAdPayload(target: Any?, payload: Any?, messageType: String? = null): Boolean {
    if (target == null || payload == null) return false
    val promiseId = extractPromiseId(payload) ?: return false
    val resolveMethod = resolveGameAdResolveMethod(target.javaClass) ?: return false
    val successPayload = buildGameAdSuccessPayload(payload, messageType)
    return runCatching { resolveMethod.invoke(target, promiseId, successPayload); true }.getOrElse { false }
}

fun rejectGameAdPayload(
    target: Any?, payload: Any?,
    message: String = GAME_AD_REJECTION_MESSAGE,
    code: String = GAME_AD_REJECTION_CODE
): Boolean {
    if (target == null || payload == null) return false
    resolveGameAdBridgeRejectMethod(target.javaClass)?.let { m ->
        if (runCatching { m.invoke(target, message, code, payload); true }.getOrElse { false }) return true
    }
    val promiseId = extractPromiseId(payload) ?: return false
    val rejectMethod = resolveGameAdRejectMethod(target.javaClass) ?: return false
    return runCatching { rejectMethod.invoke(target, promiseId, message, code); true }.getOrElse { false }
}

private fun rejectUnavailableGameAdPayloadIfNeeded(target: Any?, payload: Any?, messageType: String?, source: String = "unknown"): Boolean {
    if (!shouldMakeGameAdUnavailable(payload, messageType)) return false
    if (!rejectGameAdPayload(target, payload, GAME_AD_UNAVAILABLE_MESSAGE, GAME_AD_UNAVAILABLE_CODE)) {
        return false
    }
    lastUnavailableGameAdMs.set(System.currentTimeMillis())
    return true
}

private fun shouldMakeGameAdUnavailable(payload: Any?, messageType: String?): Boolean {
    if (messageType in GAME_AD_UNAVAILABLE_MESSAGE_TYPES) return true
    if (messageType !in setOf("loadadasync", "showadasync")) return false
    val content = extractGameAdContent(payload)
    val adInstanceId = content?.optString("adInstanceID")?.takeIf { it.isNotBlank() }
    val knownType = adInstanceId?.let { gameAdInstanceTypes[it] }
    if (knownType in GAME_AD_UNAVAILABLE_MESSAGE_TYPES) return true
    val placementText = listOf(
        content?.optString("placementID").orEmpty(),
        content?.optString("adType").orEmpty(),
        content?.optString("type").orEmpty(),
        content?.optString("format").orEmpty()
    ).joinToString(" ").lowercase()
    if (placementText.contains("reward")) return true
    return payload?.toString()?.lowercase()?.contains("rewarded") == true
}

fun shouldAutofixGameAdMessage(messageType: String?) = messageType in GAME_AD_AUTOFIX_MESSAGE_TYPES

private fun shouldBlockGameAdActivityLaunch(className: String): Boolean {
    return className in HARD_BLOCKED_GAME_AD_ACTIVITY_CLASS_NAMES ||
        (className in setOf(AUDIENCE_NETWORK_ACTIVITY_CLASS, AUDIENCE_NETWORK_REMOTE_ACTIVITY_CLASS) &&
         isRecentUnavailableGameAd())
}

private fun isRecentUnavailableGameAd(): Boolean {
    val rejectedAt = lastUnavailableGameAdMs.get()
    return rejectedAt > 0 && System.currentTimeMillis() - rejectedAt < GAME_AD_RECENT_WINDOW_MS
}

private fun isRecentGameAdActivityClose(): Boolean {
    val closedAt = lastGameAdActivityCloseMs.get()
    return closedAt > 0 && System.currentTimeMillis() - closedAt < 15_000L
}

private fun shouldConvertGameAdRejectToSuccess(promiseId: String, reason: String): Boolean {
    val snapshot = gameAdPromiseSnapshots[promiseId]
    if (shouldAutofixGameAdMessage(snapshot?.messageType)) return true
    val normalized = reason.lowercase()
    if (!isRecentGameAdActivityClose()) return false
    return normalized.contains("banner")
}

fun rememberGameAdPayload(target: Any?, payload: Any?, messageType: String?) {
    if (target == null || payload !is JSONObject || messageType !in GAME_AD_MESSAGE_TYPES) return
    val now = System.currentTimeMillis()
    recentGameAdTargets[target] = now
    val snapshotPayload = runCatching { JSONObject(payload.toString()) }.getOrNull() ?: payload
    extractGameAdContent(snapshotPayload)?.optString("adInstanceID")?.takeIf { it.isNotBlank() }?.let { id ->
        messageType?.let { gameAdInstanceTypes[id] = it }
    }
    extractPromiseId(snapshotPayload)?.let { promiseId ->
        gameAdPromiseSnapshots.entries.removeIf { now - it.value.timestampMs > GAME_AD_PROMISE_WINDOW_MS }
        gameAdPromiseSnapshots[promiseId] = GameAdPromiseSnapshot(snapshotPayload, messageType, now)
    }
    synchronized(recentGameAdPayloads) {
        recentGameAdPayloads.removeAll { now - it.timestampMs > GAME_AD_RECENT_WINDOW_MS }
        recentGameAdPayloads.add(GameAdPayloadSnapshot(target, snapshotPayload, messageType, now))
        while (recentGameAdPayloads.size > 20) recentGameAdPayloads.removeAt(0)
    }
}

fun completeRecentGameAdRequests(source: String) {
    val now = System.currentTimeMillis()
    val snapshots = synchronized(recentGameAdPayloads) {
        recentGameAdPayloads.removeAll { now - it.timestampMs > GAME_AD_RECENT_WINDOW_MS }
        recentGameAdPayloads.toList()
    }
    var resolved = 0
    snapshots.asReversed().forEach { s ->
        if (shouldAutofixGameAdMessage(s.messageType) && resolveGameAdPayload(s.target, s.payload, s.messageType)) {
            dispatchPostResolveGameAdSignals(s.target, s.payload, s.messageType); resolved++
        }
    }
    val targets = synchronized(recentGameAdTargets) {
        recentGameAdTargets.entries.removeIf { now - it.value > GAME_AD_RECENT_WINDOW_MS }; recentGameAdTargets.keys.toList()
    }
    targets.forEach { t -> dispatchGameEvent(t, "hidebannerad", JSONObject().put("completed", true)) }
}

private fun dispatchPostResolveGameAdSignals(target: Any?, payload: Any?, messageType: String?) {
    if (messageType in setOf("loadbanneradasync", "hidebanneradasync")) {
        val content = buildGameAdSuccessPayload(payload, messageType)
        dispatchGameEvent(target, "hidebannerad", content)
    }
}

fun buildGameAdSuccessPayload(payload: Any?, messageType: String? = null): JSONObject {
    val effectiveMessageType = messageType ?: (payload as? JSONObject)?.optString("type").orEmpty()
    val content = extractGameAdContent(payload)
    val result = JSONObject()
    val placementId    = content?.optString("placementID")?.takeIf { it.isNotBlank() }
    val requestedInstId = content?.optString("adInstanceID")?.takeIf { it.isNotBlank() }
    val bannerPosition = content?.optString("bannerPosition")?.takeIf { it.isNotBlank() }
    result.put("success", true)
    if (effectiveMessageType?.contains("reward", ignoreCase = true) == true) {
        result.put("completed", true).put("didComplete", true).put("watched", true)
              .put("rewarded", true).put("completionGesture", "post")
    }
    if (placementId != null)    result.put("placementID", placementId)
    if (bannerPosition != null) result.put("bannerPosition", bannerPosition)
    val adInstanceId = when {
        requestedInstId != null -> { gameAdInstanceIds.putIfAbsent(requestedInstId, requestedInstId); requestedInstId }
        placementId != null && effectiveMessageType != "loadbanneradasync" ->
            resolveGameAdInstanceId(placementId, effectiveMessageType, bannerPosition)
        else -> null
    }
    if (adInstanceId != null) {
        result.put("adInstanceID", adInstanceId)
        effectiveMessageType.takeIf { it.isNotBlank() }?.let { type ->
            gameAdInstanceTypes.putIfAbsent(adInstanceId, type)
        }
    }
    return result
}

private fun forceGameAdSuccessResult(promiseId: String, original: Any?, payload: JSONObject?, messageType: String?): JSONObject {
    val result = (original as? JSONObject)?.let { copyJsonObject(it) } ?: JSONObject()
    val success = buildGameAdSuccessPayload(payload ?: JSONObject().put("content", JSONObject().put("promiseID", promiseId)), messageType)
    val keys = success.keys(); while (keys.hasNext()) { val k = keys.next(); result.put(k, success.opt(k)) }
    result.put("success", true)
    if (messageType?.contains("reward", ignoreCase = true) == true)
        result.put("completed", true).put("didComplete", true).put("watched", true).put("rewarded", true).put("completionGesture", "post")
    return result
}

private fun inferGameAdMessageType(method: Method, payload: Any?): String? {
    val payloadType = (payload as? JSONObject)?.optString("type")?.takeIf { it.isNotBlank() }
    if (payloadType != null) return payloadType
    return when (method.name) {
        "D3s" -> "getinterstitialadasync"; "D3x" -> "getrewardedinterstitialasync"
        "D3z" -> "getrewardedvideoasync"; "D55" -> "hidebanneradasync"
        "D9v" -> "loadadasync"; "D9x" -> "loadbanneradasync"; "DX0" -> "showadasync"
        else -> null
    }
}

private fun gameAdPromiseTypeFromReason(reason: String): String? {
    val n = reason.lowercase()
    return when {
        n.contains("reward") && n.contains("interstitial") -> "getrewardedinterstitialasync"
        n.contains("reward") -> "getrewardedvideoasync"
        n.contains("interstitial") -> "getinterstitialadasync"
        n.contains("banner") -> "loadbanneradasync"
        n.contains("show") || n.contains("watch") || n.contains("complete") -> "showadasync"
        n.contains("load") -> "loadadasync"
        else -> null
    }
}

// ─── Activity helpers ─────────────────────────────────────────────────────────

private fun handleGameAdActivity(activity: Activity, source: String) {
    when (activity.javaClass.name) {
        AUDIENCE_NETWORK_ACTIVITY_CLASS, AUDIENCE_NETWORK_REMOTE_ACTIVITY_CLASS -> {
            forceAudienceNetworkRewardCompletion(activity, source)
            finishGameAdActivity(activity, source)
        }
        else -> finishGameAdActivity(activity, source)
    }
}

private fun buildGameAdActivityResultIntent(): Intent =
    Intent().apply { putExtra("success", true) }

private fun finishGameAdActivity(activity: Activity, source: String) {
    if (activity.isFinishing) return
    lastGameAdActivityCloseMs.set(System.currentTimeMillis())
    completeRecentGameAdRequests(source)
    if (activity.javaClass.name in GAME_AD_ACTIVITY_CLASS_NAMES) {
        activity.setResult(Activity.RESULT_OK, buildGameAdActivityResultIntent())
    } else {
        activity.setResult(Activity.RESULT_CANCELED, Intent())
    }
    activity.finish()
}

private fun forceAudienceNetworkRewardCompletion(activity: Activity, source: String) {
    if (activity.javaClass.name !in GAME_AD_ACTIVITY_CLASS_NAMES) return
    val seen = IdentityHashMap<Any, Boolean>()
    val queue = java.util.ArrayDeque<Pair<Any, Int>>()
    queue.add(activity to 0)
    var inspected = 0; var invoked = 0
    while (!queue.isEmpty() && inspected < 96) {
        val (value, depth) = queue.removeFirst()
        if (seen.put(value, true) != null) continue; inspected++
        invoked += invokeAudienceNetworkRewardCompletionMethods(value)
        if (depth >= 5 || !shouldTraverseAudienceNetworkObject(value, value === activity)) continue
        audienceNetworkFieldsFor(value.javaClass).forEach { field ->
            val fieldValue = runCatching { field.get(value) }.getOrNull() ?: return@forEach
            when (fieldValue) {
                is Iterable<*> -> fieldValue.take(12).forEach { item ->
                    if (item != null && shouldQueueAudienceNetworkObject(item)) queue.add(item to depth + 1)
                }
                is Array<*> -> fieldValue.take(12).forEach { item ->
                    if (item != null && shouldQueueAudienceNetworkObject(item)) queue.add(item to depth + 1)
                }
                else -> if (shouldQueueAudienceNetworkObject(fieldValue)) {
                    queue.add(fieldValue to depth + 1)
                }
            }
        }
    }
}

private fun invokeAudienceNetworkRewardCompletionMethods(target: Any): Int {
    var invoked = 0
    audienceNetworkMethodsFor(target.javaClass).filter { m ->
        !Modifier.isStatic(m.modifiers) && m.parameterCount == 0 &&
        (m.name in AUDIENCE_NETWORK_REWARD_COMPLETION_METHOD_NAMES || (m.name.contains("Reward", ignoreCase = true) && m.name.contains("Complete", ignoreCase = true)))
    }.forEach { m -> runCatching { m.invoke(target); invoked++ } }
    return invoked
}

private fun completeAudienceNetworkRewardObject(adObject: Any, source: String = "unknown"): Boolean {
    val listeners = LinkedHashSet<Any>()
    synchronized(audienceNetworkRewardAdListeners) { audienceNetworkRewardAdListeners[adObject]?.let { listeners.add(it) } }
    listeners.addAll(findAudienceNetworkRewardListeners(adObject))
    var invoked = 0
    listeners.forEach { listener -> invoked += invokeAudienceNetworkRewardListenerCallbacks(listener, adObject, source) }
    if (invoked > 0) { completeRecentGameAdRequests(source); return true }
    return false
}

private fun invokeAudienceNetworkRewardListenerCallbacks(listener: Any, adObject: Any, source: String): Int {
    var invoked = 0
    val methodGroups = listOf(
        setOf("onAdLoaded", "onLoggingImpression", "onInterstitialDisplayed"),
        setOf("onRewardedVideoCompleted", "onRewardedAdCompleted", "onRewardedInterstitialCompleted", "onAdComplete", "onAdCompleted"),
        setOf("onRewardedVideoClosed", "onRewardedInterstitialClosed", "onAdClosed", "onInterstitialDismissed")
    )
    methodGroups.forEach { group ->
        audienceNetworkRewardMethodsFor(listener.javaClass)
            .filter { m -> m.name in group }
            .forEach { m ->
                val args = audienceNetworkCallbackArgs(m, adObject) ?: return@forEach
                runCatching { m.invoke(listener, *args); invoked++ }
                    .onFailure {  }
            }
    }
    return invoked
}

private fun audienceNetworkCallbackArgs(method: Method, adObject: Any): Array<Any?>? =
    when (method.parameterCount) {
        0 -> emptyArray()
        1 -> { val pt = method.parameterTypes[0]; if (pt.isAssignableFrom(adObject.javaClass)) arrayOf(adObject) else null }
        else -> null
    }

/** Like audienceNetworkMethodsFor but also includes interface-declared methods — needed for listener callbacks. */
private fun audienceNetworkRewardMethodsFor(type: Class<*>): List<Method> {
    val map = LinkedHashMap<String, Method>()
    var cur: Class<*>? = type
    while (cur != null && cur != Any::class.java && cur != Activity::class.java) {
        (cur.declaredMethods + cur.methods).forEach { m ->
            if (!Modifier.isStatic(m.modifiers)) {
                m.isAccessible = true
                map.putIfAbsent("${m.name}/${m.parameterTypes.joinToString { it.name }}", m)
            }
        }
        cur = cur.superclass
    }
    return map.values.toList()
}

private fun findAudienceNetworkRewardListeners(root: Any?): List<Any> {
    if (root == null) return emptyList()
    val listeners = LinkedHashSet<Any>(); val seen = IdentityHashMap<Any, Boolean>()
    val queue = java.util.ArrayDeque<Pair<Any, Int>>(); queue.add(root to 0)
    var inspected = 0
    while (!queue.isEmpty() && inspected < 96 && listeners.size < 8) {
        val (value, depth) = queue.removeFirst()
        if (seen.put(value, true) != null) continue; inspected++
        if (value !== root && isAudienceNetworkRewardListenerObject(value)) { listeners.add(value); continue }
        if (depth >= 5 || !shouldQueueAudienceNetworkObject(value)) continue
        audienceNetworkFieldsFor(value.javaClass).forEach { f ->
            val fv = runCatching { f.get(value) }.getOrNull() ?: return@forEach
            when (fv) {
                is Iterable<*> -> fv.take(12).forEach { item -> if (item != null && (isAudienceNetworkRewardListenerObject(item) || shouldQueueAudienceNetworkObject(item))) queue.add(item to depth + 1) }
                is Array<*>    -> fv.take(12).forEach { item -> if (item != null && (isAudienceNetworkRewardListenerObject(item) || shouldQueueAudienceNetworkObject(item))) queue.add(item to depth + 1) }
                else -> if (isAudienceNetworkRewardListenerObject(fv) || shouldQueueAudienceNetworkObject(fv)) queue.add(fv to depth + 1)
            }
        }
    }
    return listeners.toList()
}

private fun rememberAudienceNetworkRewardListeners(owner: Any?, args: Array<Any?>?, method: Method) {
    if (owner == null || args == null) return
    args.forEach { arg ->
        if (arg != null && isAudienceNetworkRewardListenerObject(arg)) {
            audienceNetworkRewardAdListeners[owner] = arg
        } else {
            findAudienceNetworkRewardListeners(arg).firstOrNull()?.let { audienceNetworkRewardAdListeners[owner] = it }
        }
    }
}

private fun isAudienceNetworkRewardListenerObject(value: Any?): Boolean {
    if (value == null) return false
    val type = value.javaClass
    val cn = type.name.lowercase()
    if (cn.contains("listener") && (cn.contains("reward") || cn.contains("ad"))) return true
    if (audienceNetworkInterfacesFor(type).any { iface ->
            val ifn = iface.name.lowercase()
            ifn.contains("listener") && (ifn.contains("reward") || ifn.contains("ad"))
        }) return true
    return audienceNetworkRewardMethodsFor(type).any { m ->
        m.name in AUDIENCE_NETWORK_REWARD_COMPLETION_METHOD_NAMES ||
        m.name.contains("Reward", ignoreCase = true) ||
        m.name.contains("InterstitialDismissed", ignoreCase = true)
    }
}

private fun audienceNetworkInterfacesFor(type: Class<*>): List<Class<*>> {
    val interfaces = LinkedHashSet<Class<*>>()
    fun collect(current: Class<*>?) {
        if (current == null || current == Any::class.java) return
        current.interfaces.forEach { iface -> if (interfaces.add(iface)) collect(iface) }
        collect(current.superclass)
    }
    collect(type)
    return interfaces.toList()
}

private fun isAudienceNetworkRewardRelevantClass(className: String): Boolean {
    val n = className.lowercase()
    return (n.startsWith("com.facebook.ads.") || n.startsWith("com.facebook.audiencenetwork.") || n.contains("audiencenetwork")) &&
           (n.contains("reward") || n.contains("adlistener") || n.contains("adconfig") || n.endsWith(".ad"))
}

private fun isAudienceNetworkRewardShowMethod(clazz: Class<*>, method: Method) =
    clazz.name.lowercase().contains("reward") &&
    method.name == "show" &&
    !Modifier.isStatic(method.modifiers) &&
    method.parameterCount <= 1 &&
    (method.returnType == Void.TYPE ||
     method.returnType == Boolean::class.javaPrimitiveType ||
     method.returnType == Boolean::class.java)

private fun isAudienceNetworkRewardListenerRegistrationMethod(method: Method): Boolean {
    if (Modifier.isStatic(method.modifiers) || method.parameterCount == 0) return false
    if (method.name.lowercase().contains("listener")) return true
    return method.parameterTypes.any { t -> t.name.lowercase().contains("listener") && (t.name.lowercase().contains("reward") || t.name.lowercase().contains("ad")) }
}

private fun shouldQueueAudienceNetworkObject(value: Any): Boolean {
    val type = value.javaClass
    if (type.isPrimitive || value is String || value is Number || value is Boolean || value is CharSequence) return false
    return shouldTraverseAudienceNetworkObject(value, false)
}

private fun shouldTraverseAudienceNetworkObject(value: Any, isRootActivity: Boolean): Boolean {
    if (isRootActivity) return true
    val cn = value.javaClass.name.lowercase()
    return cn.startsWith("com.facebook.ads.") || cn.startsWith("com.facebook.audiencenetwork.") ||
           cn.contains("audiencenetwork") || cn.contains("reward") || cn.contains("interstitial") ||
           cn.contains("fullscreen") || cn.contains("listener") || cn.contains(".ads.")
}

private fun audienceNetworkFieldsFor(type: Class<*>): List<Field> {
    val list = ArrayList<Field>(); var cur: Class<*>? = type
    while (cur != null && cur != Any::class.java && cur != Activity::class.java && list.size < 48) {
        cur.declaredFields.forEach { f -> if (!Modifier.isStatic(f.modifiers) && list.size < 48) { f.isAccessible = true; list.add(f) } }; cur = cur.superclass
    }; return list
}

private fun audienceNetworkMethodsFor(type: Class<*>): List<Method> {
    val map = LinkedHashMap<String, Method>(); var cur: Class<*>? = type
    while (cur != null && cur != Any::class.java && cur != Activity::class.java) {
        cur.declaredMethods.forEach { m -> if (!Modifier.isStatic(m.modifiers)) { m.isAccessible = true; map.putIfAbsent("${cur.name}.${m.name}/${m.parameterCount}", m) } }; cur = cur.superclass
    }; return map.values.toList()
}

// ─── Native ad view helpers ───────────────────────────────────────────────────

private val GAME_AD_WEBVIEW_HIDE_SCRIPT = """
(function(){
  if (window.__nexalloyFbAdSweep) return;
  window.__nexalloyFbAdSweep = true;
  function textOf(el) {
    try { return (el.innerText || el.textContent || '').toLowerCase(); } catch (e) { return ''; }
  }
  function attrsOf(el) {
    try { return ((el.id || '') + ' ' + (el.className || '') + ' ' + (el.getAttribute('aria-label') || '') + ' ' + (el.getAttribute('src') || '')).toLowerCase(); } catch (e) { return ''; }
  }
  function nearBottom(el) {
    try {
      var r = el.getBoundingClientRect();
      return r.height > 0 && r.height < Math.max(260, window.innerHeight * 0.35) && r.bottom > window.innerHeight * 0.55;
    } catch (e) { return false; }
  }
  function isAd(el) {
    var t = textOf(el);
    var a = attrsOf(el);
    if (t.indexOf('ads served by meta') >= 0 || t.indexOf('ad choices') >= 0) return true;
    if (!nearBottom(el)) return false;
    if ((el.tagName || '').toLowerCase() === 'iframe') return true;
    return /audiencenetwork|adchoices|fbinstant.*ad|instant.*ad|banner.?ad|ad.?banner|ad-container|ad_container|sponsored/.test(a);
  }
  function hide(el) {
    try {
      var target = el;
      for (var i = 0; i < 4 && target.parentElement && nearBottom(target.parentElement); i++) target = target.parentElement;
      target.style.setProperty('display', 'none', 'important');
      target.style.setProperty('visibility', 'hidden', 'important');
      target.style.setProperty('height', '0px', 'important');
      target.style.setProperty('min-height', '0px', 'important');
      target.style.setProperty('pointer-events', 'none', 'important');
    } catch (e) {}
  }
  function sweep() {
    try {
      document.querySelectorAll('iframe, div, section, aside, [id], [class], [aria-label]').forEach(function(el) {
        if (isAd(el)) hide(el);
      });
    } catch (e) {}
  }
  sweep();
  new MutationObserver(sweep).observe(document.documentElement || document.body, {childList:true, subtree:true, attributes:true});
  setInterval(sweep, 1000);
})();
""".trimIndent()

// Use a stable resource-style int as the view tag key; View.setTag(int, Any) requires
// a resource id on some platforms, so we pick a value unlikely to collide with real ids.
private val FEED_ROW_SWEEP_TAG_KEY = "nexalloy_feed_row_sweep_id".hashCode()

private fun scheduleGameAdSurfaceSweep(view: View?, reason: String) {
    // This one walks up to rootView (the whole screen), so recycling of an inner
    // child doesn't invalidate the target the same way scheduleFeedRowSweep's
    // per-row target can — the root itself doesn't get recycled. No identity guard
    // needed here.
    val root = view?.rootView ?: view ?: return
    longArrayOf(0L, 250L, 1_000L, 2_500L, 5_000L).forEach { delayMs ->
        root.postDelayed({ if (root.isAttachedToWindow) sweepGameAdSurface(root, reason) }, delayMs)
    }
}

/** Feed rows often lazy-render their "Sponsored"/"Hide ad" markers after initial layout —
 *  schedule a short delayed sweep whenever a row is added directly under a RecyclerView. */
private fun shouldScheduleFeedRowSweep(parent: ViewGroup?, child: View?): Boolean {
    if (parent == null || child !is ViewGroup) return false
    return parent.javaClass.name.contains("RecyclerView")
}

private fun scheduleFeedRowSweep(view: View?, reason: String) {
    val subtree = view ?: return
    // RecyclerView recycles `subtree` for a different item while these delayed
    // sweeps are still pending — without an identity + attach check, a sweep that
    // fires late can hit a completely different (now-rebound) row and hide real
    // content (e.g. profile header text) instead of the ad row it was scheduled for.
    val expectedTag = System.identityHashCode(subtree)
    subtree.setTag(FEED_ROW_SWEEP_TAG_KEY, expectedTag)
    longArrayOf(60L, 500L, 1_500L, 3_000L).forEach { delayMs ->
        subtree.postDelayed({
            if (subtree.isAttachedToWindow && subtree.getTag(FEED_ROW_SWEEP_TAG_KEY) == expectedTag) {
                sweepGameAdSurface(subtree, reason)
            }
        }, delayMs)
    }
}

private fun sweepGameAdSurface(view: View?, reason: String): Boolean {
    if (view == null) return false
    var hidden = false
    if (view is WebView) injectGameAdHidingScript(view)
    if (isLikelyExplicitFeedAdCardContainer(view)) {
        hidden = hideLikelyExplicitFeedAdCardContainer(view, reason) || hidden
    }
    if (isPotentialNativeGameAdView(view) || isPotentialExplicitFeedAdMarkerView(view) ||
        (ENABLE_FEED_UI_MARKER_FALLBACKS &&
         (isPotentialFeedAdMarkerView(view) || (view is TextView && isAnyAdMarkerText(view.text))))) {
        hidden = hideLikelyAdContainer(view, reason) || hidden
    }
    if (ENABLE_FEED_UI_MARKER_FALLBACKS && isPotentialFeedReelCtaAdMarkerView(view)) {
        hidden = hideLikelyFeedReelCtaAdContainer(view, reason) || hidden
    }
    val group = view as? ViewGroup ?: return hidden
    for (i in 0 until group.childCount) {
        hidden = sweepGameAdSurface(group.getChildAt(i), reason) || hidden
    }
    return hidden
}

private fun injectGameAdHidingScript(webView: WebView) {
    webView.post { runCatching { webView.evaluateJavascript(GAME_AD_WEBVIEW_HIDE_SCRIPT, null) } }

}

private fun hideLikelyAdContainer(view: View, reason: String): Boolean {
    val root = view.rootView
    val target = when {
        shouldUseExplicitFeedMarkerCardTarget(view) -> {
            resolveLikelyExplicitFeedAdCardTarget(view) ?: run {
                return false
            }
        }
        shouldUseFeedMarkerCardTarget(view) -> {
            resolveLikelyFeedMarkerCardTarget(view) ?: run {
                return false
            }
        }
        else -> resolveLikelyAdContainerTarget(view)
    }
    return hideResolvedAdSurfaceTarget(target, view, root, reason, forceCollapseHeight = false)
}

private fun hideLikelyExplicitFeedAdCardContainer(view: View, reason: String): Boolean {
    val target = resolveLikelyExplicitFeedAdCardTarget(view) ?: return false
    return hideResolvedAdSurfaceTarget(target, view, view.rootView, "$reason explicit feed card", forceCollapseHeight = true)
}

private fun hideResolvedAdSurfaceTarget(
    target: View, source: View, root: View?, reason: String, forceCollapseHeight: Boolean
): Boolean {
    logAdSurfaceHideDebug("hideResolvedAdSurfaceTarget", target, source, reason)
    var hidden = false
    if (target.visibility != View.GONE) { target.visibility = View.GONE; hidden = true }
    target.minimumHeight = 0
    target.layoutParams?.let { params ->
        if (forceCollapseHeight || target !== source ||
            isLikelyBannerSized(target, root) || isPotentialNativeGameAdView(target) ||
            isPotentialFeedAdMarkerView(source) || isPotentialExplicitFeedAdMarkerView(source)) {
            params.height = 0; target.layoutParams = params; hidden = true
        }
    }
    target.requestLayout()
    return hidden
}

// TEMP DEBUG: remove once the mis-hide root cause is confirmed. Logs the exact view
// hierarchy state (class, size, text, contentDescription) at the moment something is
// hidden, so you can grep logcat for e.g. "1,9K" / "bạn chung" / the friend-add button's
// text and see exactly which reason/detector fired and what it matched on.
private fun logAdSurfaceHideDebug(source: String, target: View, matchedView: View, reason: String) {
    runCatching {
        val targetText = (target as? TextView)?.text?.toString().orEmpty()
        val matchedText = (matchedView as? TextView)?.text?.toString().orEmpty()
        android.util.Log.w(
            "NexAlloyAdSweepDebug",
            "[$source] reason=$reason " +
                "target=${target.javaClass.name}(${target.width}x${target.height}) text=\"$targetText\" desc=\"${target.contentDescription}\" " +
                "matchedView=${matchedView.javaClass.name} matchedText=\"$matchedText\" matchedDesc=\"${matchedView.contentDescription}\""
        )
    }
}

private fun hideLikelyFeedReelCtaAdContainer(view: View, reason: String): Boolean {
    val target = resolveLikelyFeedReelCtaAdContainerTarget(view) ?: return false
    logAdSurfaceHideDebug("hideLikelyFeedReelCtaAdContainer", target, view, reason)
    var hidden = false
    if (target.visibility != View.GONE) { target.visibility = View.GONE; hidden = true }
    target.minimumHeight = 0
    target.layoutParams?.let { params -> params.height = 0; target.layoutParams = params; hidden = true }
    target.requestLayout()
    return hidden
}

/** DISABLED: this used to walk up the parent chain purely by geometry (width/height
 *  ratios vs root) to guess a "post container" to collapse. That amplified any single
 *  false-positive match (e.g. a mistakenly-matched text/description deep in a profile
 *  header) into hiding a whole nearby block, because the walk never checked that the
 *  ancestors actually contained more ad-like content — only that their size looked
 *  roughly right. Restored to the old, narrower behavior: hide exactly the view that
 *  matched, nothing more. Re-enable only with a content-based (not size-based) check
 *  if the narrower behavior turns out to be insufficient. */
private fun resolveLikelyAdContainerTarget(view: View): View = view

private fun shouldUseFeedMarkerCardTarget(view: View): Boolean =
    isPotentialFeedAdMarkerView(view) || (view is TextView && isFeedAdMarkerText(view.text))

private fun shouldUseExplicitFeedMarkerCardTarget(view: View): Boolean =
    isPotentialExplicitFeedAdMarkerView(view) || (view is TextView && isExplicitFeedAdMarkerText(view.text))

private data class ExplicitFeedAdCardSignals(
    val hasHideAd: Boolean, val hasAdLabel: Boolean, val hasSharedLink: Boolean, val hasStrongCta: Boolean
)

/** DISABLED: previously walked UP from the marker view to find the "full post card"
 *  ancestor to collapse (picking the largest matching ancestor). Reverted to hiding
 *  exactly the matched view — same reasoning as resolveLikelyAdContainerTarget above:
 *  climbing ancestors by geometry/signal-scan can grab an unrelated nearby block
 *  (e.g. a profile header) instead of the actual ad, and that mismatch is what caused
 *  text to disappear. Re-enable only with a stricter, content-verified walk if needed. */
private fun resolveLikelyExplicitFeedAdCardTarget(view: View): View? = view

/** DISABLED: see resolveLikelyExplicitFeedAdCardTarget above — same reasoning applies.
 *  This one was purely geometry-based (isSafeFeedMarkerCardCandidate never checked
 *  actual ad content on the ancestor), making it the least safe of the three walks. */
private fun resolveLikelyFeedMarkerCardTarget(view: View): View? = view

/** Bounding-box safety check to avoid the broad feed-marker fallback collapsing
 *  something that isn't a full post card (e.g. a toolbar or a comment row). */
private fun isSafeFeedMarkerCardCandidate(view: View, rootWidth: Int, rootHeight: Int): Boolean {
    val width = view.width; val height = view.height
    if (width < (rootWidth * 0.82f).toInt()) return false
    if (height < maxOf(360, (rootHeight * 0.18f).toInt())) return false
    if (height > (rootHeight * 0.82f).toInt()) return false
    val location = IntArray(2)
    val topOnScreen = runCatching { view.getLocationOnScreen(location); location[1] }.getOrDefault(view.top)
    val bottomOnScreen = topOnScreen + height
    if (topOnScreen < (rootHeight * 0.04f).toInt()) return false
    if (bottomOnScreen > (rootHeight * 0.96f).toInt()) return false
    return true
}

private fun isLikelyExplicitFeedAdCardContainer(view: View): Boolean {
    val root = view.rootView ?: return false
    val rootWidth  = root.width.takeIf  { it > 0 } ?: return false
    val rootHeight = root.height.takeIf { it > 0 } ?: return false
    return isLikelyExplicitFeedAdCardContainer(view, rootWidth, rootHeight)
}

private fun isLikelyExplicitFeedAdCardContainer(view: View, rootWidth: Int, rootHeight: Int): Boolean {
    if (view !is ViewGroup) return false
    val width = view.width; val height = view.height
    if (width < (rootWidth * 0.82f).toInt()) return false
    if (height < maxOf(420, (rootHeight * 0.18f).toInt())) return false
    if (height > (rootHeight * 0.96f).toInt()) return false
    val location = IntArray(2)
    val topOnScreen = runCatching { view.getLocationOnScreen(location); location[1] }.getOrDefault(view.top)
    val bottomOnScreen = topOnScreen + height
    if (topOnScreen < (rootHeight * 0.04f).toInt()) return false
    if (bottomOnScreen > (rootHeight * 0.98f).toInt()) return false
    val signals = collectExplicitFeedAdCardSignals(view)
    return signals.hasHideAd && (signals.hasAdLabel || signals.hasSharedLink || signals.hasStrongCta)
}

/** Requires "Hide ad" PLUS at least one of: AdChoices label, "Shared link:", or a
 *  strong CTA phrase ("Learn More", "Shop Now"...) — this combination is what makes
 *  the explicit detector safe to leave always-on (low false positive rate). */
private fun collectExplicitFeedAdCardSignals(root: View): ExplicitFeedAdCardSignals {
    val queue = java.util.ArrayDeque<View>(); queue.add(root)
    var visited = 0
    var hasHideAd = false; var hasAdLabel = false; var hasSharedLink = false; var hasStrongCta = false
    while (queue.isNotEmpty() && visited < 192 && !(hasHideAd && (hasAdLabel || hasSharedLink || hasStrongCta))) {
        val view = queue.removeFirst(); visited++
        for (marker in collectViewMarkerTexts(view)) {
            val normalized = marker.lowercase()
            if (!hasHideAd && normalized.contains("hide ad")) hasHideAd = true
            if (!hasAdLabel && isExplicitFeedAdMarkerText(normalized)) hasAdLabel = true
            if (!hasSharedLink && normalized.contains("shared link:")) hasSharedLink = true
            if (!hasStrongCta && isExplicitFeedAdCtaText(normalized)) hasStrongCta = true
        }
        val group = view as? ViewGroup ?: continue
        for (i in 0 until group.childCount) queue.addLast(group.getChildAt(i))
    }
    return ExplicitFeedAdCardSignals(hasHideAd, hasAdLabel, hasSharedLink, hasStrongCta)
}

/** DISABLED: same reasoning as resolveLikelyExplicitFeedAdCardTarget above — this
 *  walked up looking for the reel-CTA-shaped ancestor by geometry + signal-scan.
 *  Reverted to hiding exactly the matched view. */
private fun resolveLikelyFeedReelCtaAdContainerTarget(view: View): View? = view

private data class FeedReelCtaAdSignals(
    val hasSharedLink: Boolean, val hasSendMessageCta: Boolean,
    val hasReelSurface: Boolean, val hasLeadGenPrompt: Boolean
)

private fun isLikelyFeedReelCtaAdContainer(view: View, rootWidth: Int, rootHeight: Int): Boolean {
    val width = view.width; val height = view.height
    if (width < (rootWidth * 0.82f).toInt()) return false
    if (height < (rootHeight * 0.45f).toInt() || height > (rootHeight * 0.92f).toInt()) return false
    val location = IntArray(2)
    val topOnScreen = runCatching { view.getLocationOnScreen(location); location[1] }.getOrDefault(view.top)
    if (topOnScreen < (rootHeight * 0.08f).toInt()) return false
    val signals = collectFeedReelCtaAdSignals(view)
    return signals.hasSharedLink && signals.hasSendMessageCta && (signals.hasReelSurface || signals.hasLeadGenPrompt)
}

private fun collectFeedReelCtaAdSignals(root: View): FeedReelCtaAdSignals {
    val queue = java.util.ArrayDeque<View>(); queue.add(root)
    var visited = 0
    var hasSharedLink = false; var hasSendMessageCta = false; var hasReelSurface = false; var hasLeadGenPrompt = false
    while (queue.isNotEmpty() && visited < 128 &&
           !(hasSharedLink && hasSendMessageCta && (hasReelSurface || hasLeadGenPrompt))) {
        val view = queue.removeFirst(); visited++
        val className = view.javaClass.name.lowercase()
        val contentDescription = view.contentDescription?.toString().orEmpty().lowercase()
        val text = (view as? TextView)?.text?.toString().orEmpty().lowercase()
        val marker = "$className $contentDescription $text"
        if (!hasSharedLink && marker.contains("shared link:")) hasSharedLink = true
        if (!hasSendMessageCta && marker.contains("send message")) hasSendMessageCta = true
        if (!hasLeadGenPrompt && (marker.contains("your business") || marker.contains("your ad"))) hasLeadGenPrompt = true
        if (!hasReelSurface && (marker.contains("reel") || className.contains("surfaceview") ||
                                 className.contains("textureview") || className.contains("videoview"))) hasReelSurface = true
        val group = view as? ViewGroup ?: continue
        for (i in 0 until group.childCount) queue.addLast(group.getChildAt(i))
    }
    return FeedReelCtaAdSignals(hasSharedLink, hasSendMessageCta, hasReelSurface, hasLeadGenPrompt)
}

/** Collects visible text + accessibility text/content-description — Meta's ad labels
 *  are sometimes accessibility-only (invisible on screen but read by screen readers
 *  and inspectable here), which is why both sources are checked. */
private fun collectViewMarkerTexts(view: View?): List<String> {
    if (view == null) return emptyList()
    val values = LinkedHashSet<String>()
    view.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(values::add)
    (view as? TextView)?.text?.toString()?.takeIf { it.isNotBlank() }?.let(values::add)
    runCatching {
        val info = view.createAccessibilityNodeInfo() ?: return@runCatching
        try {
            info.text?.toString()?.takeIf { it.isNotBlank() }?.let(values::add)
            info.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let(values::add)
        } finally { info.recycle() }
    }
    return values.toList()
}

private fun isPotentialFeedAdMarkerView(view: View?): Boolean {
    if (view == null) return false
    return collectViewMarkerTexts(view).any(::isFeedAdMarkerText)
}

private fun isPotentialExplicitFeedAdMarkerView(view: View?): Boolean {
    if (view == null) return false
    return collectViewMarkerTexts(view).any(::isExplicitFeedAdMarkerText)
}

private fun isPotentialFeedReelCtaAdMarkerView(view: View?): Boolean {
    if (view == null) return false
    return collectViewMarkerTexts(view).any(::isFeedReelCtaAdMarkerText)
}

private fun isAnyAdMarkerText(value: CharSequence?): Boolean =
    isGameAdMarkerText(value) || isFeedAdMarkerText(value)

private fun isFeedAdMarkerText(value: CharSequence?): Boolean {
    if (value.isNullOrBlank()) return false
    val normalized = value.toString().lowercase()
    return FEED_SURFACE_AD_MARKER_TOKENS.any { token ->
        if (token == "ad\u2022") AD_BULLET_MARKER_REGEX.containsMatchIn(normalized)
        else normalized.contains(token)
    }
}

// "ad\u2022" needs a word boundary before "ad" — otherwise it also matches any text
// that merely ends in the letters "ad" right before a bullet (e.g. a Vietnamese
// number/label string that happens to end that way), which was hiding real profile
// header content that had nothing to do with an actual ad label.
private val AD_BULLET_MARKER_REGEX = Regex("(?:^|[^\\p{L}\\p{N}])ad\\u2022")

private fun isExplicitFeedAdMarkerText(value: CharSequence?): Boolean {
    // TEST 2 - DISABLED: short-circuit to false to check if this always-on detector
    // (not gated by ENABLE_FEED_UI_MARKER_FALLBACKS) is the source of the mis-hide.
    // Restore the block below (uncomment + remove the early return) once confirmed.
    return false
    // if (value.isNullOrBlank()) return false
    // val normalized = value.toString().lowercase()
    // return EXPLICIT_FEED_CARD_AD_MARKER_TOKENS.any { token ->
    //     if (token == "ad\u2022") AD_BULLET_MARKER_REGEX.containsMatchIn(normalized)
    //     else normalized.contains(token)
    // }
}

private fun isExplicitFeedAdCtaText(value: CharSequence?): Boolean {
    if (value.isNullOrBlank()) return false
    val normalized = value.toString().lowercase()
    return EXPLICIT_FEED_AD_CTA_TOKENS.any { token -> normalized.contains(token) }
}

private fun isFeedReelCtaAdMarkerText(value: CharSequence?): Boolean {
    if (value.isNullOrBlank()) return false
    val normalized = value.toString().lowercase()
    return FEED_REEL_CTA_AD_MARKER_TOKENS.any { token -> normalized.contains(token) }
}

private fun isLikelyBannerSized(view: View, root: View?): Boolean {
    val rootHeight = root?.height?.takeIf { it > 0 } ?: return view.height in 1..360
    val height = view.height
    if (height <= 0 || height > maxOf(360, rootHeight / 3)) return false
    val location = IntArray(2)
    return runCatching {
        view.getLocationOnScreen(location)
        location[1] + height > rootHeight / 2
    }.getOrDefault(true)
}

private fun isPotentialNativeGameAdView(view: View?): Boolean {
    val cn = view?.javaClass?.name?.lowercase() ?: return false
    return cn == "com.facebook.ads.adview" || (cn.endsWith(".adview") && (cn.startsWith("com.facebook.ads.") || cn.contains("audiencenetwork"))) || cn.contains("adchoices")
}

private fun isGameAdMarkerText(value: CharSequence?): Boolean {
    if (value.isNullOrBlank()) return false
    val n = value.toString().lowercase()
    return n.contains("ads served by meta") || n.contains("ad choices") || n.contains("adchoices")
}

// ─── Resolve / reject helpers ─────────────────────────────────────────────────

private fun resolveGameAdResolveMethod(type: Class<*>?): Method? {
    if (type == null) return null
    val candidates = (type.declaredMethods + type.methods).filter { m ->
        !Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE && m.parameterCount == 2 &&
        m.parameterTypes[0] == String::class.java && !m.parameterTypes[1].isPrimitive
    }
    return (candidates.firstOrNull { it.parameterTypes[1] == Any::class.java }
        ?: candidates.firstOrNull { JSONObject::class.java.isAssignableFrom(it.parameterTypes[1]) }
        ?: candidates.firstOrNull())?.apply { isAccessible = true }
}

private fun resolveGameAdBridgeRejectMethod(type: Class<*>?): Method? {
    if (type == null) return null
    return (type.declaredMethods + type.methods).firstOrNull { m ->
        !Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE && m.parameterCount == 3 &&
        m.parameterTypes[0] == String::class.java && m.parameterTypes[1] == String::class.java && m.parameterTypes[2] == JSONObject::class.java
    }?.apply { isAccessible = true }
}

private fun resolveGameAdRejectMethod(type: Class<*>?): Method? {
    if (type == null) return null
    return (type.declaredMethods + type.methods).firstOrNull { m ->
        !Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE && m.parameterCount == 3 && m.parameterTypes.all { it == String::class.java }
    }?.apply { isAccessible = true }
}

private fun dispatchGameEvent(target: Any?, eventType: String, content: Any?): Boolean {
    if (target == null) return false
    val method = resolveGameEventDispatchMethod(target.javaClass) ?: return false
    val eventValue = resolveGameEventValue(method.parameterTypes[0], eventType) ?: return false
    return runCatching { method.invoke(target, eventValue, content ?: JSONObject.NULL); true }.getOrElse { false }
}

private fun resolveGameEventDispatchMethod(type: Class<*>?): Method? {
    if (type == null) return null
    return (type.declaredMethods + type.methods).firstOrNull { m ->
        !Modifier.isStatic(m.modifiers) && m.returnType == Void.TYPE && m.parameterCount == 2 &&
        m.parameterTypes[0] != String::class.java && m.parameterTypes[1] == Any::class.java
    }?.apply { isAccessible = true }
}

private fun resolveGameEventValue(eventType: Class<*>, eventName: String): Any? {
    val valuesMethod = (eventType.declaredMethods + eventType.methods).firstOrNull { m ->
        Modifier.isStatic(m.modifiers) && m.parameterCount == 0 && m.returnType.isArray && m.returnType.componentType == eventType
    }?.apply { isAccessible = true }
    val values = runCatching { valuesMethod?.invoke(null) as? Array<*> }.getOrNull().orEmpty()
    values.firstOrNull { it?.toString() == eventName }?.let { return it }
    return eventType.declaredFields.firstOrNull { f ->
        Modifier.isStatic(f.modifiers) && f.type == eventType &&
        runCatching { f.isAccessible = true; f.get(null)?.toString() == eventName }.getOrDefault(false)
    }?.let { f -> runCatching { f.get(null) }.getOrNull() }
}

fun extractPromiseId(payload: Any?): String? {
    val jClass = payload?.javaClass ?: return null
    if (jClass.name != "org.json.JSONObject") return null
    val getJSONObject = (jClass.declaredMethods + jClass.methods).firstOrNull { m -> m.name == "getJSONObject" && m.parameterCount == 1 && m.parameterTypes[0] == String::class.java }?.apply { isAccessible = true } ?: return null
    val getString = (jClass.declaredMethods + jClass.methods).firstOrNull { m -> m.name == "getString" && m.parameterCount == 1 && m.parameterTypes[0] == String::class.java }?.apply { isAccessible = true } ?: return null
    val content = runCatching { getJSONObject.invoke(payload, "content") }.getOrNull() ?: return null
    return runCatching { getString.invoke(content, "promiseID") as? String }.getOrNull()
}

private fun extractGameAdContent(payload: Any?): JSONObject? = (payload as? JSONObject)?.optJSONObject("content")

private fun buildGameAdPayloadFromServiceBundle(bundle: Bundle, messageType: String): JSONObject =
    JSONObject().put("type", messageType).put("content", bundleToJsonObject(bundle))

private fun bundleToJsonObject(bundle: Bundle): JSONObject {
    val json = JSONObject()
    runCatching { bundle.keySet().toList() }.getOrDefault(emptyList()).forEach { key ->
        val value = runCatching { bundle.get(key) }.getOrNull()
        when (value) {
            null            -> json.put(key, JSONObject.NULL)
            is String       -> json.put(key, value)
            is Boolean      -> json.put(key, value)
            is Number       -> json.put(key, value)
            is JSONObject   -> json.put(key, value)
            is org.json.JSONArray -> json.put(key, value)
            is Bundle       -> json.put(key, bundleToJsonObject(value))
            else            -> json.put(key, value.toString())
        }
    }
    return json
}

private fun resolveGameAdInstanceId(placementId: String, messageType: String?, bannerPosition: String?): String {
    val key = listOf(messageType.orEmpty(), placementId, bannerPosition.orEmpty()).joinToString("|")
    return gameAdInstanceIds.computeIfAbsent(key) { "${GAME_AD_SUCCESS_INSTANCE_PREFIX}_${key.hashCode().toLong() and 0xffffffffL}" }
}

private fun copyJsonObject(source: JSONObject): JSONObject {
    val result = JSONObject(); val keys = source.keys()
    while (keys.hasNext()) { val k = keys.next(); result.put(k, source.opt(k)) }; return result
}

// ─── List / result manipulation helpers ──────────────────────────────────────

fun filterAdItems(list: MutableList<Any?>, inspector: AdStoryInspector): Int {
    var removed = 0; val it = list.iterator()
    while (it.hasNext()) { if (inspector.containsAdStory(it.next())) { it.remove(); removed++ } }; return removed
}

fun buildImmutableListLike(sample: Any?, items: List<Any?>): Any? {
    if (sample == null) return null
    return runCatching {
        val cl = Class.forName("com.google.common.collect.ImmutableList", false, sample.javaClass.classLoader)
        cl.getDeclaredMethod("copyOf", Iterable::class.java).invoke(null, items)
    }.getOrNull()
}

fun replaceFeedItemsInResult(param: XC_MethodHook.MethodHookParam, items: List<Any?>): Boolean {
    val result = param.result ?: return false; val rebuilt = rebuildFeedResult(result, items) ?: return false
    param.result = rebuilt; return true
}

private fun rebuildFeedResult(result: Any, items: List<Any?>): Any? {
    val type = result.javaClass
    val fields = runCatching { type.declaredFields.onEach { it.isAccessible = true } }.getOrNull() ?: return null
    val listField    = fields.firstOrNull { !Modifier.isStatic(it.modifiers) && Iterable::class.java.isAssignableFrom(it.type) } ?: return null
    val intArrayField = fields.firstOrNull { !Modifier.isStatic(it.modifiers) && it.type == IntArray::class.java } ?: return null
    val intFields    = fields.filter { !Modifier.isStatic(it.modifiers) && it.type == Int::class.javaPrimitiveType }
    if (intFields.size < 3) return null
    val originalList = runCatching { listField.get(result) }.getOrNull()
    val rebuiltList  = buildImmutableListLike(originalList, items) ?: return null
    val stats        = runCatching { intArrayField.get(result) as? IntArray }.getOrNull()?.clone() ?: return null
    val ints         = intFields.map { f -> runCatching { f.getInt(result) }.getOrNull() ?: return null }
    val ctor = type.declaredConstructors.firstOrNull { c ->
        c.parameterCount == 5 && c.parameterTypes.getOrNull(0)?.name == "com.google.common.collect.ImmutableList" &&
        c.parameterTypes.getOrNull(1) == IntArray::class.java && c.parameterTypes.drop(2).all { it == Int::class.javaPrimitiveType }
    } ?: return null
    ctor.isAccessible = true
    return runCatching { ctor.newInstance(rebuiltList, stats, ints[0], ints[1], ints[2]) }.getOrNull()
}

fun extractFeedItemsFromResult(result: Any?): Iterable<*>? {
    if (result == null) return null
    if (result is Iterable<*>) return result
    return runCatching {
        val f = result.javaClass.declaredFields.firstOrNull { Iterable::class.java.isAssignableFrom(it.type) } ?: return null
        f.isAccessible = true; f.get(result) as? Iterable<*>
    }.getOrNull()
}

// ─── Sponsored pool result type helpers ──────────────────────────────────────

fun isSponsoredResultCarrier(type: Class<*>): Boolean {
    val ctor = type.declaredConstructors.firstOrNull { it.parameterCount == 2 } ?: return false
    val reasonType = ctor.parameterTypes.getOrNull(1) ?: return false
    return reasonType.enumConstants?.any { it.toString() == "SPONSORED_GET_NEXT_RETURN_NULL" } == true
}

fun buildSponsoredEmptyResult(type: Class<*>): Any? {
    val ctor = type.declaredConstructors.firstOrNull { it.parameterCount == 2 } ?: return null
    val reasonType = ctor.parameterTypes.getOrNull(1) ?: return null
    val emptyReason = reasonType.enumConstants?.firstOrNull { it.toString() == "SPONSORED_GET_NEXT_RETURN_NULL" }
        ?: reasonType.enumConstants?.firstOrNull { it.toString() == "FAIL" } ?: return null
    ctor.isAccessible = true; return runCatching { ctor.newInstance(null, emptyReason) }.getOrNull()
}

// ─── Litho render method detection ───────────────────────────────────────────

fun resolveLithoRenderMethod(componentClass: Class<*>): Method? =
    componentClass.declaredMethods.firstOrNull { m ->
        !Modifier.isStatic(m.modifiers) && !m.isBridge && !m.isSynthetic && m.parameterCount == 1 &&
        !m.returnType.isPrimitive && m.returnType != Void.TYPE && m.returnType != Any::class.java &&
        m.returnType.isAssignableFrom(componentClass)
    }?.apply { isAccessible = true }

// ─── Story ad provider resolution ────────────────────────────────────────────

// ─── Instream banner eligibility resolution ───────────────────────────────────
// Mirrors upstream resolveInstreamBannerEligibilityMethod: prefer a non-static
// boolean()/0-param method declared on (or inherited by) the resolved candidate
// class; if none exists there, walk up the superclass chain looking for one
// declared directly on an ancestor. Needs a real Class<*> (classLoader), so —
// like the list-builder append/factory methods above — this runs here rather
// than as a DexKit fingerprint.
fun resolveInstreamBannerEligibilityMethod(candidateClass: Class<*>): Method? {
    (candidateClass.declaredMethods + candidateClass.methods)
        .firstOrNull { m ->
            !Modifier.isStatic(m.modifiers) &&
            m.returnType == Boolean::class.javaPrimitiveType &&
            m.parameterCount == 0
        }
        ?.apply { isAccessible = true }
        ?.let { return it }

    var current: Class<*>? = candidateClass.superclass
    while (current != null && current != Any::class.java) {
        current.declaredMethods.firstOrNull { m ->
            !Modifier.isStatic(m.modifiers) &&
            m.returnType == Boolean::class.javaPrimitiveType &&
            m.parameterCount == 0
        }?.let { it.isAccessible = true; return it }
        current = current.superclass
    }
    return null
}

fun resolveStoryAdProviderHooks(
    providerClass: Class<*>,
    includeInsertionTrigger: Boolean,
    insertionTriggerMethod: Method? = null
): StoryAdProviderHooks {
    val methods = providerClass.declaredMethods + providerClass.methods
    // NOTE: upstream's DexKit matchers for all three methods below never filtered on
    // static vs. instance — only on shape (return type + param types) — so we don't
    // either, to avoid silently missing a method that happens to be static.
    val mergeMethod = methods.firstOrNull { m ->
        m.parameterCount == 3 &&
        m.returnType.name == "com.google.common.collect.ImmutableList" &&
        m.parameterTypes[0].name == "com.facebook.auth.usersession.FbUserSession" &&
        m.parameterTypes[2].name == "com.google.common.collect.ImmutableList"
    }?.apply { isAccessible = true }
    val fetchMoreAdsMethod = methods.firstOrNull { m ->
        m.parameterCount == 2 && m.returnType == Void.TYPE &&
        m.parameterTypes[0].name == "com.google.common.collect.ImmutableList" &&
        m.parameterTypes[1] == Int::class.javaPrimitiveType
    }?.apply { isAccessible = true }
    val deferredUpdateMethod = methods.firstOrNull { m ->
        m.parameterCount == 2 && m.returnType == Void.TYPE &&
        m.parameterTypes[1].name == "com.google.common.collect.ImmutableList"
    }?.apply { isAccessible = true }
    // insertionTriggerMethod is resolved via DexKit fingerprint (usingStrings("ads_insertion"))
    // and passed in directly — avoids picking wrong 0-param void method via broad reflection
    val resolvedInsertionTrigger = if (includeInsertionTrigger) insertionTriggerMethod else null
    return StoryAdProviderHooks(providerClass, mergeMethod, fetchMoreAdsMethod, deferredUpdateMethod, resolvedInsertionTrigger)
}
