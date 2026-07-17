package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * Hide the "Sponsored · Not connected to <friend>" ad Facebook injects at the
 * top of a friend's profile timeline (Vietnamese label:
 *     "Được tài trợ · Chưa kết nối với <tên bạn>").
 *
 * ─── Why the previous version wasn't enough ──────────────────────────────
 *
 * v1 hooked LX/R5p;->A01 (React JSON parser). Profile ads bypass that path
 * entirely — they arrive as pre-materialised GraphQL trees.
 *
 * v2 nulled GraphQLStory.getSponsoredData() and forced isSponsoredStory() to
 * false. That killed the "Được tài trợ" LABEL and the impression logging —
 * but did not remove the underlying post, so the ad body still rendered as
 * an ordinary story. Users reported: "chỉ mất label được tài trợ thôi chứ
 * quảng cáo vẫn còn".
 *
 * Root cause: TimelineStoryComponentSpec (LX/9c8;->A1G) branches on
 *     if (story.getSponsoredData() != null)  {
 *         testKey = "sponsored_timeline_stories_test_key";
 *     } else {
 *         testKey = "timeline_stories_test_key";
 *     }
 *     component = LX/CDx;->A01(inputs);        // <-- SAME builder both sides
 *     if (component == null)  return emptyComponent;   // <-- v16
 *     return component;
 * Both branches build the same visual component via LX/CDx;->A01 — the only
 * thing the sponsored check flips is the analytics/test key. Once we null
 * getSponsoredData() the branch flips to "regular", A01 still builds the
 * component, and the ad remains visible.
 *
 * ─── v3 strategy: drop the whole component for sponsored stories ──────────
 *
 * We use the sponsored check itself as our signal:
 *   1. On EACH call to GraphQLStory.getSponsoredData(), record the real
 *      answer in a ThreadLocal — TRUE if the wrapper is non-null, FALSE
 *      otherwise — THEN return null (kills label + impression logging).
 *   2. Inside LX/9c8;->A1G (TimelineStoryComponentSpec render), our A0N
 *      hook fires once with the ThreadLocal set to TRUE if the story is
 *      an ad; A1G immediately branches to the "regular" path and calls
 *      LX/CDx;->A01(...) to actually build the component.
 *   3. We hook LX/CDx;->A01 and, if the ThreadLocal says the current story
 *      is sponsored, we return null. A1G already has a null-check on the
 *      builder result — it falls through to  `return v16;` (the empty
 *      component) and logs "Creating a Stories in Profile Timeline unit
 *      not successful", which is Facebook's own safe path for a failed
 *      component build. The row simply doesn't render.
 *   4. We wrap A1G with beforeHooked=reset / afterHooked=cleanup so the
 *      ThreadLocal never leaks past a render call.
 *
 * This is surgical: only calls of LX/CDx;->A01 that happen inside the
 * profile-timeline render path for a sponsored story are neutralised.
 * Non-sponsored stories on the same profile — real posts from your friend
 * — are untouched.
 *
 * ─── Diagnostic ──────────────────────────────────────────────────────────
 *
 * Each installed hook logs once at install; the drop counter reports every
 * 50 hidden ads. Look for "NexAlloy/HideProfileTimelineAds" in Xposed log.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"
private val currentStoryIsSponsored: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val insideTimelineRender: ThreadLocal<Boolean> = ThreadLocal.withInitial { false }
private val droppedAdCount = AtomicInteger(0)

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Removes 'Sponsored · Not connected to <friend>' ads (label AND body) on friends' profile timelines.",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")

        // ── 1. GraphQLStory.getSponsoredData() — record + null out ──────────
        //
        // Method obfuscated as A0N() at time of writing. Resolve by exact
        // name first, then structurally as the sole zero-arg non-static
        // getter returning an X.*-package (obfuscated TreeJNI) type.
        val getSponsoredData = graphQLStory.declaredMethods.firstOrNull { m ->
            m.name == "A0N" && m.parameterCount == 0 &&
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic
        } ?: graphQLStory.declaredMethods.firstOrNull { m ->
            m.parameterCount == 0 &&
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic &&
                run {
                    val rt = m.returnType
                    !rt.isPrimitive && rt != Void.TYPE && rt.name.startsWith("X.")
                }
        }

        if (getSponsoredData == null) {
            XposedBridge.log("$LOG_TAG: could not resolve GraphQLStory.getSponsoredData — bailing")
            return@runCatching
        }

        getSponsoredData.isAccessible = true
        XposedBridge.hookMethod(getSponsoredData, object : XC_MethodHook() {
            override fun afterHookedMethod(param: MethodHookParam) {
                // We only care about calls that happen inside the profile-
                // timeline render window; outside of it we still null out to
                // suppress labels + impression logging everywhere else, but
                // we don't touch the ThreadLocal.
                if (insideTimelineRender.get() == true) {
                    val realResult = param.result
                    if (realResult != null) {
                        currentStoryIsSponsored.set(true)
                    }
                }
                // Always null — kills sponsored label, impression logging,
                // "sponsored_data" test key branch, and every other consumer
                // that only cares whether the wrapper is present.
                param.result = null
            }
        })
        XposedBridge.log("$LOG_TAG: hooked GraphQLStory.${getSponsoredData.name}() (record+null)")

        // ── 2. LX/9c8;->A1G — timeline render entry point ────────────────────
        //
        // Set the render window so step 1's ThreadLocal recording only
        // captures A0N calls that happen for the story being rendered right
        // now. Reset both flags on the way out — no state leaks across
        // component builds.
        val timelineComponentSpec = runCatching {
            classLoader.loadClass("X.9c8")
        }.getOrNull()

        if (timelineComponentSpec != null) {
            // A1G(LX/3SA;)LX/3RU; — one arg, one reference return type,
            // non-static, public final.
            val a1g = timelineComponentSpec.declaredMethods.firstOrNull { m ->
                m.name == "A1G" && m.parameterCount == 1 &&
                    !Modifier.isStatic(m.modifiers) && !m.isSynthetic &&
                    m.returnType != Void.TYPE && !m.returnType.isPrimitive
            }
            if (a1g != null) {
                a1g.isAccessible = true
                XposedBridge.hookMethod(a1g, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        insideTimelineRender.set(true)
                        currentStoryIsSponsored.set(false)
                    }
                    override fun afterHookedMethod(param: MethodHookParam) {
                        val wasAd = currentStoryIsSponsored.get() == true
                        insideTimelineRender.set(false)
                        currentStoryIsSponsored.set(false)
                        if (wasAd) {
                            val n = droppedAdCount.incrementAndGet()
                            if (n == 1 || n % 50 == 0) {
                                XposedBridge.log("$LOG_TAG: dropped profile-timeline ad (total=$n)")
                            }
                        }
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked X.9c8.A1G (timeline render window)")
            } else {
                XposedBridge.log("$LOG_TAG: X.9c8 loaded but A1G not found; render window disabled")
            }
        } else {
            XposedBridge.log("$LOG_TAG: X.9c8 not loadable — class index has shifted; skipping window")
        }

        // ── 3. LX/CDx;->A01 — the actual builder, neutralise for ads ────────
        //
        // Signature: A01(LX/6Gl; LX/3Jv; LX/3SA;) LX/3RU;
        // We return null when the ThreadLocal marks the current story as
        // sponsored. A1G already handles null with:
        //     if (component != null)  return component;
        //     Log.e("timeline_story_component",
        //           "Creating a Stories in Profile Timeline unit not successful");
        //     return emptyComponent;                    // <- this path runs
        val timelineBuilder = runCatching {
            classLoader.loadClass("X.CDx")
        }.getOrNull()

        if (timelineBuilder != null) {
            val a01 = timelineBuilder.declaredMethods.firstOrNull { m ->
                m.name == "A01" && m.parameterCount == 3 &&
                    !Modifier.isStatic(m.modifiers) && !m.isSynthetic
            }
            if (a01 != null) {
                a01.isAccessible = true
                XposedBridge.hookMethod(a01, object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        if (currentStoryIsSponsored.get() == true) {
                            // A1G's own null-check will fall through to the
                            // empty-component fallback — Facebook's own safe
                            // path, not something we've fabricated.
                            param.result = null
                        }
                    }
                })
                XposedBridge.log("$LOG_TAG: hooked X.CDx.A01 (drop-ad-body chokepoint)")
            } else {
                XposedBridge.log("$LOG_TAG: X.CDx loaded but A01 not found")
            }
        } else {
            XposedBridge.log("$LOG_TAG: X.CDx not loadable — class index has shifted")
        }
    }.onFailure { e ->
        XposedBridge.log("$LOG_TAG: patch install failed: ${e.javaClass.name}: ${e.message}")
    }
}
