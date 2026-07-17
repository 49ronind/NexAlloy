package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier

/**
 * Hide Facebook "Sponsored · Not connected to <friend>" profile-timeline ads.
 *
 * Fixes the Vietnamese-user-visible label:
 *     "Được tài trợ · Chưa kết nối với <tên bạn>"
 * which appears above the first post when opening a friend's profile.
 *
 * This is a small, focused companion to HideFacebookAds. It does NOT replace
 * the main feed-ads patch — it plugs the three gaps that let the profile-
 * timeline variant slip through:
 *
 *   1. Sponsored data builder (LX/R5p;->A01) — chokepoint: return null so no
 *      GraphQL Sponsored subtree is ever produced from a JSON payload
 *      containing "is_non_connected_page_post" (or any other sponsored ad).
 *   2. QE gate — force facebook_profile_ads_brand_safety to its default (0),
 *      disabling the profile-ads surface for the current session even if
 *      server-side gating is on.
 *   3. TimelineStoryComponentSpec.A1K — no-op the entry point that handles
 *      profile-ad brand-safety impressions, as belt-and-suspenders in case
 *      a Sponsored tree is somehow already resident in memory (e.g. cached
 *      before the hook installed).
 *
 * Each hook is wrapped in runCatching so a fingerprint miss on any one of
 * them leaves the others intact.
 */
val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Removes 'Sponsored · Not connected to <friend>' ads shown on friends' profile timelines.",
) {
    // ── 1. Sponsored data builder — starve every downstream consumer ─────────
    runCatching {
        val builder = ::sponsoredDataBuilderMethodFingerprint.method
        XposedBridge.hookMethod(builder, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Facebook's own catch-block on this method already handles a
                // thrown JSONException by logging "Failed to create Graph QL
                // Sponsored data" and returning null — so returning null here
                // is a code path Facebook is already prepared for.
                param.result = null
            }
        })
    }

    // ── 2. QE gate — force default (disabled) for profile ads brand safety ───
    runCatching {
        val qeMethod = ::profileAdsBrandSafetyQeMethodFingerprint.method
        XposedBridge.hookMethod(qeMethod, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                val qeName = param.args.getOrNull(0) as? String ?: return
                if (qeName == "facebook_profile_ads_brand_safety") {
                    // Return the caller-supplied default (arg 1). All feature-
                    // gated call sites treat 0 / their default as "off".
                    param.result = param.args.getOrNull(1) ?: 0
                }
            }
        })
    }

    // ── 3. TimelineStoryComponentSpec — no-op the brand-safety branch ────────
    // The method is a synthetic-bridged component-spec entry point. Its
    // descriptor is stable-ish across builds; we resolve by signature shape
    // (public final, non-static, non-synthetic, single-Object-return, exactly
    // two reference parameters) rather than by name to survive obfuscation.
    runCatching {
        val cls = ::timelineStoryComponentSpecClassFingerprint.clazz
        val candidate = cls.declaredMethods.firstOrNull { m ->
            !Modifier.isStatic(m.modifiers) &&
                !m.isSynthetic &&
                m.parameterCount == 2 &&
                m.returnType == Any::class.java &&
                m.parameterTypes.all { !it.isPrimitive }
        } ?: return@runCatching
        candidate.isAccessible = true
        XposedBridge.hookMethod(candidate, object : XC_MethodHook() {
            override fun beforeHookedMethod(param: MethodHookParam) {
                // Returning null here matches the natural "no side-effect,
                // nothing to render" fall-through the method already takes
                // when the brand-safety gate is disabled — which is exactly
                // what step (2) makes true.
                param.result = null
            }
        })
    }
}
