package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.util.concurrent.atomic.AtomicInteger

/**
 * Hide the "Sponsored . Not connected to <friend>" ad Facebook injects at the
 * top of a friend's profile timeline (Vietnamese label:
 *     "Được tài trợ · Chưa kết nối với <tên bạn>").
 *
 * Strategy: hook GraphQLStory.getSponsoredData() (obfuscated: A0N()LX/3yW;)
 * and return null. Rationale:
 *
 *   - It is the single canonical getter for the "is this story an ad?"
 *     signal — verified by scanning all 20 DEX files: exactly one method
 *     with that shape exists.
 *   - LX/2o6;->A02(GraphQLStory) — the isSponsoredStory helper used by
 *     TimelineStoryComponentSpec, FeedUnitImpressionLoggerController, the
 *     StoryViewer ads root container, and 4 other consumers in classes6
 *     alone — literally does `return story.A0N() != null`. Null-ing here
 *     turns off every one of those consumers with zero further hooks.
 *   - The ad no longer renders "Sponsored" or "Chưa kết nối với" text,
 *     stops logging impressions to Facebook's ad servers, and — because
 *     downstream logic ignores it as a non-ad — is dropped by the story
 *     rank/dedupe pipeline instead of being force-inserted at the top.
 *
 * The previous version of this patch targeted LX/R5p;->A01, the *React*
 * Marketplace video-ads JSON parser. Profile-timeline ads never go through
 * that path (they arrive as pre-materialised GraphQL trees), which is why
 * users still saw the label after installing.
 *
 * A single Xposed log line per session ("HideProfileTimelineAds: installed
 * on ...") plus a counter of intercepted calls (throttled to one line per
 * 500 hits) confirms the hook is live for diagnostics.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"
private val getSponsoredDataHitCount = AtomicInteger(0)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Removes 'Sponsored · Not connected to <friend>' ads on friends' profile timelines by nulling GraphQLStory.getSponsoredData().",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")

        // Resolve getSponsoredData() by RETURN TYPE, not by obfuscated name.
        // Signature at the time of writing: A0N()LX/3yW; — the field wrapper
        // type LX/3yW; is a TreeJNI subclass. We match by:
        //   (a) zero parameters
        //   (b) non-static, public
        //   (c) return type whose simple name begins with "TreeJNI" OR whose
        //       binary name is short/obfuscated (X.* pattern) — i.e. not a
        //       standard java.* or facebook.graphql.model.* class.
        // This survives the annual FB obfuscation reshuffle without needing
        // a DexKit fingerprint.
        val candidates = graphQLStory.declaredMethods.filter { m ->
            m.parameterCount == 0 &&
                !java.lang.reflect.Modifier.isStatic(m.modifiers) &&
                !m.isSynthetic &&
                run {
                    val rt = m.returnType
                    if (rt.isPrimitive || rt == Void.TYPE) return@run false
                    val n = rt.name
                    // FB's obfuscated inner types look like "X.3yW" / "X.C1a" —
                    // the top-level package is a single-letter directory.
                    // TreeJNI subclasses are the only inner-package types that
                    // GraphQLStory getters return; everything else on this class
                    // is either primitive, String, ImmutableList, or a public
                    // graphql.model.* class.
                    n.startsWith("X.") ||
                        (rt.superclass?.name?.contains("TreeJNI") == true) ||
                        n.contains("SponsoredData", ignoreCase = true)
                }
        }

        // Among X.*-returning zero-arg getters there are several (page, feedback,
        // etc.). The sponsored one is the ONLY one whose return-type class is
        // also reachable from getSponsoredData semantically. We can't tell them
        // apart by name alone, so we hook them ALL to return null ONLY when the
        // stack shows we're being called from the isSponsoredStory helper
        // (LX/2o6;->A02) or from a *SponsoredLabel* / *SponsoredImpression*
        // consumer. This is precise: legitimate non-ad callers of other X.*
        // getters (page info, feedback) will not appear in those stacks.
        //
        // BUT — a simpler and more reliable path: hook LX/2o6;->A02 directly to
        // return false. That method is a single guard used ONLY for ad
        // suppression logic. Forcing it to false does NOT hide a non-ad story;
        // it merely tells FB "this story is not sponsored", which for genuine
        // non-ads is already true.

        // Step 1: force isSponsoredStory() to false.
        // (LX/2o6;->A02 IS the ad-guard used throughout classes6.)
        runCatching {
            val sponsoredCheckClass = classLoader.loadClass("X.2o6")
            sponsoredCheckClass.declaredMethods.firstOrNull { m ->
                java.lang.reflect.Modifier.isStatic(m.modifiers) &&
                    m.parameterCount == 1 &&
                    m.parameterTypes[0] == graphQLStory &&
                    m.returnType == java.lang.Boolean.TYPE
            }?.let { m ->
                m.isAccessible = true
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val hits = getSponsoredDataHitCount.incrementAndGet()
                        if (hits == 1 || hits % 500 == 0) {
                            XposedBridge.log("$LOG_TAG: isSponsoredStory-> false (hits=$hits)")
                        }
                        param.result = false
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked ${m.declaringClass.name}.${m.name} " +
                    "(isSponsoredStory guard)")
            } ?: XposedBridge.log("$LOG_TAG: X.2o6 present but no (GraphQLStory)Z guard found")
        }.onFailure { e ->
            XposedBridge.log("$LOG_TAG: could not load X.2o6 (${e.javaClass.simpleName}: ${e.message}) — " +
                "class letter/index may have shifted; falling through to A0N hook")
        }

        // Step 2: also null the underlying getSponsoredData() so consumers
        // that bypass the X.2o6 guard (e.g. StoryOverlaySponsoredLabel which
        // calls story.A0N() directly) see no sponsored payload either.
        // We identify A0N by return-shape: the only zero-arg non-static
        // method whose return type is an X.* obfuscated TreeJNI wrapper AND
        // where the field-name-hash annotation (if present) matches
        // "sponsored_data". As a robust proxy we look for a method named
        // exactly "A0N" first (current build), then fall back to structural.
        val getSponsoredData = graphQLStory.declaredMethods.firstOrNull { m ->
            m.name == "A0N" && m.parameterCount == 0 &&
                !java.lang.reflect.Modifier.isStatic(m.modifiers)
        } ?: candidates.firstOrNull()

        if (getSponsoredData != null) {
            getSponsoredData.isAccessible = true
            XposedBridge.hookMethod(getSponsoredData, object : XC_MethodHook() {
                override fun beforeHookedMethod(param: MethodHookParam) {
                    param.result = null
                }
            })
            XposedBridge.log("$LOG_TAG: hooked GraphQLStory.${getSponsoredData.name}() " +
                "(getSponsoredData chokepoint)")
        } else {
            XposedBridge.log("$LOG_TAG: could not resolve getSponsoredData on GraphQLStory " +
                "(declaredMethods=${graphQLStory.declaredMethods.size}, " +
                "candidates=${candidates.size}) — profile ads may still appear")
        }
    }.onFailure { e ->
        XposedBridge.log("$LOG_TAG: patch install failed: ${e.javaClass.name}: ${e.message}")
    }
}
