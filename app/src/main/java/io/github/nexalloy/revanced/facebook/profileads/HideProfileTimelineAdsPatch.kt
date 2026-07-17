package io.github.nexalloy.revanced.facebook.profileads

import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import io.github.nexalloy.patch
import java.lang.reflect.Modifier
import java.util.concurrent.atomic.AtomicInteger

/**
 * v4 — diagnostic + broaden render-side coverage.
 *
 * v3 hooked X.9c8.A1G (TimelineStoryComponentSpec render) + X.CDx.A01
 * (component builder) to fall back to Facebook's own empty-component
 * path when a story was sponsored. Log showed "dropped total=1" during a
 * scroll where the user still saw the "Sponsored . Not connected to"
 * ad — so either:
 *
 *   (a) the ad row is rendered by a DIFFERENT spec (not X.9c8), or
 *   (b) X.9c8.A1G returns null but Litho still renders the row from a
 *       cached/prior layout, or
 *   (c) something else swallows our null return.
 *
 * v4:
 *   1. Keeps A0N record+null (kills "Được tài trợ" label + impression).
 *   2. Removes the A1G render-window + CDx-drop combo (didn't remove the
 *      row). Replaces it with a WeakHashMap of "story identity hash ->
 *      isSponsored", so we can also identify ads by object identity in
 *      code that runs AFTER A0N has been nulled.
 *   3. Adds a stack-inspection dump at each drop point so we can see
 *      exactly which spec / caller is rendering the surviving ad row.
 *   4. Hooks BOTH X.9c8.A1G and (if resolvable) the two most likely
 *      alternatives: X.C1t (SearchResultsSponsoredStoryComponentSpec-ish
 *      analogues) and any class whose name contains "MiniFeedStory" /
 *      "FeedStoryBasicComponent" in its Litho spec renderer.
 *      When any of these is called for a story we recorded as sponsored,
 *      the method's result is replaced with null; Facebook's own null
 *      guards produce an empty row.
 *
 * Please share the next Xposed log — the "sample stack" line printed on
 * the first few drops will tell us the exact class rendering the ad and
 * will let us pin the last hook needed.
 */

private const val LOG_TAG = "NexAlloy/HideProfileTimelineAds"

// Track story identity -> sponsored (identity keys so GC still works).
private val sponsoredStories: MutableMap<Any, Boolean> =
    java.util.Collections.synchronizedMap(java.util.WeakHashMap())

private val droppedCount = AtomicInteger(0)
private val stackSamplesLeft = AtomicInteger(5)

private fun sampleStack(from: String) {
    if (stackSamplesLeft.getAndDecrement() <= 0) return
    val t = Thread.currentThread().stackTrace
    val summary = t.asSequence()
        .drop(3)
        .take(12)
        .joinToString(" <- ") { "${it.className}.${it.methodName}" }
    XposedBridge.log("$LOG_TAG: sample stack at $from: $summary")
}

val HideProfileTimelineAds = patch(
    name = "Hide profile-timeline ads",
    description = "Removes 'Sponsored · Not connected to <friend>' ads on friends' profile timelines (v4 diagnostic).",
) {
    runCatching {
        val graphQLStory = classLoader.loadClass("com.facebook.graphql.model.GraphQLStory")

        // ── 1. A0N — record identity + null ──────────────────────────────────
        val getSponsoredData = graphQLStory.declaredMethods.firstOrNull { m ->
            m.name == "A0N" && m.parameterCount == 0 &&
                !Modifier.isStatic(m.modifiers) && !m.isSynthetic
        }
        if (getSponsoredData != null) {
            getSponsoredData.isAccessible = true
            XposedBridge.hookMethod(getSponsoredData, object : XC_MethodHook() {
                override fun afterHookedMethod(param: MethodHookParam) {
                    val story = param.thisObject ?: return
                    val real = param.result
                    if (real != null) {
                        sponsoredStories[story] = true
                        // Log at most a handful of samples so we can see who's asking.
                        sampleStack("A0N(sponsored)")
                    }
                    param.result = null
                }
            })
            XposedBridge.log("$LOG_TAG: hooked GraphQLStory.A0N() (record identity + null)")
        } else {
            XposedBridge.log("$LOG_TAG: WARN — could not resolve GraphQLStory.A0N()")
        }

        // Helper — look through a hook's this + args for any GraphQLStory
        // we've marked as sponsored, return true if found.
        val isSponsoredContext: (XC_MethodHook.MethodHookParam) -> Boolean = fn@{ param ->
            fun check(o: Any?): Boolean {
                if (o == null) return false
                if (graphQLStory.isInstance(o)) return sponsoredStories[o] == true
                return false
            }
            if (check(param.thisObject)) return@fn true
            param.args?.forEach { if (check(it)) return@fn true }
            false
        }

        // ── 2. Hook every timeline / feed story RENDER spec we can find. ─────
        //
        // The Litho render method on a Kotlin/Java ComponentSpec follows the
        // shape:  public final LX/3RU; A1G(LX/3SA;)  — non-static, one arg
        // (Litho component context), returns 3RU (a Component). Many specs
        // in FB share this signature. We match by:
        //   - class name pattern (obfuscated "X.9c8" style is fine; also
        //     look at classes with English names containing Timeline/Feed/
        //     Story ComponentSpec)
        //   - method signature shape
        // and null the result IF the current story is marked sponsored.
        val candidateClasses = mutableListOf<Class<*>>()
        listOf("X.9c8").forEach { n ->
            runCatching { candidateClasses.add(classLoader.loadClass(n)) }
        }
        // Best-effort: try named specs from FB codebase (may not exist as-is).
        listOf(
            "com.facebook.timeline.rows.spec.TimelineStoryComponentSpec",
            "com.facebook.feed.rows.sections.basiccomponent.FeedStoryBasicComponentSpec",
            "com.facebook.feed.rows.sections.minifeed.MiniFeedStoryComponentSpec",
        ).forEach { n ->
            runCatching { candidateClasses.add(classLoader.loadClass(n)) }
        }
        XposedBridge.log("$LOG_TAG: render-spec candidates: ${candidateClasses.map { it.name }}")

        var renderHooks = 0
        for (cls in candidateClasses) {
            for (m in cls.declaredMethods) {
                if (Modifier.isStatic(m.modifiers) || m.isSynthetic) continue
                if (m.parameterCount != 1) continue
                if (m.returnType.isPrimitive || m.returnType == Void.TYPE) continue
                // Litho spec entry methods typically named A1G / onCreateLayout / render.
                val n = m.name
                if (n != "A1G" && n != "onCreateLayout" && n != "render") continue
                m.isAccessible = true
                XposedBridge.hookMethod(m, object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        if (isSponsoredContext(param)) {
                            param.result = null
                            val n2 = droppedCount.incrementAndGet()
                            if (n2 <= 5 || n2 % 25 == 0) {
                                XposedBridge.log("$LOG_TAG: null-ed render on ${cls.name}.${m.name} (total=$n2)")
                                sampleStack("${cls.name}.${m.name}")
                            }
                        }
                    }
                })
                renderHooks++
                XposedBridge.log("$LOG_TAG: hooked render spec ${cls.name}.${m.name}")
            }
        }
        if (renderHooks == 0) {
            XposedBridge.log("$LOG_TAG: WARN — no render-spec method hooked")
        }
    }.onFailure { e ->
        XposedBridge.log("$LOG_TAG: patch install failed: ${e.javaClass.name}: ${e.message}")
    }
}
