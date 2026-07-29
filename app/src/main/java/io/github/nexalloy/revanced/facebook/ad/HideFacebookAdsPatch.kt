package io.github.nexalloy.revanced.facebook.ad

import android.os.Bundle
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import io.github.nexalloy.revanced.facebook.AdStoryInspector
import io.github.nexalloy.revanced.facebook.AUDIENCE_NETWORK_ACTIVITY_CLASS
import io.github.nexalloy.revanced.facebook.GAME_AD_ACTIVITY_CLASS_NAMES
import io.github.nexalloy.revanced.facebook.AUDIENCE_NETWORK_REMOTE_ACTIVITY_CLASS
import io.github.nexalloy.revanced.facebook.AUDIENCE_NETWORK_EXPORTED_ACTIVITY_CLASS
import io.github.nexalloy.revanced.facebook.FeedCsrFilterHook
import io.github.nexalloy.revanced.facebook.FeedItemInspector
import io.github.nexalloy.revanced.facebook.FeedListSanitizerHook
import io.github.nexalloy.revanced.facebook.NEKO_PLAYABLE_ACTIVITY_CLASS
import io.github.nexalloy.revanced.facebook.hookAudienceNetworkRewardFallbacks
import io.github.nexalloy.revanced.facebook.hookFeedCsrFilterInput
import io.github.nexalloy.revanced.facebook.hookGameAdActivityLaunchFallbacks
import io.github.nexalloy.revanced.facebook.hookGameAdBridge
import io.github.nexalloy.revanced.facebook.hookGameAdRequest
import io.github.nexalloy.revanced.facebook.hookGameAdResultMethods
import io.github.nexalloy.revanced.facebook.hookGameAdServiceDispatchMethods
// import io.github.nexalloy.revanced.facebook.hookGlobalGameAdActivityLifecycleFallback
// import io.github.nexalloy.revanced.facebook.hookGlobalGameAdSurfaceFallbacks
import io.github.nexalloy.revanced.facebook.hookIndicatorPillAdEligibility
import io.github.nexalloy.revanced.facebook.hookInstreamBannerEligibility
import io.github.nexalloy.revanced.facebook.hookLateFeedListSanitizer
import io.github.nexalloy.revanced.facebook.hookListBuilderAppend
import io.github.nexalloy.revanced.facebook.hookListResultFilter
import io.github.nexalloy.revanced.facebook.hookPlayableAdActivity
import io.github.nexalloy.revanced.facebook.hookPluginPackFallback
import io.github.nexalloy.revanced.facebook.hookReelsBannerRender
import io.github.nexalloy.revanced.facebook.hookSponsoredPoolAdd
import io.github.nexalloy.revanced.facebook.hookSponsoredPoolListMethods
import io.github.nexalloy.revanced.facebook.hookSponsoredPoolResultMethods
import io.github.nexalloy.revanced.facebook.hookSponsoredStoryListMethods
import io.github.nexalloy.revanced.facebook.hookSponsoredStoryNext
import io.github.nexalloy.revanced.facebook.hookStoryAdProvider
import io.github.nexalloy.revanced.facebook.hookStoryPoolAdd
import io.github.nexalloy.revanced.facebook.resolveListBuilderAppendMethod
import io.github.nexalloy.revanced.facebook.resolveListBuilderFactoryMethod
import io.github.nexalloy.revanced.facebook.resolveInstreamBannerEligibilityMethod
import io.github.nexalloy.revanced.facebook.resolveStoryAdProviderHooks

/**
 * Master patch – ports all FacebookAppAdsRemover hooks into NexAlloy.
 *
 * Synced with upstream commit that:
 *  - Adds MarketplaceAdsPluginPack blocking
 *  - Adds hidebanneradasync message type
 *  - Splits fix strategy: banner → autofix, rewarded/interstitial → ADS_UNAVAILABLE
 *  - Adds hookGameAdResultMethods + hookGameAdServiceDispatchMethods (deeper bridge hooks)
 *  - Adds hookGlobalGameAdSurfaceFallbacks (native ad view / WebView / TextView)
 *  - Adds hookAudienceNetworkRewardFallbacks (reward completion callbacks)
 *  - Sets RESULT_OK (not RESULT_CANCELED) when finishing game ad activities
 *  - Changes storyAdsInDisc search string to "ads_deletion"
 */
val HideFacebookAds = patch(
    name = "Hide Facebook ads",
    description = "Removes sponsored feed stories, Reels ads, game ads, and banner ads.",
) {
    // ── 1. Ad-kind enum & Reels list-builder ─────────────────────────────────

    val adKindEnumClass = ::adKindEnumFingerprint.clazz
    val storyInspector  = AdStoryInspector(adKindEnumClass)

    // listBuilderClass itself is DexKit-cached; the specific append/factory method on
    // it is then picked via a plain-reflection scoring heuristic (no DexKit search),
    // matching upstream's more flexible (non-rigid-param-shape) resolution.
    val listBuilderClass = ::listBuilderClassFingerprint.clazz

    hookListBuilderAppend(resolveListBuilderAppendMethod(listBuilderClass), storyInspector)

    resolveListBuilderFactoryMethod(listBuilderClass)?.let { factoryMethod ->
        hookListResultFilter(factoryMethod, "list factory", storyInspector)
    }

    // Both FbShortsViewerPluginPack and MarketplaceAdsPluginPack
    ::pluginPackMethodsFingerprint.dexMethodList.forEach { dm ->
        hookPluginPackFallback(dm.toMethod(), storyInspector)
    }

    // ── 2. Instream banner & indicator pill ───────────────────────────────────

    resolveInstreamBannerEligibilityMethod(::instreamBannerEligibilityClassFingerprint.clazz)
        ?.let { hookInstreamBannerEligibility(it) }

    hookIndicatorPillAdEligibility(::indicatorPillAdEligibilityFingerprint.method)

    // ── 3. Reels banner Litho render ──────────────────────────────────────────

    ::reelsBannerRenderMethodsFingerprint.dexMethodList.forEach { dm ->
        hookReelsBannerRender(dm.toMethod())
    }

    // ── 4. Feed CSR cache filter ──────────────────────────────────────────────

    val storyPoolAddMethods = ::storyPoolAddMethodsFingerprint.dexMethodList.map { dm ->
        dm.toMethod()
    }
    val feedItemInspector = FeedItemInspector(storyPoolAddMethods.map { it.parameterTypes[0] })

    ::feedCsrFilterMethodsFingerprint.dexMethodList.forEach { dm ->
        val method = dm.toMethod()
        val listArgIndex = method.parameterTypes.indexOfFirst {
            it.name == "com.google.common.collect.ImmutableList"
        }.coerceAtLeast(0)
        hookFeedCsrFilterInput(FeedCsrFilterHook(method, listArgIndex), feedItemInspector)
    }

    // ── 5. Late feed list sanitisers ──────────────────────────────────────────

    ::lateFeedListMethodsFingerprint.dexMethodList.forEach { dm ->
        val method = dm.toMethod()
        val listArgIndex = method.parameterTypes.indexOfFirst {
            it.name == "com.google.common.collect.ImmutableList"
        }.coerceAtLeast(0)
        hookLateFeedListSanitizer(FeedListSanitizerHook(method, listArgIndex), feedItemInspector)
    }

    // ── 6. Story pool add ─────────────────────────────────────────────────────

    storyPoolAddMethods.forEach { method ->
        hookStoryPoolAdd(method, feedItemInspector)
    }

    // ── 7. Sponsored pool ─────────────────────────────────────────────────────

    hookSponsoredPoolAdd(::sponsoredPoolAddMethodFingerprint.method)

    hookSponsoredStoryNext(::sponsoredStoryNextMethodFingerprint.method)

    hookSponsoredStoryListMethods(::sponsoredStoryManagerClassFingerprint.clazz)

    run {
        val poolClass = ::sponsoredPoolClassFingerprint.clazz
        hookSponsoredPoolListMethods(poolClass)
        hookSponsoredPoolResultMethods(poolClass)
    }

    // ── 8. Story ad provider (in-disc) ────────────────────────────────────────

    run {
        val insertionTrigger = ::storyAdsInsertionTriggerMethodFingerprint.method
        hookStoryAdProvider(resolveStoryAdProviderHooks(::storyAdsInDiscClassFingerprint.clazz, true, insertionTrigger))
    }

    // ── 9. Game ad requests + bridge ─────────────────────────────────────────

    val gameAdMethods = ::gameAdRequestMethodsFingerprint.dexMethodList.map { dm ->
        dm.toMethod()
    }

    gameAdMethods.forEach { m ->
        hookGameAdRequest(m)
    }

    // postMessage bridge
    gameAdMethods.firstOrNull()?.let { firstMethod ->
        firstMethod.declaringClass.declaredMethods
            .firstOrNull { m -> m.name == "postMessage" && m.parameterCount == 2 && m.parameterTypes.all { it == String::class.java } }
            ?.apply { isAccessible = true }
            ?.let { hookGameAdBridge(it) }
    }

    // ── 10. Deeper bridge hooks (resolve / reject / service dispatch) ─────────

    gameAdMethods.firstOrNull()?.declaringClass?.let { bridgeClass ->
        hookGameAdResultMethods(bridgeClass)
        hookGameAdServiceDispatchMethods(bridgeClass)
    }

    // ── 11. Audience Network reward fallbacks ─────────────────────────────────

    hookAudienceNetworkRewardFallbacks(classLoader)

    // ── 12. Activity lifecycle hooks ──────────────────────────────────────────

    fun loadOrNull(name: String): Class<*>? = runCatching { classLoader.loadClass(name) }.getOrNull()

    fun entryPointOf(clazz: Class<*>): java.lang.reflect.Method? =
        (clazz.declaredMethods + clazz.methods).firstOrNull { m ->
            (m.name == "onResume" && m.parameterCount == 0) ||
            (m.name == "onStart" && m.parameterCount == 0) ||
            (m.name == "onCreate" && m.parameterCount == 1 && m.parameterTypes[0] == Bundle::class.java)
        }?.apply { isAccessible = true }

    // NekoPlayableAdActivity
    loadOrNull(NEKO_PLAYABLE_ACTIVITY_CLASS)?.declaredMethods
        ?.firstOrNull { m -> m.name == "onResume" && m.parameterCount == 0 }
        ?.apply { isAccessible = true }
        ?.let { hookPlayableAdActivity(it) }

    // AudienceNetwork activities — one entry-point method per class (first match
    // among onResume / onStart / onCreate(Bundle)), matching upstream's
    // resolveGameAdUiActivityMethods. Falls back to a broader Activity-subclass
    // scan across GAME_AD_ACTIVITY_CLASS_NAMES if neither AN class yields a hook.
    // AUDIENCE_NETWORK_ACTIVITY_CLASS no longer exists on FB 571+; the exported
    // variant replaces it. Every lookup is optional so one missing class cannot
    // take the whole patch down.
    val gameAdUiHooked = java.util.LinkedHashMap<String, java.lang.reflect.Method>()
    listOf(
        AUDIENCE_NETWORK_ACTIVITY_CLASS,
        AUDIENCE_NETWORK_REMOTE_ACTIVITY_CLASS,
        AUDIENCE_NETWORK_EXPORTED_ACTIVITY_CLASS
    ).forEach { cn ->
        loadOrNull(cn)?.let { actClass ->
            entryPointOf(actClass)
                ?.let { gameAdUiHooked.putIfAbsent("${it.declaringClass.name}.${it.name}", it) }
        }
    }
    if (gameAdUiHooked.isEmpty()) {
        val activityClass = android.app.Activity::class.java
        GAME_AD_ACTIVITY_CLASS_NAMES.forEach { className ->
            loadOrNull(className)?.let { clazz ->
                if (activityClass.isAssignableFrom(clazz)) {
                    entryPointOf(clazz)
                        ?.let { gameAdUiHooked.putIfAbsent("${it.declaringClass.name}.${it.name}", it) }
                }
            }
        }
    }
    gameAdUiHooked.values.forEach { method -> hookPlayableAdActivity(method) }

    // hookGlobalGameAdActivityLifecycleFallback()

    hookGameAdActivityLaunchFallbacks()

    // ── 13. Native ad view / WebView surface fallbacks ────────────────────────

    // hookGlobalGameAdSurfaceFallbacks()

    // ── 14. ProfileReelsAsyncAdsQuery dispatch block ────────────────────

    run {
        val queryDispatch = ::profileReelsAsyncAdsQueryFingerprint.method
        XposedBridge.hookMethod(queryDispatch, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                param.result = null
            }
        })
    }
}
